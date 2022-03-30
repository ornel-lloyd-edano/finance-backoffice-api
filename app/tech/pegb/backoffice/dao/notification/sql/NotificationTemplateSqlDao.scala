package tech.pegb.backoffice.dao.notification.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime
import java.util.UUID

import anorm.SqlParser.scalar
import anorm._
import javax.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.notification.abstraction.NotificationTemplateDao
import tech.pegb.backoffice.dao.notification.dto.{NotificationTemplateCriteria, NotificationTemplateToInsert, NotificationTemplateToUpdate}
import tech.pegb.backoffice.dao.notification.entity.NotificationTemplate
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

class NotificationTemplateSqlDao @Inject() (val dbApi: DBApi, kafkaDBSyncService: KafkaDBSyncService) extends NotificationTemplateDao with SqlDao with MostRecentUpdatedAtGetter[NotificationTemplate, NotificationTemplateCriteria] {

  import NotificationTemplate._
  import NotificationTemplateSqlDao._
  import SqlDao._

  protected def getUpdatedAtColumn: String = s"${NotificationTemplateSqlDao.TableAlias}.${NotificationTemplateSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = NotificationTemplateSqlDao.qCommonSelect

  protected def getRowToEntityParser: Row ⇒ NotificationTemplate = (arg: Row) ⇒ convertRowToNotificationTemplate(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[NotificationTemplateCriteria]): String = generateWhereFilter(criteriaDto)

  def getNotificationTemplateByCriteria(
    criteria: Option[NotificationTemplateCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[NotificationTemplate]] =
    withConnection({ implicit connection: Connection ⇒

      val where = generateWhereFilter(criteria)

      val ord = ordering.map(_.toString).getOrElse(
        s"ORDER BY ${TableAlias}.${cId} ASC")

      val pagination = getPagination(limit, offset)

      val query = SQL(
        s"$qCommonSelect $where $ord $pagination")

      logger.info(s"get notification template by criteria query = $query")
      query
        .as(query.defaultParser.*)
        .map(convertRowToNotificationTemplate(_))
    }, s"error while fetching notification template by criteria $criteria")

  def getCountByCriteria(criteria: Option[NotificationTemplateCriteria]): DaoResponse[Int] =
    withConnection({ implicit connection: Connection ⇒
      val where = generateWhereFilter(criteria)
      val query = SQL(s"SELECT COUNT(*) as n FROM $TableName $TableAlias $where")

      query
        .as(query.defaultParser.singleOpt)
        .map(row ⇒ row[Int]("n")).getOrElse(0)

    }, "error while fetching notifications template count by criteria")

  def insertNotificationTemplate(dto: NotificationTemplateToInsert): DaoResponse[NotificationTemplate] =
    withConnectionAndFlatten({ implicit connection: Connection ⇒
      val uuid = UUID.randomUUID()
      val id = insertQuery.on(
        cUuid → uuid,
        cName → dto.name,
        cTitleResource → dto.titleResource,
        cDefaultTitle → dto.defaultTitle,
        cContentResource → dto.contentResource,
        cDefaultContent → dto.defaultContent,
        cChannels → dto.channels,
        cDescription → dto.description.getOrElse(""),
        cCreatedAt → dto.createdAt,
        cCreatedBy → dto.createdBy,
        cUpdatedAt → dto.createdAt, //not nullable in db and same as created at on insertion
        cUpdatedBy → dto.createdBy, //not nullable in db and same as created by on insertion
        cIsActive → dto.isActive).executeInsert(scalar[Int].single)

      for (
        notificationTemplate ← internalGetNotificationTemplate(id)
      ) yield {
        kafkaDBSyncService.sendInsert(TableName, notificationTemplate)

        notificationTemplate
      }
    }, s"error while inserting notification template $dto")

  def updateNotificationTemplate(id: String, dto: NotificationTemplateToUpdate): DaoResponse[NotificationTemplate] =
    withConnectionAndFlatten({ implicit connection: Connection ⇒
      for {
        nt ← internalGetNotificationTemplateByUUID(id)
        result = updateQuery(nt.id, dto).executeUpdate()
        notificationTemplate ← if (result.isUpdated) {
          internalGetNotificationTemplate(nt.id)
        } else {
          throw new IllegalStateException(s"Update failed. notification template criteria $id has been modified by another process.")
        }

        _ = kafkaDBSyncService.sendUpdate(TableName, notificationTemplate)

      } yield notificationTemplate
    }, s"error while updating notification template $dto",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update notification template $id"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })

  private[sql] def internalGetNotificationTemplate(id: Int)(implicit cxn: Connection) = {
    fetchByIdQuery.on(cId → id)
      .executeQuery()
      .as(fetchByIdQuery.defaultParser.singleOpt)
      .map(convertRowToNotificationTemplate(_))
      .toRight(entityNotFoundError(s"not found notification template id $id"))
  }

  private[sql] def internalGetNotificationTemplateByUUID(uuid: String)(implicit cxn: Connection) = {
    fetchByUUIDQuery.on(cUuid → uuid)
      .executeQuery()
      .as(fetchByUUIDQuery.defaultParser.singleOpt)
      .map(convertRowToNotificationTemplate(_))
      .toRight(entityNotFoundError(s"not found notification template uuid $uuid"))
  }

  private def updateQuery(id: Int, dto: NotificationTemplateToUpdate): SimpleSql[Row] = {
    val paramsBuffer = dto.paramsBuilder
    val filterParam = NamedParameter(cId, id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))

    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }
}

object NotificationTemplateSqlDao {
  import SqlDao._

  val TableName = "notification_templates"
  val TableAlias = "nt"

  val cId = "id"
  val cUuid = "uuid"
  val cName = "name"
  val cTitleResource = "title_resource"
  val cDefaultTitle = "default_title"
  val cContentResource = "content_resource"
  val cDefaultContent = "default_content"
  val cChannels = "channels"
  val cDescription = "description"
  val cCreatedAt = "created_at"
  val cCreatedBy = "created_by"
  val cUpdatedAt = "updated_at"
  val cUpdatedBy = "updated_by"
  val cIsActive = "is_active"

  private final val insertQuery =
    SQL(s"INSERT INTO $TableName ($cUuid, $cName, $cTitleResource, $cDefaultTitle, $cContentResource," +
      s"$cDefaultContent, $cChannels, $cDescription, $cCreatedAt, $cCreatedBy, $cUpdatedAt, $cUpdatedBy, `$cIsActive`)" +
      s"VALUES ({$cUuid}, {$cName}, {$cTitleResource}, {$cDefaultTitle}, {$cContentResource}, {$cDefaultContent}," +
      s"{$cChannels}, {$cDescription}, {$cCreatedAt}, {$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy}, {$cIsActive})")

  private final val qCommonSelect = s"SELECT $TableAlias.* FROM $TableName $TableAlias"

  private def fetchByIdQuery: SqlQuery = {
    val filters = s"WHERE $TableAlias.$cId = {$cId}"

    SQL(s"$qCommonSelect $filters".stripMargin)
  }

  private def fetchByUUIDQuery: SqlQuery = {
    val filters = s"WHERE $TableAlias.$cUuid = {$cUuid}"

    SQL(s"$qCommonSelect $filters".stripMargin)
  }

  private def convertRowToNotificationTemplate(row: Row): NotificationTemplate =
    NotificationTemplate(
      id = row[Int](cId),
      uuid = row[String](cUuid),
      name = row[String](cName),
      titleResource = row[String](cTitleResource),
      defaultTitle = row[String](cDefaultTitle),
      contentResource = row[String](cContentResource),
      defaultContent = row[String](cDefaultContent),
      description = row[Option[String]](cDescription),
      channels = row[String](cChannels),
      createdAt = row[LocalDateTime](cCreatedAt),
      createdBy = row[String](cCreatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
      updatedBy = row[Option[String]](cUpdatedBy),
      isActive = row[Int](cIsActive).toBoolean)

  private def generateWhereFilter(maybeCriteria: Option[NotificationTemplateCriteria]): String = {
    {

      maybeCriteria.map { criteria ⇒
        Seq(
          criteria.id.map(_.toSql(Some(cId), Some(TableAlias))),
          criteria.uuid.map(_.toSql(Some(cUuid), Some(TableAlias))),

          criteria.name.map(_.toSql(Some(cName), Some(TableAlias))),
          criteria.titleResource.map(_.toSql(Some(cTitleResource), Some(TableAlias))),

          criteria.contentResource.map(_.toSql(Some(cContentResource), Some(TableAlias))),
          criteria.channels.map(_.toSql(Some(cChannels), Some(TableAlias))),

          criteria.isActive.map(_.toSql(Some(cIsActive), Some(TableAlias))),

          criteria.createdAt.map(_.toSql(Some(cCreatedAt), Some(TableAlias))),
          criteria.createdBy.map(_.toSql(Some(cCreatedBy), Some(TableAlias))),
          criteria.updatedAt.map(_.toSql(Some(cUpdatedAt), Some(TableAlias))),
          criteria.updatedBy.map(_.toSql(Some(cUpdatedBy), Some(TableAlias))))
          .flatten.toSql
      }
    }.getOrElse("")
  }

}
