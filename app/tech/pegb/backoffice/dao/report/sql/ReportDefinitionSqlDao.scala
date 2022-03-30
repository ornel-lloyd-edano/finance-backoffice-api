package tech.pegb.backoffice.dao.report.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm.{NamedParameter, Row, RowParser, SQL, SimpleSql, SqlQuery, SqlRequestError}
import cats.implicits._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.{RowParsingException, SqlDao}
import tech.pegb.backoffice.dao.SqlDao.genId
import tech.pegb.backoffice.dao.auth.dto.ScopeToInsert
import tech.pegb.backoffice.dao.auth.sql.{BackOfficeUserSqlDao, PermissionSqlDao, ScopeSqlDao}
import tech.pegb.backoffice.dao.model.{MatchTypes, OrderingSet}
import tech.pegb.backoffice.dao.report.abstraction.ReportDefinitionDao
import tech.pegb.backoffice.dao.report.dto.{ReportDefinitionCriteria, ReportDefinitionPermission, ReportDefinitionToInsert, ReportDefinitionToUpdate}
import tech.pegb.backoffice.dao.report.entity.ReportDefinition
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.Logging

import scala.util.Try

class ReportDefinitionSqlDao @Inject() (
    val dbApi: DBApi) extends ReportDefinitionDao
  with SqlDao
  with MostRecentUpdatedAtGetter[ReportDefinition, ReportDefinitionCriteria] {

  import ReportDefinitionSqlDao._

  protected def getUpdatedAtColumn: String = s"${ReportDefinitionSqlDao.TableAlias}.${ReportDefinitionSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = ReportDefinitionSqlDao.qCommonSelect

  protected def getRowToEntityParser: Row ⇒ ReportDefinition = (arg: Row) ⇒ convertRowToReportDefinition(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[ReportDefinitionCriteria]): String = generateReportDefinitionWhereFilter(criteriaDto)

  def createReportDefinition(record: ReportDefinitionToInsert, scopeToInsert: ScopeToInsert): DaoResponse[ReportDefinition] = {

    withTransaction({ implicit cxn: Connection ⇒
      val reportDefId = genId().toString
      val scopeId = genId().toString

      ScopeSqlDao.insertSql
        .on(ScopeSqlDao.buildParametersForCreate(scopeId, scopeToInsert): _*)
        .executeInsert()

      insertReportDefinitionQuery
        .on(buildParametersForReportDef(reportDefId, record): _*)
        .executeInsert()

      internalGetReportDefinition(reportDefId) match {
        case Some(reportDefinition) ⇒
          reportDefinition
        case None ⇒ throw new Throwable("Failed to created fetch report definition")
      }
    }, s"Failed to create report definition: ${record.toSmartString}",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not create report definition ${record.toSmartString}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error("error encountered in [createReportDefinition]", generic)
          genericDbError(s"error encountered while creating report definition [${record.toSmartString}]")
      })
  }

  def getReportDefinitionByCriteria(
    criteria: ReportDefinitionCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[ReportDefinition]] = withConnection({ implicit connection ⇒

    val whereFilter = generateReportDefinitionWhereFilter(criteria.some)

    val reportDefinitionAllSql = findAllQuery(whereFilter, ordering, limit, offset)

    reportDefinitionAllSql
      .as(reportDefinitionRowParser.*)

  }, s"Error while retrieving all report definition")

  def countReportDefinitionByCriteria(criteria: ReportDefinitionCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateReportDefinitionWhereFilter(criteria.some)
    val countByCriteriaSql = countReportDefinitionByCriteriaQuery(whereFilter)

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  def getReportDefinitionById(id: String): DaoResponse[Option[ReportDefinition]] = {
    withConnection({ implicit cxn: Connection ⇒
      internalGetReportDefinition(id)
    }, s"Failed to fetch report definition $id")
  }

  def deleteReportDefinitionById(reportDefId: String, scopeId: Option[String], permissionId: Seq[String]): DaoResponse[Boolean] = {
    withTransaction({ implicit cxn: Connection ⇒
      val deletedRecords = deleteByIdQuery.on(cUuid → reportDefId).executeUpdate()

      logger.debug(s"deleting permissions with ids in ${permissionId.defaultMkString}")
      //NOTE it seems anorm does not parse an array or list to correct named parameter
      PermissionSqlDao.hardDeleteByIdQuery(permissionId).executeUpdate()

      scopeId.foreach { id ⇒
        logger.debug(s"deleting scope with id $id")
        ScopeSqlDao.hardDeleteByIdQuery.on(ScopeSqlDao.cId → id).executeUpdate()
      }

      deletedRecords.isUpdated
    }, s"Failed to create report definition: ${reportDefId}",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not delete report definition ${reportDefId}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error("error encountered in [deleteReportDefinitionById]", generic)
          genericDbError(s"error encountered when deleting report definition id = $reportDefId")
      })
  }

  def updateReportDefinitionById(id: String, reportDefinitionToUpdate: ReportDefinitionToUpdate): DaoResponse[Option[ReportDefinition]] = {
    withTransaction({ implicit cxn: Connection ⇒
      for {
        _ ← internalGetReportDefinition(id)
        updateResult = updateQuery(id, reportDefinitionToUpdate).executeUpdate()
        updated ← if (updateResult > 0) {
          internalGetReportDefinition(id)
        } else {
          throw new Exception(s"Updating report definition ${id} encountered an exception.")
        }
      } yield {
        updated
      }
    }, s"Failed to update Report Definition $id",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update Report definition ${id}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case error ⇒
          logger.error("error encountered in [updateReportDefinitionById]", error)
          genericDbError(s"Error encountered when updating report definition id = $id")
      })
  }

  private[sql] def internalGetReportDefinition(
    id: String)(
    implicit
    cxn: Connection): Option[ReportDefinition] = {
    fetchByIdQuery.on(cUuid → id)
      .executeQuery().as(reportDefinitionRowParser.singleOpt)
  }

  private def generateReportDefinitionWhereFilter(maybeCriteria: Option[ReportDefinitionCriteria]): String = {
    import SqlDao._

    maybeCriteria.map { criteria ⇒
      val idFilter = criteria.id.map { cf ⇒
        queryConditionClause(cf.value, cUuid, Some(TableAlias), cf.operator == MatchTypes.Partial)
      }

      val nameFilter = criteria.name.map(queryConditionClause(_, cName, Some(TableAlias)))
      val titleFilter = criteria.title.map(queryConditionClause(_, cTitle, Some(TableAlias)))

      val descriptionFilter = criteria.description.map { cf ⇒
        queryConditionClause(cf.value, cDescription, Some(TableAlias), cf.operator == MatchTypes.Partial)
      }

      Seq(idFilter, nameFilter, titleFilter, descriptionFilter)
        .flatten.toSql
    }.getOrElse("")

  }

  private def reportDefinitionPermissionParser(row: Row) = Try {
    ReportDefinitionPermission(
      reportDefId = row[String](s"${ReportDefinitionSqlDao.TableName}.${ReportDefinitionSqlDao.cUuid}"),
      reportDefName = row[String](s"${ReportDefinitionSqlDao.TableName}.${ReportDefinitionSqlDao.cName}"),
      reportDefTitle = row[String](s"${ReportDefinitionSqlDao.TableName}.${ReportDefinitionSqlDao.cTitle}"),
      scopeId = row[String](s"${PermissionSqlDao.TableName}.${PermissionSqlDao.cScopeId}"),
      businessUserId = row[String](s"${PermissionSqlDao.TableName}.${PermissionSqlDao.cBuId}"),
      roleId = row[String](s"${PermissionSqlDao.TableName}.${PermissionSqlDao.cRoleId}"))
  }

  def getReportDefinitionPermissionByBackOfficeUserName(backOfficeUserName: String) =
    withTransaction({ implicit cxn: Connection ⇒
      val rawSql =
        s"""
           |SELECT
           |${ReportDefinitionSqlDao.TableAlias}.${ReportDefinitionSqlDao.cUuid},
           |${ReportDefinitionSqlDao.TableAlias}.${ReportDefinitionSqlDao.cName},
           |${ReportDefinitionSqlDao.TableAlias}.${ReportDefinitionSqlDao.cTitle},
           |${PermissionSqlDao.TableAlias}.${PermissionSqlDao.cScopeId},
           |${PermissionSqlDao.TableAlias}.${PermissionSqlDao.cBuId},
           |${PermissionSqlDao.TableAlias}.${PermissionSqlDao.cRoleId}
           |FROM ${PermissionSqlDao.TableName} as ${PermissionSqlDao.TableAlias}
           |INNER JOIN ${ScopeSqlDao.TableName} as ${ScopeSqlDao.TableAlias}
           |ON ${PermissionSqlDao.TableAlias}.${PermissionSqlDao.cScopeId} = ${ScopeSqlDao.TableAlias}.${ScopeSqlDao.cId}
           |INNER JOIN
           |${BackOfficeUserSqlDao.TableName} as ${BackOfficeUserSqlDao.TableAlias}
           |ON ${BackOfficeUserSqlDao.TableAlias}.${BackOfficeUserSqlDao.cBuId} = ${PermissionSqlDao.TableAlias}.${PermissionSqlDao.cBuId}
           |AND ${BackOfficeUserSqlDao.TableAlias}.${BackOfficeUserSqlDao.cRoleId} = ${PermissionSqlDao.TableAlias}.${PermissionSqlDao.cRoleId}
           |INNER JOIN ${ReportDefinitionSqlDao.TableName} as ${ReportDefinitionSqlDao.TableAlias}
           |ON ${ReportDefinitionSqlDao.TableAlias}.${ReportDefinitionSqlDao.cName} = ${ScopeSqlDao.cName}
           |WHERE ${ScopeSqlDao.cParentId} = (SELECT ${ScopeSqlDao.cId} FROM ${ScopeSqlDao.TableName} WHERE ${ScopeSqlDao.cName} = 'reporting' AND ${ScopeSqlDao.cIsActive} = 1)
           |AND ${BackOfficeUserSqlDao.TableAlias}.${BackOfficeUserSqlDao.cUsername} = '$backOfficeUserName'
           |ORDER BY ${ReportDefinitionSqlDao.TableAlias}.${ReportDefinitionSqlDao.cName}
         """.stripMargin

      logger.debug(s"report definition permission query = $rawSql")
      val sqlQuery = SQL(rawSql)
      val results = sqlQuery.as(sqlQuery.defaultParser.*)
        .map(reportDefinitionPermissionParser(_)
          .fold(_ ⇒ throw new RowParsingException("Unable to parse result of report definition and permission join query"), identity))
      logger.debug(s"query results: ${results.defaultMkString}")

      results
    }, s"Failed to fetch report definition permission of user [${backOfficeUserName}]",
      handlerPF = {
        case err: RowParsingException ⇒
          logger.error(s"Row parsing exception in ${this.getClass.getSimpleName}.getReportDefinitionPermissionByBackOfficeUserName", err)
          rowParsingError("Error encountered when parsing report definition")
        case e: SQLException ⇒
          val errorMessage = s"Could not fetch report definition permission of user [${backOfficeUserName}]"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case error ⇒
          logger.error("error encountered in [getReportDefinitionPermissionByBackOfficeUserName]", error)
          genericDbError(s"Error encountered when fetching report definition permission of user [$backOfficeUserName]")
      })
}

object ReportDefinitionSqlDao extends Logging {

  val TableName = "report_definitions"
  val TableAlias = "rd"

  val cUuid = "id"
  val cName = "report_name"
  val cTitle = "report_title"
  val cDescription = "report_description"
  val cColumns = "report_columns"
  val cParameters = "parameters"
  val cJoins = "joins"
  val cGrouping = "grouping_columns"
  val cOrdering = "ordering"
  val cPaginated = "paginated"
  val cSql = "raw_sql"
  val cCreatedAt = "created_at"
  val cCreatedBy = "created_by"
  val cUpdatedAt = "updated_at"
  val cUpdatedBy = "updated_by"

  val TableFields = Seq(cUuid, cName, cTitle, cDescription, cColumns, cParameters, cJoins, cGrouping, cOrdering, cPaginated, cSql, cCreatedAt, cCreatedBy, cUpdatedAt, cUpdatedBy)
  val TableFieldStr = TableFields.mkString(",")
  val ValuesPlaceHolders = TableFields.map(c ⇒ s"{$c}").mkString(",")

  private val qCommonSelect = s"SELECT $TableAlias.* FROM $TableName $TableAlias"

  private def buildParametersForReportDef(
    reportDefUUID: String,
    reportDefToInsert: ReportDefinitionToInsert): Seq[NamedParameter] =
    Seq[NamedParameter](
      cUuid → reportDefUUID,
      cName → reportDefToInsert.name,
      cTitle → reportDefToInsert.title,
      cDescription → reportDefToInsert.description,
      cColumns → reportDefToInsert.columns.map(_.toString()),
      cParameters → reportDefToInsert.parameters.map(_.toString()),
      cJoins → reportDefToInsert.joins.map(_.toString()),
      cGrouping → reportDefToInsert.grouping.map(_.toString()),
      cOrdering → reportDefToInsert.ordering.map(_.toString()),
      cPaginated → reportDefToInsert.paginated,
      cSql → reportDefToInsert.sql,
      cCreatedBy → reportDefToInsert.createdBy,
      cCreatedAt → reportDefToInsert.createdAt,
      cUpdatedBy → reportDefToInsert.createdBy,
      cUpdatedAt → reportDefToInsert.createdAt)

  private val insertReportDefinitionQuery =
    SQL(s"INSERT INTO $TableName ($TableFieldStr) VALUES ($ValuesPlaceHolders)")

  private def baseFindReportDefinitionByCriteria(selectColumns: String, filters: String): String = {
    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |$filters""".stripMargin
  }

  private def countReportDefinitionByCriteriaQuery(filters: String): SqlQuery = {
    val column = "COUNT(*) as n"
    SQL(s"""${baseFindReportDefinitionByCriteria(column, filters)}""")
  }

  private def findAllQuery(
    filters: String,
    maybeOrderBy: Option[OrderingSet],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): SqlQuery = {

    val ordering = maybeOrderBy.fold("")(_.toString)
    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)

    val columns = s"$TableAlias.*"
    val rawQuery = s"""${baseFindReportDefinitionByCriteria(columns, filters)} $ordering $pagination""".stripMargin
    logger.info(s"get report definitions query = $rawQuery")
    SQL(rawQuery)
  }

  private val fetchByIdQuery: SqlQuery = {
    val columns = s"$TableAlias.*"
    val filters = s"""WHERE $TableAlias.$cUuid = {$cUuid}"""

    SQL(s"""${baseFindReportDefinitionByCriteria(columns, filters)}""".stripMargin)
  }

  private val deleteByIdQuery: SqlQuery = {
    val filters = s"""WHERE $TableName.$cUuid = {$cUuid}"""
    val rawSql = s"DELETE FROM $TableName $filters"
    logger.info(s"delete report definitions query = $rawSql")
    SQL(rawSql)
  }

  private def updateQuery(id: String, dto: ReportDefinitionToUpdate): SimpleSql[Row] = {
    val paramsBuffer = dto.paramsBuilder
    val filterParam = NamedParameter(cUuid, id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"), disableOptimisticLock = true)

    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  private def convertRowToReportDefinition(row: Row): ReportDefinition = {
    ReportDefinition(
      id = row[String](cUuid),
      name = row[String](cName),
      title = row[String](cTitle),
      description = row[String](cDescription),
      columns = row[Option[String]](cColumns),
      parameters = row[Option[String]](cParameters),
      joins = row[Option[String]](cJoins),
      grouping = row[Option[String]](cGrouping),
      ordering = row[Option[String]](cOrdering),
      paginated = row[Int](cPaginated).toBoolean,
      sql = row[String](cSql),
      createdAt = row[LocalDateTime](cCreatedAt),
      createdBy = row[String](cCreatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
      updatedBy = row[Option[String]](cUpdatedBy))
  }

  private val reportDefinitionRowParser: RowParser[ReportDefinition] = row ⇒ {
    Try {
      convertRowToReportDefinition(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }
}
