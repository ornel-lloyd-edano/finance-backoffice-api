package tech.pegb.backoffice.dao.customer.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm._
import cats.syntax.either._
import com.google.inject.Inject
import org.apache.commons.text.StringEscapeUtils
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.customer.abstraction.CompanyDao
import tech.pegb.backoffice.dao.customer.dto.CompanyToInsert
import tech.pegb.backoffice.dao.customer.entity.Company

import scala.util.Try

class CompanySqlDao @Inject() (protected val dbApi: DBApi, kafkaDBSyncService: KafkaDBSyncService) extends CompanyDao with SqlDao {

  import CompanySqlDao._

  def create(company: CompanyToInsert): DaoResponse[Company] = withTransactionAndFlatten({ implicit connection ⇒

    val parameters = buildParameters(company)
    for {
      _ ← Try(insertCompanySql.on(parameters: _*).execute())
        .toEither.leftMap({
          case e: SQLException if isUniqueConstraintViolation(e) ⇒
            val msg = s"company name is not unique : ${company.name}"
            logger.error(StringEscapeUtils.escapeJava(msg), e)
            entityAlreadyExistsError(msg)
          case e @ (_: SQLException | _: AnormException) ⇒
            val msg = s"Unexpected SQL exception in ${this.getClass}.create"
            logger.error(StringEscapeUtils.escapeJava(msg), e)
            genericDbError(msg)
        })
      created ← findByNameInternal(company.name)
    } yield {
      kafkaDBSyncService.sendInsert(TableName, created)
      created
    }

  }, s"Error while creating company ${company.name}")

  def getAll: DaoResponse[Set[Company]] =
    withConnection(implicit connection ⇒ findAllInternal, "Error while retrieving all company")

  private def findByNameInternal(name: String)(implicit connection: Connection): DaoResponse[Company] = {
    findByNameSql
      .on('company_name -> name)
      .as(findByNameSql.defaultParser.*)
      .headOption
      .map(row ⇒ convertRowToCompany(row))
      .toRight(entityNotFoundError(s"No company found with name $name"))
  }

  private def findAllInternal(implicit connection: Connection) =
    findAllSql.as(findAllSql.defaultParser.*).map(row ⇒ convertRowToCompany(row)).toSet

}

object CompanySqlDao {
  private[dao] final val TableName = "companies"

  //id is auto incremented, therefore not added
  private[dao] final val TableFields = Seq("company_name", "company_full_name", "created_at", "updated_at",
    "created_by", "updated_by", "is_active")

  private[dao] final val insertCompanySql =
    SQL(
      s"""INSERT INTO $TableName $TableFields VALUES ({company_name},
                                      {company_full_name}, {created_at}, {updated_at}, {created_by}, {updated_by}, {is_active})""")

  private[dao] final val findAllSql = SQL(s"SELECT * FROM $TableName")

  private[dao] final val findByNameSql = SQL(s"SELECT * FROM $TableName WHERE company_name={company_name}")

  private def buildParameters(company: CompanyToInsert) =
    Seq[NamedParameter](
      "company_name" -> company.name,
      "company_full_name" -> company.fullName,
      "created_at" -> company.createdAt,
      "created_by" -> company.createdBy,
      "updated_at" -> company.updatedAt,
      "updated_by" -> company.updatedBy,
      "is_active" -> company.isActive)

  private def convertRowToCompany(row: Row) =
    Company(
      id = row[Int]("id"),
      companyName = row[String]("company_name"),
      companyFullName = row[Option[String]]("company_full_name"),
      createdAt = row[LocalDateTime]("created_at"),
      createdBy = row[String]("created_by"),
      updatedAt = row[Option[LocalDateTime]]("updated_at"),
      updatedBy = row[Option[String]]("updated_by"),
      isActive = row[Boolean]("is_active"))

}
