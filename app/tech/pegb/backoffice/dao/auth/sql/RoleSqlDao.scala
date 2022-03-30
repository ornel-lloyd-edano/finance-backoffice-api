package tech.pegb.backoffice.dao.auth.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime
import java.util.UUID

import anorm.{AnormException, NamedParameter, Row, SQL}
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.auth.abstraction.RoleDao
import tech.pegb.backoffice.dao.auth.dto.{RoleCriteria, RoleToCreate, RoleToUpdate}
import tech.pegb.backoffice.dao.auth.entity.Role
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

@Singleton
class RoleSqlDao @Inject() (val dbApi: DBApi) extends RoleDao with MostRecentUpdatedAtGetter[Role, RoleCriteria] with SqlDao {

  import RoleSqlDao._
  import SqlDao._

  def createRole(dto: RoleToCreate): DaoResponse[Role] =
    withConnectionAndFlatten({ implicit connection: Connection ⇒

      val parameters = Seq[NamedParameter](
        cId → dto.id,
        cName → dto.name,
        cIsActive → dto.isActive,
        cLevel → dto.level,
        cCreatedBy → dto.createdBy,
        cCreatedAt → dto.createdAt,
        cUpdatedBy → dto.updatedBy,
        cUpdatedAt → dto.updatedAt)

      SQL(
        s"""INSERT INTO $TableName ($cId, $cName, $cIsActive, $cLevel, $cCreatedBy, $cCreatedAt, $cUpdatedBy, $cUpdatedAt)
           |VALUES ({$cId}, {$cName}, {$cIsActive},{$cLevel}, {$cCreatedBy}, {$cCreatedAt}, {$cUpdatedBy}, {$cUpdatedAt})"""
          .stripMargin).on(parameters: _*).execute

      internalGetById(dto.id).fold[DaoResponse[Role]](Left(entityNotFoundError(s"No role created for dto $dto")))(Right(_))

    }, s"error while inserting role $dto",
      {
        case error: java.sql.SQLTimeoutException ⇒
          timeoutError(s"Inserting the role [${dto.toSmartString}] took too long to complete. Confirm later if insert was successful or not.")
        case error: SQLException if isUniqueConstraintViolation(error) ⇒
          entityAlreadyExistsError(s"Failed to insert role [${dto.toSmartString}]. Id or name may already be existing.")
        case error: SQLException ⇒
          logger.error("sql error encountered in [createRole]", error)
          constraintViolationError(s"Failed to insert role [${dto.toSmartString}].")
        case error: java.net.ConnectException ⇒
          connectionFailed(s"Failed to insert role [${dto.toSmartString}]. Connection to database was lost.")
      })

  def updateRole(id: UUID, dto: RoleToUpdate): DaoResponse[Option[Role]] =
    withTransaction({ implicit connection: Connection ⇒
      val result = {
        val paramsBuffer = dto.paramsBuilder
        val filterParam = NamedParameter(cId, id)
        paramsBuffer += filterParam

        val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))

        val params = paramsBuffer.result()
        SQL(preQuery).on(params: _*).executeUpdate()
      }

      if (result.isUpdated) internalGetById(id)
      else throw new IllegalStateException(s"Update failed. role $id has been modified by another process.")

    }, s"error while updating role $dto",
      {
        case error: java.sql.SQLTimeoutException ⇒
          timeoutError(s"Updating the role [${dto.toSmartString}] took too long to complete. Confirm later if insert was successful or not.")
        case error: SQLException if isUniqueConstraintViolation(error) ⇒
          entityAlreadyExistsError(s"Failed to update role [${dto.toSmartString}]. Id or name may already be existing.")
        case error: SQLException ⇒
          logger.error("error encountered in [updateRole]", error)
          constraintViolationError(s"Failed to update role [${dto.toSmartString}]")
        case error: java.net.ConnectException ⇒
          connectionFailed(s"Failed to update role [${dto.toSmartString}]. Connection to database was lost.")
        case error: IllegalStateException ⇒
          preconditionFailed(error.getMessage)
      })

  def countRolesByCriteria(criteria: Option[RoleCriteria]): DaoResponse[Int] =
    withConnection({ implicit connection: Connection ⇒
      val query = SQL(s"SELECT COUNT(*) as n FROM $TableName ${where(criteria)}")
      val row = query.as(query.defaultParser.single)

      row[Int]("n")
    }, s"error while fetching roles count by criteria $criteria",
      {
        case error: AnormException ⇒
          rowParsingError(s"Data cannot be parsed to role correctly. Maybe missing/unknown column or type mismatch.")
        case error: java.sql.SQLTimeoutException ⇒
          timeoutError(s"Counting role with criteria [${criteria.toSmartString}] took too long to complete. Confirm later if insert was successful or not.")
        case error: java.net.ConnectException ⇒
          connectionFailed(s"Failed to count role with criteria [${criteria.toSmartString}]. Connection to database was lost.")
      })

  def getRolesByCriteria(
    criteria: Option[RoleCriteria],
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[Role]] =
    withConnection({ implicit connection: Connection ⇒
      val ord = orderBy.map(_.toString).getOrElse(s"ORDER BY $TableName.$cId ASC")
      val pagination = getPagination(limit, offset)
      val sqlQuery = SQL(s"$selectQuery ${where(criteria)} $ord $pagination")

      logger.info(s"get roles by criteria query = $sqlQuery")
      sqlQuery.as(sqlQuery.defaultParser.*).map(parseRow(_))

    }, s"error while fetching roles by criteria $criteria",
      {
        case error: AnormException ⇒
          rowParsingError(s"Data cannot be parsed to role correctly. Maybe missing/unknown column or type mismatch.")
        case error: java.sql.SQLTimeoutException ⇒
          timeoutError(s"Fetching role with criteria [${criteria.toSmartString}] took too long to complete. Confirm later if insert was successful or not.")
        case error: java.net.ConnectException ⇒
          connectionFailed(s"Failed to fetch role with criteria [${criteria.toSmartString}]. Connection to database was lost.")
      })

  def internalGetById(uuid: UUID)(implicit connection: Connection): Option[Role] =
    selectByIdQuery.on(cId → uuid).as(selectByIdQuery.defaultParser.singleOpt).map(parseRow(_))

  protected def getUpdatedAtColumn: String = cUpdatedAt

  protected def getMainSelectQuery: String = selectQuery

  protected def getRowToEntityParser: Row ⇒ Role = (arg: Row) ⇒ parseRow(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[RoleCriteria]): String = where(criteriaDto)
}

object RoleSqlDao {

  import SqlDao._

  final val TableName = "roles"
  final val TableAlias = "ro"

  final val cId = "id"
  final val cName = "name"
  final val cIsActive = "is_active"
  final val cLevel = "level"
  final val cCreatedBy = "created_by"
  final val cCreatedAt = "created_at"
  final val cUpdatedAt = "updated_at"
  final val cUpdatedBy = "updated_by"

  private val selectQuery = s"SELECT * FROM $TableName"

  private val selectByIdQuery = SQL(s"$selectQuery WHERE $cId={$cId}")

  private def where(mayBeCriteria: Option[RoleCriteria]) =
    mayBeCriteria.map { criteria ⇒
      Seq(
        criteria.id.map(_.toSql()),
        criteria.name.map(_.toSql()),
        criteria.isActive.map(_.toSql()),
        criteria.level.map(_.toSql()),
        criteria.createdBy.map(_.toSql()),
        criteria.createdAt.map(_.toSql()),
        criteria.updatedBy.map(_.toSql()),
        criteria.updatedAt.map(_.toSql())).flatten.toSql
    }.getOrElse("")

  private def parseRow(row: Row): Role =
    Role(
      id = row[UUID](cId),
      name = row[String](cName),
      level = row[Int](cLevel),
      isActive = row[Int](cIsActive),
      createdBy = row[Option[String]](cCreatedBy),
      createdAt = row[Option[LocalDateTime]](cCreatedAt),
      updatedBy = row[Option[String]](cUpdatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt))
}
