package tech.pegb.backoffice.dao.i18n.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm.SqlParser.scalar
import anorm.{BatchSql, NamedParameter, Row, RowParser, SQL, SimpleSql, SqlQuery, SqlRequestError}
import cats.data.NonEmptyList
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.i18n.abstraction.I18nStringDao
import tech.pegb.backoffice.dao.i18n.dto.{I18nStringCriteria, I18nStringToInsert, I18nStringToUpdate}
import tech.pegb.backoffice.dao.i18n.entity.{I18nPair, I18nString}
import tech.pegb.backoffice.dao.model._
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

class I18nStringSqlDao @Inject() (
    val dbApi: DBApi,
    kafkaDBSyncService: KafkaDBSyncService) extends I18nStringDao with MostRecentUpdatedAtGetter[I18nString, I18nStringCriteria] with SqlDao {

  import I18nStringSqlDao._

  protected def getUpdatedAtColumn: String = s"${I18nStringSqlDao.TableAlias}.${I18nStringSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = I18nStringSqlDao.qCommonSelect

  protected def getRowToEntityParser: Row ⇒ I18nString = (arg: Row) ⇒ i18nStringRowParser(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[I18nStringCriteria]): String = generateI18nStringWhereFilter(criteriaDto)

  def insertString(dto: I18nStringToInsert): DaoResponse[I18nString] = {

    withTransaction({ implicit cxn: Connection ⇒
      val generatedId = insertI18nStringQuery
        .on(
          cKey → dto.key,
          cText → dto.text,
          cLocale → dto.locale,
          cPlatform → dto.platform,
          cType → dto.`type`,
          cExplanation → dto.explanation,
          cCreatedAt → dto.createdAt,
          cUpdatedAt → dto.createdAt) //not nullable in db and same as created at on insertion
        .executeInsert(scalar[Int].single)

      internalGetI18nString(generatedId) match {
        case Some(i18nString) ⇒
          kafkaDBSyncService.sendInsert(TableName, i18nString)
          i18nString
        case None ⇒ throw new Throwable("Failed to created fetch i18n string")
      }
    }, s"Failed to create i18n string: ${dto.toSmartString}",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not create i18n string ${dto.toSmartString}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error("error encountered in [insertString]", generic)
          genericDbError(s"Error encountered while inserting i18n [${dto.toSmartString}]")
      })
  }

  def bulkInsertString(dtos: NonEmptyList[I18nStringToInsert])(implicit maybeConnection: Option[Connection]): DaoResponse[Int] = {
    withTransaction({ cxn: Connection ⇒
      implicit val connection = maybeConnection.getOrElse(cxn)

      val i18nNamedParameterSeq = dtos.map { dto ⇒
        Seq(
          NamedParameter(cKey, dto.key),
          NamedParameter(cText, dto.text),
          NamedParameter(cLocale, dto.locale),
          NamedParameter(cPlatform, dto.platform),
          NamedParameter(cType, dto.`type`),
          NamedParameter(cExplanation, dto.explanation),
          NamedParameter(cCreatedAt, dto.createdAt),
          NamedParameter(cUpdatedAt, dto.createdAt))
      }

      BatchSql(rawInsertSqlString, i18nNamedParameterSeq.head, i18nNamedParameterSeq.tail: _*).execute()

      dtos.size
    }, s"[bulkInsertString] error encountered on bulk insert of I18nStrings",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not create i18n string, SQLException encountered"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error(s"[bulkInsertString] generic error", generic)
          genericDbError(s"error encountered on bulk insert of I18nStrings")
      })
  }

  def getStringById(id: Int): DaoResponse[Option[I18nString]] = {
    withConnection({ implicit cxn: Connection ⇒
      internalGetI18nString(id)
    }, s"Failed to fetch i18n string $id")
  }

  def getStringByCriteria(
    criteria: I18nStringCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int])(implicit maybeConnection: Option[Connection] = None): DaoResponse[Seq[I18nString]] = withConnection({ cxn: Connection ⇒

    implicit val connection = maybeConnection.getOrElse(cxn)

    val whereFilter = generateI18nStringWhereFilter(criteria.toOption)

    val i18nStringByCriteriaSql = findI18nStringByCriteriaQuery(whereFilter, ordering, limit, offset)

    i18nStringByCriteriaSql.as(i18nStringRowParser.*)

  }, s"Error while retrieving i18n strings by criteria: $criteria")

  def getI18nPairsByCriteria(criteria: I18nStringCriteria): DaoResponse[Seq[I18nPair]] = withConnection({ implicit connection ⇒
    val whereFilter = generateI18nStringWhereFilter(criteria.toOption)

    val i18nPairsByCriteriaSql = findI18nPairsByCriteriaQuery(whereFilter)

    i18nPairsByCriteriaSql
      .as(i18nPairRowParser.*)

  }, s"Error while retrieving i18n pairs by criteria: $criteria")

  def countStringByCriteria(criteria: I18nStringCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateI18nStringWhereFilter(criteria.toOption)
    val countByCriteriaSql = countI18nStringByCriteriaQuery(whereFilter)

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  def updateString(
    id: Int,
    dto: I18nStringToUpdate,
    disableOptimisticLock: Boolean = false)(implicit maybeConnection: Option[Connection] = None): DaoResponse[Option[I18nString]] = {
    withTransaction({ implicit cxn: Connection ⇒
      for {
        existing ← internalGetI18nString(id)
        updateResult = updateQuery(id, dto, disableOptimisticLock).executeUpdate()
        updated ← if (updateResult > 0) {
          internalGetI18nString(id)
        } else {
          throw new IllegalStateException(s"Update failed. I18n String ${id} has been modified by another process.")
        }
      } yield {
        kafkaDBSyncService.sendUpdate(TableName, updated)
        updated
      }
    }, s"Failed to update I18n String $id",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update I18n String ${id}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })

  }

  def deleteString(id: Int, updatedAt: Option[LocalDateTime]): DaoResponse[Option[Int]] = {
    withTransaction({ implicit cxn: Connection ⇒
      for {
        existing ← internalGetI18nString(id)
        deleteResult = deleteQuery(id, updatedAt).executeUpdate()
        updated = if (deleteResult < 1) {
          throw new IllegalStateException(s"Delete failed. I18n String ${id} has been modified by another process.")
        }
      } yield {
        kafkaDBSyncService.sendDelete(TableName, id)
        id
      }
    }, s"Failed to delete I18n String $id",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not delete I18n String ${id}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })
  }

  private[sql] def internalGetI18nString(
    id: Int)(
    implicit
    cxn: Connection): Option[I18nString] = {
    fetchByIdQuery.on(cId → id)
      .executeQuery().as(i18nStringRowParser.singleOpt)

  }
}

object I18nStringSqlDao {

  val TableName = "i18n_strings"
  val TableAlias = "i18n_s"

  //columns
  val cId = "id"
  val cKey = "key"
  val cText = "text"
  val cLocale = "locale"
  val cPlatform = "platform"
  val cType = "type"
  val cExplanation = "explanation"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"

  private val rawInsertSqlString = s"INSERT INTO $TableName (`$cKey`, `$cText`, $cLocale, $cPlatform, $cType, $cExplanation, $cCreatedAt, $cUpdatedAt) " +
    s"VALUES ({$cKey}, {$cText}, {$cLocale}, {$cPlatform}, {$cType}, {$cExplanation}, {$cCreatedAt}, {$cUpdatedAt})"

  private val insertI18nStringQuery = SQL(rawInsertSqlString)

  private def fetchByIdQuery: SqlQuery = {
    val columns = s"$TableAlias.*"
    val filters = s"""WHERE $TableAlias.$cId = {$cId}"""

    SQL(s"""${baseFindI18nStringByCriteria(columns, filters)}""".stripMargin)
  }

  private def baseFindI18nStringByCriteria(selectColumns: String, filters: String): String = {
    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |$filters""".stripMargin
  }

  private val qCommonSelect = s"SELECT $TableAlias.* FROM $TableName $TableAlias"

  private def generateI18nStringWhereFilter(mayBeCriteria: Option[I18nStringCriteria]): String = {
    import SqlDao._
    mayBeCriteria.map { criteria ⇒

      Seq(
        criteria.id.map(_.toSql(cId.toOption, TableAlias.toOption)),
        criteria.locale.map(_.toSql(cLocale.toOption, TableAlias.toOption)),
        criteria.platform.map(_.toSql(cPlatform.toOption, TableAlias.toOption)),
        criteria.key.map(_.toSql(cKey.toOption, TableAlias.toOption)),
        criteria.explanation.map(_.toSql(cExplanation.toOption, TableAlias.toOption)),
        criteria.`type`.map(_.toSql(cType.toOption, TableAlias.toOption))).flatten.toSql
    }.getOrElse("")
  }

  private def countI18nStringByCriteriaQuery(filters: String): SqlQuery = {
    val column = "COUNT(*) as n"
    SQL(s"""${baseFindI18nStringByCriteria(column, filters)}""")
  }

  private def findI18nStringByCriteriaQuery(
    filters: String,
    maybeOrderBy: Option[OrderingSet],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): SqlQuery = {

    val ordering = maybeOrderBy.fold("")(_.toString
      .replace(cKey, s"`$cKey`")
      .replace(cText, s"`$cText`"))
    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)

    val columns = s"$TableAlias.*"

    SQL(s"""${baseFindI18nStringByCriteria(columns, filters)} $ordering $pagination""".stripMargin)
  }

  private def findI18nPairsByCriteriaQuery(filters: String): SqlQuery = {
    val columns = s"$TableAlias.$cKey, $TableAlias.$cText"
    SQL(s"""${baseFindI18nStringByCriteria(columns, filters)}""".stripMargin)
  }

  private def updateQuery(id: Int, dto: I18nStringToUpdate, disableOptimisticLock: Boolean): SimpleSql[Row] = {
    val paramsBuffer = dto.paramsBuilder
    val filterParam = NamedParameter(cId, id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"), disableOptimisticLock)

    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }

  private def deleteQuery(id: Int, lastUpdatedAt: Option[LocalDateTime]): SimpleSql[Row] = {
    val paramsBuilder = ArrayBuffer.newBuilder[NamedParameter]

    paramsBuilder += NamedParameter(cId, id)
    lastUpdatedAt.foreach(x ⇒ paramsBuilder += NamedParameter(cUpdatedAt, x))

    val filter = s"WHERE $cId = {$cId} AND ${lastUpdatedAt.fold(s"$cUpdatedAt is NULL")(_ ⇒ s"$cUpdatedAt = {$cUpdatedAt}")}"
    val preQuery = s"DELETE FROM $TableName $filter"

    SQL(preQuery).on(paramsBuilder.result(): _*)
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  private def i18nStringRowParser(row: Row): I18nString = {

    I18nString(
      id = row[Int](cId),
      key = row[String](cKey),
      text = row[String](cText),
      locale = row[String](cLocale),
      platform = row[String](cPlatform),
      `type` = row[Option[String]](cType),
      explanation = row[Option[String]](cExplanation),
      createdAt = row[LocalDateTime](cCreatedAt),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt))
  }

  private val i18nStringRowParser: RowParser[I18nString] = (row: Row) ⇒ {
    Try {
      I18nString(
        id = row[Int](cId),
        key = row[String](cKey),
        text = row[String](cText),
        locale = row[String](cLocale),
        platform = row[String](cPlatform),
        `type` = row[Option[String]](cType),
        explanation = row[Option[String]](cExplanation),
        createdAt = row[LocalDateTime](cCreatedAt),
        updatedAt = row[Option[LocalDateTime]](cUpdatedAt))

    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private val i18nPairRowParser: RowParser[I18nPair] = (row: Row) ⇒ {
    Try {
      I18nPair(
        key = row[String](cKey),
        text = row[String](cText))
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

}
