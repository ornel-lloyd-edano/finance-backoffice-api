package tech.pegb.backoffice.dao.auth.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm.{NamedParameter, Row, RowParser, SQL, SimpleSql, SqlQuery, SqlRequestError}
import cats.implicits._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.auth.abstraction.ScopeDao
import tech.pegb.backoffice.dao.auth.dto.{ScopeCriteria, ScopeToInsert, ScopeToUpdate}
import tech.pegb.backoffice.dao.auth.entity.Scope
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._

import scala.util.Try

class ScopeSqlDao @Inject() (
    val dbApi: DBApi)
  extends ScopeDao with MostRecentUpdatedAtGetter[Scope, ScopeCriteria] with SqlDao {

  import ScopeSqlDao._
  import SqlDao._

  protected def getUpdatedAtColumn: String = s"${ScopeSqlDao.TableAlias}.${ScopeSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = ScopeSqlDao.qCommonSelect

  protected def getRowToEntityParser: Row ⇒ Scope = (arg: Row) ⇒ rowToScopeConverter(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[ScopeCriteria]): String = generateScopeWhereFilter(criteriaDto)

  def insertScope(dto: ScopeToInsert): DaoResponse[Scope] = {

    withTransaction({ implicit cxn: Connection ⇒
      val uuid = genId()
      insertSql
        .on(buildParametersForCreate(uuid.toString, dto): _*) //not nullable in db and same as created at on insertion
        .executeInsert()

      internalGetScope(uuid.toString) match {
        case Some(scope) ⇒
          scope
        case None ⇒ throw new Throwable("Failed to fetch created scope")
      }
    }, s"Failed to create scope: $dto",
      handlerPF = {
        case e: SQLException if isUniqueConstraintViolation(e)⇒
          val errorMessage = s"ScopeToCreate already exists $dto"
          logger.error(errorMessage, e)
          entityAlreadyExistsError(errorMessage)
        case e: SQLException ⇒
          val errorMessage = s"Could not create scope $dto"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error("error encountered in [insertScope]", generic)
          genericDbError(s"error encountered while inserting scope $dto")
      })
  }

  def getScopeByCriteria(criteria: ScopeCriteria, ordering: Option[OrderingSet], limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[Scope]] = withConnection({ implicit connection ⇒
    val whereFilter = generateScopeWhereFilter(criteria.some)

    val order = ordering.fold("")(_.toString)
    val pagination = SqlDao.getPagination(limit, offset)

    val columns = s"$TableAlias.*"

    val scopeByCriteriaSql = SQL(s"""${baseFindScopeByCriteria(columns, whereFilter)} $order $pagination""".stripMargin)

    scopeByCriteriaSql.as(scopeRowParser.*)

  }, s"Error while retrieving scope by criteria: $criteria")

  def countScopeByCriteria(criteria: ScopeCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateScopeWhereFilter(criteria.some)
    val column = "COUNT(*) as n"
    val countByCriteriaSql = SQL(s"""${baseFindScopeByCriteria(column, whereFilter)}""")

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  def updateScope(id: String, dto: ScopeToUpdate): DaoResponse[Option[Scope]] = {
    withTransaction({ implicit cxn: Connection ⇒
      for {
        _ ← internalGetScope(id)
        updateResult = updateQuery(id, dto).executeUpdate()
        updated ← if (updateResult > 0) {
          internalGetScope(id)
        } else {
          throw new IllegalStateException(s"Update failed. Scope $id has been modified by another process.")
        }
      } yield {
        updated
      }
    }, s"Failed to update Scope $id",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update Scope $id"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })
  }

  def getScopeIdByName(name: String): DaoResponse[Option[String]] = {
    withConnection({ implicit cxn: Connection ⇒
      val columns = s"$TableAlias.$cId"
      val filters = s"""WHERE $TableAlias.$cName = {$cName} and $TableAlias.$cIsActive = 1"""

      SQL(s"""${baseFindScopeByCriteria(columns, filters)}""".stripMargin)
        .on(cName → name)
        .executeQuery()
        .as(rowToIdParser.singleOpt)

    }, s"Failed to fetch ScopeId by name $name")
  }

  private[sql] def internalGetScope(id: String)(implicit cxn: Connection): Option[Scope] = {
    val columns = s"$TableAlias.*"
    val filters = s"""WHERE $TableAlias.$cId = {$cId}"""

    SQL(s"""${baseFindScopeByCriteria(columns, filters)}""".stripMargin)
      .on(cId → id)
      .executeQuery().as(scopeRowParser.singleOpt)

  }
}

object ScopeSqlDao {

  val TableName = "scopes"
  val TableAlias = "s"

  val cId = "id"
  val cParentId = "parentId"
  val cName = "name"
  val cDescription = "description"
  val cIsActive = "is_active"
  val cCreatedBy = "created_by"
  val cUpdatedBy = "updated_by"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"

  val TableFields = Seq(cId, cParentId, cName, cDescription, cIsActive, cCreatedBy, cUpdatedBy, cCreatedAt, cUpdatedAt,
    "status", "cBy", "uBy", "cDate", "uDate") //TODO: Remove this when auth is fully migrated
  val TableFieldStr = TableFields.mkString(",")
  val ValuesPlaceHolders = TableFields.map(c ⇒ s"{$c}").mkString(",")

  private val qCommonSelect = s"SELECT $TableAlias.* FROM $TableName $TableAlias"

  final val insertSql = SQL(s"INSERT INTO $TableName ($TableFieldStr) VALUES ($ValuesPlaceHolders)")

  private def updateQuery(id: String, dto: ScopeToUpdate): SimpleSql[Row] = {
    val paramsBuffer = dto.paramsBuilder
    val filterParam = NamedParameter(cId, id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))
    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }

  private def baseFindScopeByCriteria(selectColumns: String, filters: String): String = {
    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |$filters""".stripMargin
  }

  private def generateScopeWhereFilter(mayBeCriteria: Option[ScopeCriteria]): String = {
    import SqlDao._
    mayBeCriteria.map{ criteria ⇒

      Seq(
        criteria.id.map(_.toSql(cId.some, TableAlias.some)),
        criteria.parentId.map(_.toSql(cParentId.some, TableAlias.some)),
        criteria.name.map(_.toSql(cName.some, TableAlias.some)),
        criteria.description.map(_.toSql(cDescription.some, TableAlias.some)),
        criteria.isActive.map(_.toSql(cIsActive.some, TableAlias.some)),
      ).flatten.toSql
    }.getOrElse("")
  }

  val hardDeleteByIdQuery: SqlQuery = {
    val filters = s"""WHERE $cId = {$cId}"""
    SQL(s"DELETE FROM $TableName $filters")
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  def rowToScopeConverter(row:Row): Scope = {
    Scope(
      id = row[String](s"$TableName.$cId"),
      parentId = row[Option[String]](s"$TableName.$cParentId"),
      name = row[String](s"$TableName.$cName"),
      description = row[Option[String]](s"$TableName.$cDescription"),
      isActive = row[Int](s"$TableName.$cIsActive"),
      createdBy = row[Option[String]](s"$TableName.$cCreatedBy"),
      createdAt = row[Option[LocalDateTime]](s"$TableName.$cCreatedAt"),
      updatedBy = row[Option[String]](s"$TableName.$cUpdatedBy"),
      updatedAt = row[Option[LocalDateTime]](s"$TableName.$cUpdatedAt"))
  }

  private val scopeRowParser: RowParser[Scope] = row ⇒ {
    Try {
      rowToScopeConverter(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private def rowToIdParser: RowParser[String] = row ⇒ {
    anorm.Success(row[String](cId))
  }

  def buildParametersForCreate(id: String, dto: ScopeToInsert): Seq[NamedParameter] =
    Seq[NamedParameter](
      cId → id,
      cParentId → dto.parentId,
      cName → dto.name,
      cDescription → dto.description,
      cIsActive → dto.isActive,
      cCreatedBy → dto.createdBy,
      cCreatedAt → dto.createdAt,
      cUpdatedBy → dto.createdBy,
      cUpdatedAt → dto.createdAt,
      //TODO: Remove this when auth is fully migrated
      "status" → dto.isActive,
      "cBy" → dto.createdBy,
      "uBy" → dto.createdBy,
      "cDate" → dto.createdAt,
      "uDate" → dto.createdAt,
    )
}
