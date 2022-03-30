package tech.pegb.backoffice.dao.auth.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm.{NamedParameter, Row, RowParser, SQL, SimpleSql, SqlQuery, SqlRequestError}
import cats.implicits._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.auth.abstraction.PermissionDao
import tech.pegb.backoffice.dao.auth.dto.{PermissionCriteria, PermissionToInsert, PermissionToUpdate}
import tech.pegb.backoffice.dao.auth.entity.Permission
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._

import scala.util.Try

class PermissionSqlDao @Inject() (
    val dbApi: DBApi)
  extends PermissionDao with MostRecentUpdatedAtGetter[Permission, PermissionCriteria] with SqlDao {

  import PermissionSqlDao._
  import SqlDao._

  protected def getUpdatedAtColumn: String = s"$TableAlias.$cUpdatedAt"

  protected def getMainSelectQuery: String = qCommonSelect

  protected def getRowToEntityParser: Row ⇒ Permission = (arg: Row) ⇒ rowToPermissionConverter(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[PermissionCriteria]): String = generatePermissionWhereFilter(criteriaDto)

  def insertPermission(dto: PermissionToInsert): DaoResponse[Permission] = {

    withTransaction({ implicit cxn: Connection ⇒
      val uuid = genId()
      SQL(s"INSERT INTO $TableName ($TableFieldStr) VALUES ($ValuesPlaceHolders)")
        .on(
          cId → uuid,
          cBuId → dto.businessUnitId,
          cUserId → dto.userId,
          cRoleId → dto.roleId,
          cScopeId → dto.scopeId,
          cCanWrite → dto.canWrite,
          cIsActive → dto.isActive,
          cCreatedBy → dto.createdBy,
          cCreatedAt → dto.createdAt,
          cUpdatedBy → dto.createdBy,
          cUpdatedAt → dto.createdAt) //not nullable in db and same as created at on insertion
        .executeInsert()

      internalGetPermission(uuid.toString) match {
        case Some(permission) ⇒
          permission
        case None ⇒ throw new Throwable("Failed to fetch created permission")
      }
    }, s"Failed to create permission: $dto",
      handlerPF = {
        case e: SQLException if isUniqueConstraintViolation(e) ⇒
          val errorMessage = s"PermissionToInsert already exists $dto"
          entityAlreadyExistsError(errorMessage)
        case e: SQLException ⇒
          val errorMessage = s"Could not create permission $dto"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error("error encountered in [insertPermission]", generic)
          genericDbError(s"Error encountered while inserting permission $dto")
      })
  }

  def getPermissionByCriteria(criteria: PermissionCriteria, ordering: Option[OrderingSet], limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[Permission]] = withConnection({ implicit connection ⇒
    val whereFilter = generatePermissionWhereFilter(criteria.some)

    val order = ordering.fold(""){ orderingSet ⇒
      OrderingSet(orderingSet.underlying.map(o ⇒ o.copy(field = s"$TableAlias.${o.field}"))).toString}

    val pagination = SqlDao.getPagination(limit, offset)

    val columns = s"$TableAlias.*, ${ScopeSqlDao.TableAlias}.*"

    val permissionByCriteriaSql = SQL(s"""${baseFindPermissionByCriteria(columns, whereFilter)} $order $pagination""".stripMargin)

    permissionByCriteriaSql.as(permissionRowParser.*)

  }, s"Error while retrieving permission by criteria: $criteria")

  def countPermissionByCriteria(criteria: PermissionCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generatePermissionWhereFilter(criteria.some)
    val column = "COUNT(*) as n"
    val countByCriteriaSql = SQL(s"""${baseFindPermissionByCriteria(column, whereFilter)}""")

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")


  def updatePermission(id: String, dto: PermissionToUpdate): DaoResponse[Option[Permission]] = {
    withTransaction({ implicit cxn: Connection ⇒
      for {
        _ ← internalGetPermission(id)
        updateResult = updateQuery(id, dto).executeUpdate()
        updated ← if (updateResult > 0) {
          internalGetPermission(id)
        } else {
          throw new IllegalStateException(s"Update failed. Permission $id has been modified by another process.")
        }
      } yield {
        updated
      }
    }, s"Failed to update Permission $id",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update Permission $id"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })
  }

  def getPermissionIdsByScopeId(scopeId: String): DaoResponse[Seq[String]] = {
    withConnection({ implicit cxn: Connection ⇒
      val columns = s"$TableAlias.$cId"
      val filters = s"WHERE $TableAlias.$cScopeId = {$cScopeId} AND $TableAlias.$cIsActive <> 0;"

      SQL(s"""${baseFindPermissionByCriteria(columns, filters)}""".stripMargin)
        .on(cScopeId → scopeId)
        .executeQuery()
        .as(rowToIdParser.*)
    }, s"Failed to fetch PermissionId by scopeId $scopeId")
  }

  private[sql] def internalGetPermission(id: String)(implicit cxn: Connection): Option[Permission] = {
    val columns = s"$TableAlias.*, ${ScopeSqlDao.TableAlias}.*"
    val filters = s"""WHERE $TableAlias.$cId = {$cId}"""

    SQL(s"""${baseFindPermissionByCriteria(columns, filters)}""".stripMargin)
      .on(cId → id)
      .executeQuery().as(permissionRowParser.singleOpt)

  }
}

object PermissionSqlDao {

  val TableName = "permissions"
  val TableAlias = "p"

  val cId = "id"
  val cBuId = "buId"
  val cUserId = "userId"
  val cRoleId = "roleId"
  val cScopeId = "scopeId"
  val cCanWrite = "canWrite"
  val cIsActive = "is_active"
  val cCreatedBy = "created_by"
  val cUpdatedBy = "updated_by"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"

  val TableFields = Seq(cId, cBuId, cUserId, cRoleId, cScopeId, cCanWrite, cIsActive, cCreatedBy, cUpdatedBy, cCreatedAt, cUpdatedAt)
  val TableFieldStr = TableFields.mkString(",")
  val ValuesPlaceHolders = TableFields.map(c ⇒ s"{$c}").mkString(",")

  private val qCommonSelect =
    s"""SELECT $TableAlias.*, ${ScopeSqlDao.TableAlias}.*
       |FROM $TableName $TableAlias
       |JOIN ${ScopeSqlDao.TableName} ${ScopeSqlDao.TableAlias}
       |ON $TableAlias.$cScopeId = ${ScopeSqlDao.TableAlias}.${ScopeSqlDao.cId}""".stripMargin

  private def updateQuery(id: String, dto: PermissionToUpdate): SimpleSql[Row] = {
    val paramsBuffer = dto.paramsBuilder
    val filterParam = NamedParameter(cId, id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))
    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }

  private def baseFindPermissionByCriteria(selectColumns: String, filters: String): String = {
    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |JOIN ${ScopeSqlDao.TableName} ${ScopeSqlDao.TableAlias}
       |ON $TableAlias.$cScopeId = ${ScopeSqlDao.TableAlias}.${ScopeSqlDao.cId}
       |$filters""".stripMargin
  }

  def hardDeleteByIdQuery(ids: Seq[String]): SqlQuery = {
    val filters = s"""WHERE $cId ${if (ids.nonEmpty) ids.map(i⇒ s"'$i'").mkString("IN (",",",")") else "IS NULL" }"""
    SQL(s"DELETE FROM $TableName $filters")
  }

  private def generatePermissionWhereFilter(mayBeCriteria: Option[PermissionCriteria]): String = {
    import SqlDao._
    mayBeCriteria.map{ criteria ⇒

      Seq(
        criteria.id.map(_.toSql(cId.some, TableAlias.some)),
        criteria.businessId.map(_.toSql(cBuId.some, TableAlias.some)),
        criteria.roleId.map(_.toSql(cRoleId.some, TableAlias.some)),
        criteria.userId.map(_.toSql(cUserId.some, TableAlias.some)),
        criteria.scopeId.map(_.toSql(cScopeId.some, TableAlias.some)),
        criteria.isActive.map(_.toSql(cIsActive.some, TableAlias.some)),
        criteria.createdAt.map(_.toFormattedDateTime.toSql(cCreatedAt.some, TableAlias.some)),
      ).flatten.toSql
    }.getOrElse("")
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  def rowToPermissionConverter(row: Row): Permission = {
    Permission(
      id = row[String](s"$TableName.$cId"),
      businessUnitId = row[Option[String]](s"$TableName.$cBuId"),
      roleId = row[Option[String]](s"$TableName.$cRoleId"),
      userId = row[Option[String]](s"$TableName.$cUserId"),
      isActive = row[Int](s"$TableName.$cIsActive"),
      createdBy = row[Option[String]](s"$TableName.$cCreatedBy"),
      createdAt = row[Option[LocalDateTime]](s"$TableName.$cCreatedAt"),
      updatedBy = row[Option[String]](s"$TableName.$cUpdatedBy"),
      updatedAt = row[Option[LocalDateTime]](s"$TableName.$cUpdatedAt"),
      scope = ScopeSqlDao.rowToScopeConverter(row))
  }

  private val permissionRowParser: RowParser[Permission] = row ⇒ {
    Try {
      rowToPermissionConverter(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private def rowToIdParser: RowParser[String] = row ⇒ {
    anorm.Success(row[String](cId))
  }
}
