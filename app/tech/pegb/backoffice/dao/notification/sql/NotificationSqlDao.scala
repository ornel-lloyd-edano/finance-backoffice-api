package tech.pegb.backoffice.dao.notification.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime
import java.util.UUID

import anorm.SqlParser.scalar
import anorm._
import javax.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService

import tech.pegb.backoffice.dao.{DaoError, SqlDao}
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.notification.abstraction.NotificationDao
import tech.pegb.backoffice.dao.notification.dto.{NotificationCriteria, NotificationToInsert, NotificationToUpdate}
import tech.pegb.backoffice.dao.notification.entity.Notification
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._

class NotificationSqlDao @Inject() (
    final val dbApi: DBApi,
    kafkaDBSyncService: KafkaDBSyncService) extends NotificationDao
  with SqlDao with MostRecentUpdatedAtGetter[Notification, NotificationCriteria] {

  import NotificationSqlDao._
  import SqlDao._
  import Notification._

  protected def getUpdatedAtColumn: String = s"${NotificationSqlDao.TableAlias}.${NotificationSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = NotificationSqlDao.qCommonSelect

  protected def getRowToEntityParser: Row ⇒ Notification = (arg: Row) ⇒ convertRowToNotification(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[NotificationCriteria]): String = generateWhereFilter(criteriaDto)

  def getNotificationByCriteria(
    criteria: Option[NotificationCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[Notification]] =
    withConnection({ implicit connection: Connection ⇒

      val where = generateWhereFilter(criteria)

      val ord = ordering.map(_.toString).getOrElse(
        s"ORDER BY ${TableAlias}.${cId} ASC")

      val pagination = getPagination(limit, offset)

      val query = SQL(
        s"$qCommonSelect $where $ord $pagination")

      logger.info(s"get notification by criteria query = $query")
      query
        .as(query.defaultParser.*)
        .map(convertRowToNotification(_))
    }, s"error while fetching notification by criteria $criteria")

  def getCountByCriteria(criteria: Option[NotificationCriteria]): DaoResponse[Int] =
    withConnection({ implicit connection: Connection ⇒
      val where = generateWhereFilter(criteria)
      val query = SQL(s"SELECT COUNT(*) as n $qCommonJoin $where")

      query
        .as(query.defaultParser.singleOpt)
        .map(row ⇒ row[Int]("n")).getOrElse(0)

    }, "error while fetching notifications count by criteria")

  def insertNotification(dto: NotificationToInsert): DaoResponse[Notification] =
    withConnectionAndFlatten({ implicit connection: Connection ⇒
      val uuid = UUID.randomUUID()
      val id = insertQuery.on(
        cUuid → uuid,
        cTemplateId → dto.templateId,
        cChannel → dto.channel,
        cTitle → dto.title,
        cContent → dto.content,
        cOperationId → dto.operationId,
        cAddress → dto.address,
        cUserId → dto.userId,
        cStatus → dto.status,
        cSentAt → dto.sentAt,
        cErrorMessage → dto.errorMsg,
        cRetries → dto.retries,
        cCreatedAt → dto.createdAt,
        cCreatedBy → dto.createdBy,
        cUpdatedAt → dto.createdAt, //not nullable in db and same as created at on insertion
        cUpdatedBy → dto.createdBy).executeInsert(scalar[Int].single) //not nullable in db and same as created by on insertion

      for (
        notification ← internalGetNotification(id)
      ) yield {
        kafkaDBSyncService.sendInsert(TableName, notification)

        notification
      }
    }, s"error while inserting notification $dto")

  def updateNotification(uuid: String, dto: NotificationToUpdate): DaoResponse[Notification] =
    withConnectionAndFlatten({ implicit connection: Connection ⇒
      for {
        notification ← internalGetNotificationByUuid(uuid)
        result = updateQuery(notification.id, dto).executeUpdate()
        updatedNotification ← if (result.isUpdated) {
          internalGetNotification(notification.id)
        } else {
          throw new IllegalStateException(s"Update failed. notification ${notification.id} has been modified by another process.")
        }

        _ = kafkaDBSyncService.sendUpdate(TableName, updatedNotification)

      } yield updatedNotification

    }, s"error while updating notification $dto",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update notification $uuid"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })

  private[sql] def internalGetNotification(id: Int)(implicit cxn: Connection): Either[DaoError.EntityNotFoundError, Notification] = {
    fetchByIdQuery.on(cId → id)
      .executeQuery()
      .as(fetchByIdQuery.defaultParser.singleOpt)
      .map(convertRowToNotification(_))
      .toRight(entityNotFoundError(s"No notification found for id $id"))
  }

  private[sql] def internalGetNotificationByUuid(uuid: String)(implicit cxn: Connection): Either[DaoError.EntityNotFoundError, Notification] = {
    fetchByUUIDQuery.on(cUuid → uuid)
      .executeQuery()
      .as(fetchByUUIDQuery.defaultParser.singleOpt)
      .map(convertRowToNotification(_))
      .toRight(entityNotFoundError(s"No notification found for uuid $uuid"))
  }

  private def updateQuery(id: Int, dto: NotificationToUpdate): SimpleSql[Row] = {
    val paramsBuffer = dto.paramsBuilder
    val filterParam = NamedParameter(cId, id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))

    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }

}

object NotificationSqlDao {
  import SqlDao._

  final val TableName = "notifications"
  final val TableAlias = "n"

  final val cId = "id"
  final val cUuid = "uuid"
  final val cTemplateId = "template_id"
  final val cChannel = "channel"
  final val cTitle = "title"
  final val cContent = "content"
  final val cOperationId = "operation_id"
  final val cAddress = "address"
  final val cUserId = "user_id"
  final val cStatus = "status"
  final val cSentAt = "sent_at"
  final val cErrorMessage = "error_message"
  final val cRetries = "retries"
  final val cCreatedAt = "created_at"
  final val cCreatedBy = "created_by"
  final val cUpdatedAt = "updated_at"
  final val cUpdatedBy = "updated_by"

  final val cUserUuid = "user_uuid"
  final val cTemplateUuid = "template_uuid"

  private final val insertQuery =
    SQL(s"INSERT INTO $TableName ($cUuid, $cTemplateId, $cChannel, $cTitle, $cContent," +
      s"$cOperationId, $cAddress, $cUserId, $cStatus, $cSentAt, $cErrorMessage, `$cRetries`," +
      s"$cCreatedAt, $cCreatedBy, $cUpdatedAt, $cUpdatedBy) " +
      s"VALUES ({$cUuid}, {$cTemplateId}, {$cChannel}, {$cTitle}, {$cContent}, {$cOperationId}," +
      s"{$cAddress}, {$cUserId}, {$cStatus}, {$cSentAt}, {$cErrorMessage}, {$cRetries}," +
      s"{$cCreatedAt}, {$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy})")

  private final val qCommonJoin =
    s"""
       |FROM $TableName $TableAlias
       |JOIN ${NotificationTemplateSqlDao.TableName} ${NotificationTemplateSqlDao.TableAlias}
       |ON $TableAlias.$cTemplateId = ${NotificationTemplateSqlDao.TableAlias}.${NotificationTemplateSqlDao.cId}
       |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
       |ON ${UserSqlDao.TableAlias}.${UserSqlDao.id} =  $TableAlias.$cUserId""".stripMargin

  private final val qCommonSelect = s"SELECT $TableAlias.*, ${NotificationTemplateSqlDao.TableAlias}.${NotificationTemplateSqlDao.cUuid}" +
    s" as $cTemplateUuid, ${UserSqlDao.TableAlias}.${UserSqlDao.uuid} as $cUserUuid $qCommonJoin"

  private def fetchByIdQuery: SqlQuery = {

    SQL(s"$qCommonSelect WHERE $TableAlias.$cId = {$cId}")
  }

  private def fetchByUUIDQuery: SqlQuery = {

    SQL(s"$qCommonSelect WHERE $TableAlias.$cUuid = {$cUuid}")
  }

  private def convertRowToNotification(row: Row): Notification =
    Notification(
      id = row[Int](cId),
      uuid = row[String](cUuid),
      userId = row[Option[Int]](cUserId),
      userUuid = row[Option[String]](cUserUuid), //get user uuid
      templateId = row[Int](cTemplateId),
      templateUuid = row[String](cTemplateUuid), //get template uuid
      channel = row[String](cChannel),
      title = row[String](cTitle),
      content = row[String](cContent),
      address = row[String](cAddress),
      status = row[String](cStatus),
      errorMsg = row[Option[String]](cErrorMessage),
      retries = row[Option[Int]](cRetries),
      operationId = row[String](cOperationId),
      createdBy = row[String](cCreatedBy),
      createdAt = row[LocalDateTime](cCreatedAt),
      updatedBy = row[Option[String]](cUpdatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
      sentAt = row[Option[LocalDateTime]](cSentAt))

  private def generateWhereFilter(maybeCriteria: Option[NotificationCriteria]): String = {

    maybeCriteria.map { criteria ⇒
      Seq(
        criteria.id.map(_.toSql(Some(cId), Some(TableAlias))),
        criteria.uuid.map(_.toSql(Some(cUuid), Some(TableAlias))),

        criteria.userId.map(_.toSql(Some(cUserId), Some(TableAlias))),
        criteria.userUuid.map(_.toSql(Some(cUserUuid), Some(TableAlias))),

        criteria.templateId.map(_.toSql(Some(cTemplateUuid), Some(TableAlias))),
        criteria.templateUuid.map(_.toSql(Some(cTemplateUuid), Some(TableAlias))),

        criteria.channel.map(_.toSql(Some(cChannel), Some(TableAlias))),
        criteria.title.map(_.toSql(Some(cTitle), Some(TableAlias))),
        criteria.content.map(_.toSql(Some(cContent), Some(TableAlias))),
        criteria.address.map(_.toSql(Some(cAddress), Some(TableAlias))),

        criteria.status.map(_.toSql(Some(cStatus), Some(TableAlias))),
        criteria.errorMsg.map(_.toSql(Some(cErrorMessage), Some(TableAlias))),
        criteria.retries.map(_.toSql(Some(cRetries), Some(TableAlias))),
        criteria.operationId.map(_.toSql(Some(cOperationId), Some(TableAlias))),

        criteria.createdAt.map(_.toSql(Some(cCreatedAt), Some(TableAlias))),
        criteria.createdBy.map(_.toSql(Some(cCreatedBy), Some(TableAlias))),
        criteria.updatedAt.map(_.toSql(Some(cUpdatedAt), Some(TableAlias))),
        criteria.updatedBy.map(_.toSql(Some(cUpdatedBy), Some(TableAlias))))
        .flatten.toSql

    }
  }.getOrElse("")

}
