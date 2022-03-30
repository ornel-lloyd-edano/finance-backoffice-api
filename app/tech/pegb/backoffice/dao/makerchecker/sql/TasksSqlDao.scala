package tech.pegb.backoffice.dao.makerchecker.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao.queryConditionClause
import tech.pegb.backoffice.dao.helper.Helper._
import tech.pegb.backoffice.dao.makerchecker.abstraction.TasksDao
import tech.pegb.backoffice.dao.makerchecker.dto.{MakerCheckerCriteria, TaskToInsert, TaskToUpdate}
import tech.pegb.backoffice.dao.makerchecker.entity.MakerCheckerTask
import tech.pegb.backoffice.dao.model.{MatchTypes, OrderingSet}
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.{DaoError, SqlDao}
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class TasksSqlDao @Inject() (val dbApi: DBApi, config: AppConfig) extends TasksDao with MostRecentUpdatedAtGetter[MakerCheckerTask, MakerCheckerCriteria] with SqlDao {

  import TasksSqlDao._
  import SqlDao._

  def selectTasksByCriteria(
    criteria: MakerCheckerCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[MakerCheckerTask]] = withConnection({ implicit connection ⇒

    val whereFilter = generateTaskWhereFilter(criteria.toOption)

    val taskByCriteriaSql = findTaskByCriteriaQuery(whereFilter, ordering, limit, offset)

    taskByCriteriaSql.as(taskRowParser.*)

  }, s"Error while retrieving tasks by criteria: $criteria")

  def selectTaskByUUID(uuid: String): DaoResponse[Option[MakerCheckerTask]] = {
    withConnection({ implicit cxn: Connection ⇒
      internalGetTask(uuid)
    }, s"Failed to fetch Task $uuid")
  }

  def countTasks(criteria: MakerCheckerCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateTaskWhereFilter(criteria.toOption)
    val countByCriteriaSql = countTaskByCriteriaQuery(whereFilter)

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  private val rawInsertQuery =
    s"""
       |INSERT INTO ${TasksSqlDao.TableName}
       |(${TasksSqlDao.cUuid}, ${TasksSqlDao.cModule},  ${TasksSqlDao.cAction}, ${TasksSqlDao.cVerb}, ${TasksSqlDao.cUrl}, ${TasksSqlDao.cHeaders},
       |${TasksSqlDao.cBody}, ${TasksSqlDao.cValueToUpdate}, ${TasksSqlDao.cStatus}, ${TasksSqlDao.cMakerLevel}, ${TasksSqlDao.cMakerBu},
       |${TasksSqlDao.cCreatedBy}, ${TasksSqlDao.cCreatedAt}, ${TasksSqlDao.cCheckedBy}, ${TasksSqlDao.cCheckedAt})
       |VALUES
       |({${TasksSqlDao.cUuid}}, {${TasksSqlDao.cModule}}, {${TasksSqlDao.cAction}}, {${TasksSqlDao.cVerb}}, {${TasksSqlDao.cUrl}}, {${TasksSqlDao.cHeaders}},
       |{${TasksSqlDao.cBody}}, {${TasksSqlDao.cValueToUpdate}}, {${TasksSqlDao.cStatus}}, {${TasksSqlDao.cMakerLevel}}, {${TasksSqlDao.cMakerBu}},
       |{${TasksSqlDao.cCreatedBy}}, {${TasksSqlDao.cCreatedAt}}, {${TasksSqlDao.cCheckedBy}}, {${TasksSqlDao.cCheckedAt}})
     """.stripMargin

  private val anormInsertQuery = SQL(rawInsertQuery)

  def insertTask(dto: TaskToInsert): DaoResponse[MakerCheckerTask] = {
    withTransaction({ implicit conn ⇒
      val generatedId = anormInsertQuery.on(
        TasksSqlDao.cUuid → dto.uuid,
        TasksSqlDao.cModule → dto.module,
        TasksSqlDao.cAction → dto.action,
        TasksSqlDao.cVerb → dto.verb,
        TasksSqlDao.cUrl → dto.url,
        TasksSqlDao.cHeaders → dto.headers,
        TasksSqlDao.cBody → dto.body,
        TasksSqlDao.cValueToUpdate → dto.valueToUpdate,
        TasksSqlDao.cStatus → dto.status,
        TasksSqlDao.cMakerLevel → dto.makerLevel,
        TasksSqlDao.cMakerBu → dto.makerBusinessUnit,
        TasksSqlDao.cCreatedBy → dto.createdBy,
        TasksSqlDao.cCreatedAt → dto.createdAt,
        TasksSqlDao.cCheckedBy → dto.checkedBy,
        TasksSqlDao.cCheckedAt → dto.checkedAt).executeInsert(SqlParser.scalar[Int].single)

      dto.asEntity(id = generatedId)

    }, s"Error while inserting settlement and/or settlement lines.", dto.handleException("insert"))
  }

  def updateTask(uuid: String, dto: TaskToUpdate): DaoResponse[MakerCheckerTask] = {

    for {
      _ ← selectTaskByUUID(uuid).fold(
        error ⇒ Left(error),
        results ⇒ if (results.isEmpty) Left(DaoError.EntityNotFoundError(s"Task with id $uuid was not found")) else Right(results.head))

      _ ← withTransaction(
        implicit conn ⇒ {
          updateTaskSql(generateColumnsToSet(dto), uuid)
            .on(TasksSqlDao.cUuid → uuid)
            .executeUpdate()
        },
        "Unexpected exception in updateTask", {
          dto.handleException("update")
        })

      updatedEntity ← {
        selectTaskByUUID(uuid)
          .fold(error ⇒ Left(error), results ⇒ if (results.isEmpty) Left(DaoError.EntityNotFoundError(s"Task with id $uuid was not found")) else Right(results.head))
      }

    } yield {
      updatedEntity
    }
  }

  protected def getUpdatedAtColumn: String = s"${TasksSqlDao.TableAlias}.${TasksSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = baseFindTaskByCriteria("*", "")

  protected def getRowToEntityParser: Row ⇒ MakerCheckerTask = (arg: Row) ⇒ convertRowToMakerCheckerTask(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[MakerCheckerCriteria]): String = generateTaskWhereFilter(criteriaDto)

  private def generateTaskWhereFilter(criteriaOption: Option[MakerCheckerCriteria]): String = {
    criteriaOption match {
      case None ⇒ ""
      case Some(criteria) ⇒
        val idFilter = criteria.id.map(v ⇒ queryConditionClause(v.value, cId, Some(TableAlias), v.operator == MatchTypes.Partial))
        val uuidFilter = criteria.uuid.map(v ⇒ queryConditionClause(v.value, cUuid, Some(TableAlias), v.operator == MatchTypes.Partial))
        val statusFilter = criteria.status.map(v ⇒ queryConditionClause(v.value, cStatus, Some(TableAlias), v.operator == MatchTypes.Partial))
        val moduleFilter = criteria.module.map(v ⇒ queryConditionClause(v.value, cModule, Some(TableAlias), v.operator == MatchTypes.Partial))
        val createAtFilter = fromDateTimeRange(TableAlias, cCreatedAt, criteria.createdAtFrom, criteria.createdAtTo)
        val makerLevelFilter = criteria.makerLevel.map(_.copy(actualColumn = cMakerLevel).toSql(cMakerLevel.some, TableAlias.some))
        val makerBusinessUnitFilter = criteria.makerBusinessUnit.map(v ⇒ queryConditionClause(v.value, cMakerBu, Some(TableAlias), v.operator == MatchTypes.Partial))

        Seq(idFilter, uuidFilter, statusFilter, moduleFilter, createAtFilter, makerLevelFilter, makerBusinessUnitFilter)
          .flatten.toSql
    }
  }

  private[sql] def internalGetTask(uuid: String)(implicit cxn: Connection): Option[MakerCheckerTask] = {
    fetchByIdQuery.on(cUuid → uuid)
      .executeQuery().as(taskRowParser.singleOpt)
  }
}

object TasksSqlDao {
  val TableName = "tasks"
  val TableAlias = "mc"

  val cId = "id"
  val cUuid = "uuid"
  val cModule = "module"
  val cAction = "action"
  val cVerb = "verb"
  val cUrl = "url"
  val cHeaders = "headers"
  val cBody = "body"
  val cValueToUpdate = "value_to_update"
  val cStatus = "status"
  val cMakerLevel = "maker_level"
  val cMakerBu = "maker_business_unit"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cCheckedBy = "checked_by"
  val cCheckedAt = "checked_at"
  val cReason = "reason"
  val cUpdatedAt = "updated_at"

  private def fetchByIdQuery: SqlQuery = {
    val columns = s"$TableAlias.*"
    val filters = s"""WHERE $TableAlias.$cUuid = {$cUuid}"""

    SQL(s"""${baseFindTaskByCriteria(columns, filters)}""".stripMargin)
  }

  private def countTaskByCriteriaQuery(filters: String): SqlQuery = {
    val column = "COUNT(*) as n"
    SQL(s"""${baseFindTaskByCriteria(column, filters)}""")
  }

  private def findTaskByCriteriaQuery(
    filters: String,
    maybeOrderBy: Option[OrderingSet],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): SqlQuery = {

    val ordering = maybeOrderBy.fold("")(_.toString)
    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)

    val columns = s"$TableAlias.*"

    SQL(s"""${baseFindTaskByCriteria(columns, filters)} $ordering $pagination""".stripMargin)
  }

  private def baseFindTaskByCriteria(selectColumns: String, filters: String): String = {
    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |$filters""".stripMargin
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  private val taskRowParser: RowParser[MakerCheckerTask] = row ⇒ {
    Try {
      convertRowToMakerCheckerTask(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private def convertRowToMakerCheckerTask(row: Row): MakerCheckerTask =
    MakerCheckerTask(
      id = row[Int](cId),
      uuid = row[String](cUuid),
      module = row[String](cModule),
      action = row[String](cAction),
      verb = row[String](cVerb),
      url = row[String](cUrl),
      headers = row[String](cHeaders),
      body = row[Option[String]](cBody),
      valueToUpdate = row[Option[String]](cValueToUpdate),
      status = row[String](cStatus),
      createdBy = row[String](cCreatedBy),
      createdAt = row[LocalDateTime](cCreatedAt),
      makerLevel = row[Int](cMakerLevel),
      makerBusinessUnit = row[String](cMakerBu),
      checkedBy = row[Option[String]](cCheckedBy),
      checkedAt = row[Option[LocalDateTime]](cCheckedAt),
      reason = row[Option[String]](cReason),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt))

  private def updateTaskSql(setValues: String, uuid: String) = {
    SQL(s"UPDATE ${TasksSqlDao.TableName} ${TasksSqlDao.TableAlias} " +
      s"SET $setValues WHERE ${TasksSqlDao.TableAlias}.${TasksSqlDao.cUuid} = {${TasksSqlDao.cUuid}}")

  }

  private def generateColumnsToSet(taskToUpdate: TaskToUpdate): String = {
    Seq(
      taskToUpdate.module.map(queryConditionClause(_, TasksSqlDao.cModule, Some(TasksSqlDao.TableAlias))),
      taskToUpdate.action.map(queryConditionClause(_, TasksSqlDao.cAction, Some(TasksSqlDao.TableAlias))),
      taskToUpdate.verb.map(queryConditionClause(_, TasksSqlDao.cVerb, Some(TasksSqlDao.TableAlias))),
      taskToUpdate.url.map(queryConditionClause(_, TasksSqlDao.cUrl, Some(TasksSqlDao.TableAlias))),
      taskToUpdate.headers.map(h ⇒ queryConditionClause(h, TasksSqlDao.cHeaders, Some(TasksSqlDao.TableAlias))),
      taskToUpdate.body.map(b ⇒ queryConditionClause(b, TasksSqlDao.cBody, Some(TasksSqlDao.TableAlias))),
      taskToUpdate.status.map(queryConditionClause(_, TasksSqlDao.cStatus, TableAlias.some)),
      taskToUpdate.createdBy.map(queryConditionClause(_, TasksSqlDao.cCreatedBy, TableAlias.some)),
      taskToUpdate.createdAt.map(queryConditionClause(_, TasksSqlDao.cCreatedAt, TableAlias.some)),
      taskToUpdate.makerLevel.map(queryConditionClause(_, TasksSqlDao.cMakerLevel, TableAlias.some)),
      taskToUpdate.makerBusinessUnit.map(queryConditionClause(_, TasksSqlDao.cMakerBu, TableAlias.some)),
      taskToUpdate.checkedBy.map(queryConditionClause(_, TasksSqlDao.cCheckedBy, TableAlias.some)),
      taskToUpdate.checkedAt.map(queryConditionClause(_, TasksSqlDao.cCheckedAt, TableAlias.some)),
      taskToUpdate.reason.map(queryConditionClause(_, TasksSqlDao.cReason, TableAlias.some))).flatten.mkString(", ")
  }
}
