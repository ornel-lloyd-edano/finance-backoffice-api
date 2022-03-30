package tech.pegb.backoffice.dao.customer.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm._
import cats.syntax.either._
import org.apache.commons.text.StringEscapeUtils
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.customer.abstraction.EmployerDao
import tech.pegb.backoffice.dao.customer.dto.EmployerToInsert
import tech.pegb.backoffice.dao.customer.entity.Employer

import scala.util.Try

class EmployerSqlDao(protected val dbApi: DBApi, kafkaDBSyncService: KafkaDBSyncService) extends EmployerDao with SqlDao {

  import EmployerSqlDao._

  def create(employerToInsert: EmployerToInsert): DaoResponse[Employer] =

    withConnectionAndFlatten({ implicit connection ⇒
      val parameters = buildParameters(employerToInsert)
      for {
        _ ← Try(createQuery.on(parameters: _*).execute())
          .toEither.leftMap({
            case e: SQLException if isUniqueConstraintViolation(e) ⇒
              val msg = s"employer name is not unique : ${employerToInsert.name}"
              logger.error(StringEscapeUtils.escapeJava(msg), e)
              entityAlreadyExistsError(msg)
            case e @ (_: SQLException | _: AnormException) ⇒
              val msg = s"Unexpected SQL exception in ${this.getClass}.create"
              logger.error(StringEscapeUtils.escapeJava(msg), e)
              genericDbError(msg)
          })
        created ← findByNameInternal(employerToInsert.name)
      } yield {
        kafkaDBSyncService.sendInsert(TableName, created)
        created
      }

    }, s"Error while creating employer ${employerToInsert.name}")

  def getAll: DaoResponse[Set[Employer]] =
    withConnection(implicit connection ⇒ findAllInternal, "Error while retrieving all employers")

  private def findByNameInternal(name: String)(implicit connection: Connection): DaoResponse[Employer] = {
    findByNameQuery
      .on('employer_name -> name)
      .as(findByNameQuery.defaultParser.*)
      .headOption
      .map(convertRowToEmployer)
      .toRight(entityNotFoundError(s"No company found with name $name"))
  }

}

object EmployerSqlDao {
  private[sql] final val TableName = "employers"
  private[sql] final val TableFields = Seq("employer_name", "description", "created_at", "created_by",
    "updated_at", "updated_by", "is_active")

  private[sql] final val createQuery = SQL(
    s"""INSERT INTO $TableName $TableFields VALUES {employer_name}, {description},{created_at},{created_by},
       |{updated_at},{updated_by}, {is_active}""".stripMargin)

  private[sql] final val findAllQuery = SQL(s"SELECT * FROM $TableName")

  private[sql] final val findByNameQuery = SQL(s"SELECT * FROM $TableName WHERE employer_name = {employer_name}")

  private def buildParameters(employerToInsert: EmployerToInsert) =
    Seq[NamedParameter](
      "employer_name" -> employerToInsert.name,
      "description" -> employerToInsert.description,
      "created_at" -> employerToInsert.createdAt,
      "created_by" -> employerToInsert.createdBy,
      "updated_at" -> employerToInsert.updatedAt,
      "updated_by" -> employerToInsert.updatedBy,
      "is_active" -> employerToInsert.isActive)

  private def convertRowToEmployer(row: Row) =
    Employer(
      id = row[Int]("id"),
      employerName = row[String]("employer_name"),
      description = row[Option[String]]("description"),
      createdAt = row[LocalDateTime]("created_at"),
      createdBy = row[String]("created_by"),
      updatedAt = row[Option[LocalDateTime]]("updated_at"),
      updatedBy = row[Option[String]]("updated_by"),
      isActive = row[Boolean]("is_active"))

  private def findAllInternal(implicit connection: Connection): Set[Employer] =
    findAllQuery.as(findAllQuery.defaultParser.*).map(convertRowToEmployer).toSet

}
