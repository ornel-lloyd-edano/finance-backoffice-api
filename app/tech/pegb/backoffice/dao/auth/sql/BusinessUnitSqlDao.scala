package tech.pegb.backoffice.dao.auth.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime
import java.util.UUID

import anorm._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.auth.abstraction.BusinessUnitDao
import tech.pegb.backoffice.dao.auth.dto.{BusinessUnitCriteria, BusinessUnitToInsert}
import tech.pegb.backoffice.dao.auth.dto.BusinessUnitToUpdate
import tech.pegb.backoffice.dao.auth.entity.BusinessUnit
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

class BusinessUnitSqlDao @Inject() (
    val dbApi: DBApi)
  extends BusinessUnitDao with SqlDao with MostRecentUpdatedAtGetter[BusinessUnit, BusinessUnitCriteria] {
  import SqlDao._
  import BusinessUnitSqlDao._

  protected def getUpdatedAtColumn: String = cUpdatedAt

  protected def getMainSelectQuery: String = commonSelectQuery

  protected def getRowToEntityParser: Row ⇒ BusinessUnit = parseRowToEntity

  protected def getWhereFilterFromCriteria(criteriaDto: Option[BusinessUnitCriteria]): String = generateWhereFilter(criteriaDto)

  def create(dto: BusinessUnitToInsert): DaoResponse[BusinessUnit] = withConnection(
    { implicit conn ⇒
      val insertSql =
        s"""
         |INSERT INTO ${TableName}
         |($cId,   $cName,   $cIsActive,   $cCreatedAt,   $cCreatedBy,   $cUpdatedAt,   $cUpdatedBy)
         |VALUES
         |({$cId}, {$cName}, {$cIsActive}, {$cCreatedAt}, {$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy});
       """.stripMargin

      val generatedUUID = UUID.randomUUID()
      SQL(insertSql)
        .on(cId → generatedUUID, cName → dto.name, cIsActive → dto.isActive,
          cCreatedBy → dto.createdBy, cCreatedAt → dto.createdAt,
          cUpdatedBy → dto.updatedBy, cUpdatedAt → dto.updatedAt)
        .execute()

      internalGet(generatedUUID.toString) match {
        case Some(result) ⇒ result
        case None ⇒ throw new Exception(s"Failed to execute insert")
      }
    },
    s"Failed to insert business unit: ${dto.toSmartString}",
    {
      case error: java.sql.SQLTimeoutException ⇒
        timeoutError(s"Inserting the business unit [${dto.toSmartString}] took too long to complete. Confirm later if insert was successful or not.")
      case error: SQLException if isUniqueConstraintViolation(error) ⇒
        entityAlreadyExistsError(s"Failed to insert business unit [${dto.toSmartString}]. Id or name may already be existing.")
      case error: SQLException ⇒
        constraintViolationError(s"Failed to insert business unit [${dto.toSmartString}]. A value assigned to a column is not allowed.")
      case error: java.net.ConnectException ⇒
        connectionFailed(s"Failed to insert business unit [${dto.toSmartString}]. Connection to database was lost.")
    })

  def getBusinessUnitsByCriteria(
    dto: BusinessUnitCriteria,
    ordering: Option[OrderingSet],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): DaoResponse[Seq[BusinessUnit]] = withConnection(
    { implicit conn ⇒

      val whereClause = generateWhereFilter(Some(dto))
      val orderByClause = ordering.map(_.toString).getOrElse(
        s"ORDER BY ${TableAlias}.${cId} ASC")

      val paginationClause = getPagination(maybeLimit, maybeOffset)

      val query = SQL(
        s"""
           |$commonSelectQuery
           |$whereClause
           |$orderByClause
           |$paginationClause
         """.stripMargin)

      query.executeQuery().as(rowParser.*)
    },
    s"Unexpected error on get business units by criteria [${dto.toSmartString}]",
    {
      case error: AnormException ⇒
        rowParsingError(s"Data cannot be parsed to business unit correctly. Maybe missing/unknown column or type mismatch.")
      case error: java.sql.SQLTimeoutException ⇒
        timeoutError(s"Counting business units with criteria [${dto.toSmartString}] took too long to complete. Confirm later if insert was successful or not.")
      case error: java.net.ConnectException ⇒
        connectionFailed(s"Failed to count business units with criteria [${dto.toSmartString}]. Connection to database was lost.")
    })

  def countBusinessUnitsByCriteria(dto: BusinessUnitCriteria): DaoResponse[Int] = withConnection(
    { implicit conn ⇒
      val whereClause = generateWhereFilter(Some(dto))

      val query = SQL(
        s"""
           |SELECT COUNT($cId) as n FROM $TableName as $TableAlias $whereClause
         """.stripMargin)

      query.as(query.defaultParser.singleOpt).map(row ⇒ row[Int]("n")).get

    }, s"Unexpected error on count business units by criteria [${dto.toSmartString}]",
    {
      case error: java.sql.SQLTimeoutException ⇒
        timeoutError(s"Counting business units with criteria [${dto.toSmartString}] took too long to complete. Confirm later if insert was successful or not.")
      case error: java.net.ConnectException ⇒
        connectionFailed(s"Failed to count business units with criteria [${dto.toSmartString}]. Connection to database was lost.")
    })

  def update(id: String, dto: BusinessUnitToUpdate): DaoResponse[Option[BusinessUnit]] = withConnection(
    { implicit conn ⇒

      internalGet(id).flatMap { _ ⇒

        val paramsBuffer = dto.paramsBuilder
        val filterParam = NamedParameter(cId, id)
        paramsBuffer += filterParam

        val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))

        val params = paramsBuffer.result()
        val updateResult = SQL(preQuery).on(params: _*).executeUpdate()

        if (updateResult.isUpdated) {
          internalGet(id)
        } else {
          throw new IllegalStateException(s"Update failed. Business unit ${id} has been modified by another process.")
        }
      }
    },
    s"Failed to update business unit [${id}]",
    {
      case error: java.sql.SQLTimeoutException ⇒
        timeoutError(s"Updating the business unit [${dto.toSmartString}] took too long to complete. Confirm later if update was successful or not.")
      case error: SQLException ⇒
        entityAlreadyExistsError(s"Failed to update business unit [${dto.toSmartString}]. Id or name may already be existing.")
      case error: java.net.ConnectException ⇒
        connectionFailed(s"Failed to update business unit [${dto.toSmartString}]. Connection to database was lost.")
      case error: IllegalStateException ⇒
        preconditionFailed(error.getMessage)
    })

  private def internalGet(id: String)(implicit conn: Connection): Option[BusinessUnit] = {
    val query = SQL(
      s"""
         |SELECT * FROM $TableName as $TableAlias WHERE $cId = {$cId};
         """.stripMargin)

    query.on(cId → id).executeQuery().as(rowParser.singleOpt)
  }

  private val rowParser: RowParser[BusinessUnit] = row ⇒ Try {
    parseRowToEntity(row)
  }.fold(
    exc ⇒ anorm.Error(SqlRequestError(exc)),
    anorm.Success(_))

  private def parseRowToEntity(row: Row) = {
    BusinessUnit(
      id = row[String](cId),
      name = row[String](cName),
      isActive = row[Int](cIsActive),
      createdAt = row[Option[LocalDateTime]](cCreatedAt),
      createdBy = row[Option[String]](cCreatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
      updatedBy = row[Option[String]](cUpdatedBy))
  }

  protected def generateWhereFilter(maybeCriteria: Option[BusinessUnitCriteria]): String = {
    maybeCriteria.map { criteria ⇒
      Seq(
        criteria.id.map(_.toSql(Some(cId), Some(TableAlias))),
        criteria.name.map(_.toSql(Some(cName), Some(TableAlias))),
        criteria.isActive.map(_.toSql(Some(cIsActive), Some(TableAlias))),
        criteria.createdAt.map(_.toSql(Some(cCreatedAt), Some(TableAlias))),
        criteria.createdBy.map(_.toSql(Some(cCreatedBy), Some(TableAlias))),
        criteria.updatedAt.map(_.toSql(Some(cUpdatedAt), Some(TableAlias))),
        criteria.updatedBy.map(_.toSql(Some(cUpdatedBy), Some(TableAlias))))
        .flatten.toSql
    }
  }.getOrElse("")

}

object BusinessUnitSqlDao {

  val TableName = "business_units"
  val TableAlias = "bu"

  val cId = "id"
  val cName = "name"
  val cIsActive = "is_active"
  val cCreatedAt = "created_at"
  val cCreatedBy = "created_by"
  val cUpdatedAt = "updated_at"
  val cUpdatedBy = "updated_by"

  val commonSelectQuery =
    s"""
       |SELECT * FROM $TableName as $TableAlias
     """.stripMargin
}
