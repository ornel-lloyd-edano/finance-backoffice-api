package tech.pegb.backoffice.dao.customer.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm.{AnormException, NamedParameter, Row, _}
import cats.syntax.either._
import org.apache.commons.text.StringEscapeUtils
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.customer.abstraction.OccupationDao
import tech.pegb.backoffice.dao.customer.dto.OccupationToInsert
import tech.pegb.backoffice.dao.customer.entity.Occupation

import scala.util.Try

class OccupationSqlDao(protected val dbApi: DBApi, kafkaDBSyncService: KafkaDBSyncService) extends OccupationDao with SqlDao {

  import OccupationSqlDao._

  def create(occupationToInsert: OccupationToInsert): DaoResponse[Occupation] =
    withConnectionAndFlatten({ implicit connection ⇒
      val parameters = buildParameters(occupationToInsert)

      for {
        _ ← Try(createQuery.on(parameters: _*).execute())
          .toEither.leftMap({
            case e: SQLException if isUniqueConstraintViolation(e) ⇒
              val msg = s"occupation name is not unique : ${occupationToInsert.name}"
              logger.error(StringEscapeUtils.escapeJava(msg), e)
              entityAlreadyExistsError(msg)
            case e @ (_: SQLException | _: AnormException) ⇒
              val msg = s"Unexpected SQL exception in ${this.getClass}.create"
              logger.error(StringEscapeUtils.escapeJava(msg), e)
              genericDbError(msg)
          })

        created ← findByNameInternal(occupationToInsert.name)
      } yield {
        kafkaDBSyncService.sendInsert(TableName, created)
        created
      }

    }, s"error while creating occupation ${occupationToInsert.name}")

  def getAll: DaoResponse[Set[Occupation]] =
    withConnection(implicit connection ⇒ findAllInternal, "Error while retrieving all occupations")

  private def findByNameInternal(name: String)(implicit connection: Connection) = {
    findByNameQuery
      .on('occupation_name -> name)
      .as(findByNameQuery.defaultParser.*)
      .headOption
      .map(convertRowToEntity)
      .toRight(entityNotFoundError(s"No occupation found with name $name"))
  }

  private def findAllInternal(implicit connection: Connection): Set[Occupation] =
    findAllQuery.as(findAllQuery.defaultParser.*).map(convertRowToEntity).toSet
}

object OccupationSqlDao {
  private[sql] final val TableName = "occupations"
  private[sql] final val TableFields =
    Seq("occupation_name", "description", "created_at", "created_by", "updated_at", "updated_by", "is_active")
  private[sql] final val createQuery =
    SQL(
      s"""INSERT INTO $TableName $TableFields VALUES {occupation_name},
         |{description},{created_at},{created_by},{updated_at},{updated_by},{is_active}""".stripMargin)
  private[sql] final val findAllQuery = SQL(s"SELECT * FROM $TableName")
  private[sql] final val findByNameQuery = SQL(s"SELECT * FROM $TableName WHERE occupation_name={occupation_name}")

  private def buildParameters(occupationToInsert: OccupationToInsert) =
    Seq[NamedParameter](
      "occupation_name" -> occupationToInsert.name,
      "description" -> occupationToInsert.description,
      "created_at" -> occupationToInsert.createdAt,
      "created_by" -> occupationToInsert.createdBy,
      "updated_at" -> occupationToInsert.updatedAt,
      "updated_by" -> occupationToInsert.updatedBy,
      "is_active" -> occupationToInsert.isActive)

  private def convertRowToEntity(row: Row) =
    Occupation(
      id = row[Int]("id"),
      name = row[String]("occupation_name"),
      description = row[Option[String]]("description"),
      createdAt = row[LocalDateTime]("created_at"),
      createdBy = row[String]("created_by"),
      updatedAt = row[Option[LocalDateTime]]("updated_at"),
      updatedBy = row[Option[String]]("updated_by"),
      isActive = row[Boolean]("is_active"))

}
