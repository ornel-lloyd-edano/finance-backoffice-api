package tech.pegb.backoffice.dao.customer.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm._
import cats.syntax.either._
import org.apache.commons.text.StringEscapeUtils
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.customer.abstraction.NationalityDao
import tech.pegb.backoffice.dao.customer.dto.NationalityToInsert
import tech.pegb.backoffice.dao.customer.entity.Nationality

import scala.util.Try

class NationalitySqlDao(protected val dbApi: DBApi, kafkaDBSyncService: KafkaDBSyncService) extends NationalityDao with SqlDao {

  import NationalitySqlDao._

  override def create(nationalityToInsert: NationalityToInsert): DaoResponse[Nationality] =
    withConnectionAndFlatten({ implicit connection ⇒
      val parameters = buildParameters(nationalityToInsert)

      for {
        _ ← Try(createQuery.on(parameters: _*).execute())
          .toEither.leftMap({
            case e: SQLException if isUniqueConstraintViolation(e) ⇒
              val msg = s"nationality name is not unique : ${nationalityToInsert.name}"
              logger.error(StringEscapeUtils.escapeJava(msg), e)
              entityAlreadyExistsError(msg)
            case e @ (_: SQLException | _: AnormException) ⇒
              val msg = s"Unexpected SQL exception in ${this.getClass}.create"
              logger.error(StringEscapeUtils.escapeJava(msg), e)
              genericDbError(msg)
          })

        created ← findByNameInternal(nationalityToInsert.name)
      } yield {
        kafkaDBSyncService.sendInsert(TableName, created)
        created
      }

    }, s"error while creating nationality ${nationalityToInsert.name}")

  def getAll: DaoResponse[Set[Nationality]] =
    withConnection(implicit connection ⇒ findAllInternal, "Error while retrieving all nationalities")

  private def findByNameInternal(name: String)(implicit connection: Connection) = {
    findByNameQuery
      .on('nationality_name -> name)
      .as(findByNameQuery.defaultParser.*)
      .headOption
      .map(convertRowToEntity)
      .toRight(entityNotFoundError(s"No nationality found with name $name"))
  }
}

object NationalitySqlDao {
  private[sql] final val TableName = "nationalities"
  private[sql] final val TableFields =
    Seq("nationality_name", "description", "created_at", "created_by", "updated_at", "updated_by", "is_active")

  private[sql] final val createQuery =
    SQL(
      s"""INSERT INTO $TableName VALUES {nationality_name},{description},{created_at},{created_by},
         |{updated_at},{updated_by},{is_active}""".stripMargin)

  private[sql] final val findAllQuery = SQL(s"SELECT * FROM $TableName")
  private[sql] final val findByNameQuery = SQL(s"SELECT * FROM $TableName WHERE nationality_name={nationality_name}")

  private def buildParameters(nationalityToInsert: NationalityToInsert) =
    Seq[NamedParameter](
      "nationality_name" -> nationalityToInsert.name,
      "description" -> nationalityToInsert.description,
      "created_at" -> nationalityToInsert.createdAt,
      "created_by" -> nationalityToInsert.createdBy,
      "updated_at" -> nationalityToInsert.updatedAt,
      "updated_by" -> nationalityToInsert.updatedBy,
      "is_active" -> nationalityToInsert.isActive)

  private def convertRowToEntity(row: Row) =
    Nationality(
      id = row[Int]("id"),
      name = row[String]("nationality_name"),
      description = row[Option[String]]("description"),
      createdAt = row[LocalDateTime]("created_at"),
      createdBy = row[String]("created_by"),
      updatedAt = row[Option[LocalDateTime]]("updated_at"),
      updatedBy = row[Option[String]]("updated_by"),
      isActive = row[Boolean]("is_active"))

  private def findAllInternal(implicit connection: Connection): Set[Nationality] =
    findAllQuery.as(findAllQuery.defaultParser.*).map(convertRowToEntity).toSet
}
