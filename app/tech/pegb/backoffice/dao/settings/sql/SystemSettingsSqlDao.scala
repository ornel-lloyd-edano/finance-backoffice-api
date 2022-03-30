package tech.pegb.backoffice.dao.settings.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm.SqlParser.scalar
import anorm.{NamedParameter, Row, RowParser, SQL, SimpleSql, SqlQuery, SqlRequestError}
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.settings.abstraction.SystemSettingsDao
import tech.pegb.backoffice.dao.settings.dto.{SystemSettingToInsert, SystemSettingToUpdate, SystemSettingsCriteria}
import tech.pegb.backoffice.dao.settings.entity.SystemSetting
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

class SystemSettingsSqlDao @Inject() (
    val dbApi: DBApi) extends SystemSettingsDao with SqlDao {

  import SystemSettingsSqlDao._

  def insertSystemSetting(dto: SystemSettingToInsert): DaoResponse[SystemSetting] = {
    withTransaction({ implicit cxn: Connection ⇒
      val generatedId = insertSystemSettingQuery
        .on(
          cKey → dto.key,
          cValue → dto.value,
          cCreatedAt → dto.createdAt,
          cCreatedBy → dto.createdBy,
          cUpdatedAt → dto.createdAt, //not nullable in db and same as created at on insertion
          cUpdatedBy → dto.createdBy, //not nullable in db and same as created by on insertion
          cType → dto.`type`,
          cForAndroid → dto.forAndroid,
          cForIos → dto.forIOS,
          cForBackOffice → dto.forBackoffice,
          cExplanation → dto.explanation)
        .executeInsert(scalar[Int].single)

      internalGetSystemSetting(generatedId) match {
        case Some(systemSetting) ⇒ systemSetting
        case None ⇒ throw new Throwable("Failed to fetch created system setting")
      }
    }, s"Failed to create system setting: ${dto}",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not create system setting ${dto.toSmartString}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error("error encountered in [insertSystemSetting]", generic)
          genericDbError(s"Error encountered while inserting system setting [${dto.toSmartString}]")
      })
  }

  def getSystemSettingById(id: Long): DaoResponse[Option[SystemSetting]] = {
    withConnection({ implicit cxn: Connection ⇒
      internalGetSystemSetting(id.toInt)
    }, s"Failed to fetch system setting $id")
  }

  def getSystemSettingByKey(key: String): DaoResponse[Option[SystemSetting]] = {
    withConnection({ implicit connection: Connection ⇒

      val query = SQL(s"SELECT * FROM $TableName WHERE `$cKey` = {$cKey}").on(cKey → key)

      query.executeQuery().as(systemSettingRowParser.singleOpt)
    }, s"Failed to fetch system setting by key $key")

  }

  def getSystemSettingsByCriteria(
    criteria: Option[SystemSettingsCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[SystemSetting]] = withConnection({ implicit connection ⇒

    val whereFilter = generateSystemSettingsWhereFilter(criteria)

    val systemSettingsByCriteriaSql = findSystemSettingsByCriteriaQuery(whereFilter, ordering, limit, offset)

    systemSettingsByCriteriaSql
      .as(systemSettingRowParser.*)

  }, s"Error while retrieving system settings by criteria: $criteria")

  def getSystemSettingsTypes(): DaoResponse[Seq[String]] = withConnection({ implicit connection ⇒
    findSystemSettingsTypesQuery
      .as(findSystemSettingsTypesQuery.defaultParser.*)
      .map(row ⇒ row[String](cType))
  }, s"Error while retrieving system setting types")

  def countSystemSettingsByCriteria(criteria: SystemSettingsCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateSystemSettingsWhereFilter(criteria.toOption)
    val countByCriteriaSql = countSystemSettingsByCriteriaQuery(whereFilter)

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  def updateSystemSettings(id: Long, dto: SystemSettingToUpdate): DaoResponse[SystemSetting] = {
    withTransaction({ implicit cxn: Connection ⇒
      val updateResult = updateQuery(id.toInt, dto).executeUpdate()
      if (updateResult > 0) {
        internalGetSystemSettingForUpdate(id.toInt)
      } else {
        throw new IllegalStateException(s"Update failed. System Settings id: ${id} has been modified by another process.")
      }

    }, s"Failed to update System Settings $id",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update System Settings ${id}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })

  }

  private[sql] def internalGetSystemSetting(
    id: Long)(
    implicit
    cxn: Connection): Option[SystemSetting] = {
    fetchByIdQuery.on(cId → id)
      .executeQuery().as(systemSettingRowParser.singleOpt)

  }

  private[sql] def internalGetSystemSettingForUpdate(
    id: Long)(
    implicit
    cxn: Connection): SystemSetting = {
    fetchByIdQuery.on(cId → id)
      .executeQuery().as(systemSettingRowParser.single)

  }
}

object SystemSettingsSqlDao {
  import SqlDao._

  val TableName = "system_settings"
  val TableAlias = "ss"

  //columns
  val cId = "id"
  val cKey = "key"
  val cValue = "value"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"
  val cType = "type"
  val cForAndroid = "for_android"
  val cForIos = "for_ios"
  val cForBackOffice = "for_backoffice"
  val cExplanation = "explanation"
  val cCreatedBy = "created_by"
  val cUpdatedBy = "updated_by"

  private val insertSystemSettingQuery =
    SQL(s"INSERT INTO $TableName (`$cKey`, `$cValue`, $cCreatedAt, $cCreatedBy, $cUpdatedAt, $cUpdatedBy,$cType, $cForAndroid, $cForIos, $cForBackOffice, $cExplanation) " +
      s"VALUES ({$cKey}, {$cValue}, {$cCreatedAt}, {$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy}, {$cType}, {$cForAndroid}, {$cForIos}, {$cForBackOffice}, {$cExplanation})")

  private def fetchByIdQuery: SqlQuery = {
    val columns = s"$TableAlias.*"
    val filters = s"""WHERE $TableAlias.$cId = {$cId}"""

    SQL(s"""${baseFindSystemSettingsByCriteria(columns, filters)}""".stripMargin)
  }

  private def baseFindSystemSettingsByCriteria(selectColumns: String, filters: String): String = {
    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |$filters""".stripMargin
  }

  private def generateSystemSettingsWhereFilter(mayBeCriteria: Option[SystemSettingsCriteria]): String = {
    mayBeCriteria.map { criteria ⇒
      Seq(
        criteria.id.map(_.toSql(Some(cId), Some(TableAlias))),
        criteria.key.map(_.toSql(Some(cKey), Some(TableAlias))),
        criteria.explanation.map(_.toSql(Some(cExplanation), Some(TableAlias))),
        criteria.forAndroid.map(_.toSql(Some(cForAndroid), Some(TableAlias))),
        criteria.forIOS.map(_.toSql(Some(cForIos), Some(TableAlias))),
        criteria.forBackoffice.map(_.toSql(Some(cForBackOffice), Some(TableAlias))))
        .flatten.toSql
    }.getOrElse("")
  }

  private def countSystemSettingsByCriteriaQuery(filters: String): SqlQuery = {
    val column = "COUNT(*) as n"
    SQL(s"""${baseFindSystemSettingsByCriteria(column, filters)}""")
  }

  private def findSystemSettingsByCriteriaQuery(
    filters: String,
    maybeOrderBy: Option[OrderingSet],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): SqlQuery = {

    val ordering = maybeOrderBy.fold("")(_.toString
      .replace(cKey, s"`$cKey`")
      .replace(cValue, s"`$cValue`")
      .replace(cType, s"`$cType`"))
    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)

    val columns = s"$TableAlias.*"

    SQL(s"""${baseFindSystemSettingsByCriteria(columns, filters)} $ordering $pagination""".stripMargin)
  }

  private def findSystemSettingsTypesQuery(): SqlQuery = {
    SQL(s"""SELECT DISTINCT($cType) FROM $TableName ORDER BY $cType""".stripMargin)
  }

  private def updateQuery(id: Int, dto: SystemSettingToUpdate): SimpleSql[Row] = {
    val paramsBuffer: mutable.Builder[NamedParameter, ArrayBuffer[NamedParameter]] = dto.paramsBuilder
    val filterParam: NamedParameter = NamedParameter(cId, id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))
    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  private val systemSettingRowParser: RowParser[SystemSetting] = row ⇒ {
    Try {
      SystemSetting(
        id = row[Int](cId),
        key = row[String](cKey),
        value = row[String](cValue),
        `type` = row[String](cType),
        explanation = row[Option[String]](cExplanation),
        forAndroid = row[Int](cForAndroid).toBoolean,
        forIOS = row[Int](cForIos).toBoolean,
        forBackoffice = row[Int](cForBackOffice).toBoolean,
        createdAt = row[LocalDateTime](cCreatedAt),
        createdBy = row[String](cCreatedBy),
        updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
        updatedBy = row[Option[String]](cUpdatedBy))
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

}
