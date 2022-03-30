package tech.pegb.backoffice.dao.businessuserapplication.sql

import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID

import anorm._
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.BUApplicPrimaryContactsDao
import tech.pegb.backoffice.dao.businessuserapplication.dto.BUApplicPrimaryContactToInsert
import tech.pegb.backoffice.dao.businessuserapplication.entity.BUApplicPrimaryContact
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class BUApplicPrimaryContactsSqlDao @Inject() (val dbApi: DBApi) extends BUApplicPrimaryContactsDao with SqlDao {
  import BUApplicPrimaryContactsSqlDao._

  private val selectSql =
    s"""
       |SELECT * FROM ${TableName} WHERE  ${cApplicId} IN ({${cApplicId}})
     """.stripMargin

  def getByApplicationId(applicationId: Int): DaoResponse[Seq[BUApplicPrimaryContact]] =
    withConnection({ implicit conn ⇒
      getByApplicationIdInternal(Seq(applicationId))
    }, s"Unexpected error in get business user application contact by applicationId [$applicationId]")

  private def getByApplicationIdInternal(applicationIds: Seq[Int])(implicit conn: Connection) = {
    val sql = SQL(selectSql).on(cApplicId → applicationIds)
    sql.as(sql.defaultParser.*)(conn).map(parseRow(_).get)
  }

  private def parseRow(row: Row) = Try {
    BUApplicPrimaryContact(
      id = row[Int](cId),
      uuid = row[String](cUuid),
      applicationId = row[Int](cApplicId),
      contactType = row[String](cContactType),
      name = row[String](cName),
      middleName = row[Option[String]](cMidName),
      surname = row[String](cSurname),
      phoneNumber = row[String](cPhone),
      email = row[String](cEmail),
      idType = row[String](cIdType),
      isVelocityUser = row[Int](cIsVelocityUsr).toBoolean,
      velocityLevel = row[Option[String]](cVelocityLevel),
      isDefaultContact = row[Option[Int]](cIsDefault).map(_.toBoolean),
      createdBy = row[String](cCreatedBy),
      createdAt = row[LocalDateTime](cCreatedAt),
      updatedBy = row[Option[String]](cUpdatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt))
  }

  private val insertSql =
    s"""
       |INSERT INTO ${TableName}
       |(${cUuid},   ${cApplicId},   ${cContactType},   ${cName},   ${cMidName},   ${cSurname},   ${cPhone},
       |${cEmail},   ${cIdType},   ${cIsVelocityUsr},   ${cVelocityLevel},  ${cIsDefault},
       |${cCreatedBy},   ${cCreatedAt},   ${cUpdatedBy},   ${cUpdatedAt})
       |VALUES
       |({${cUuid}}, {${cApplicId}}, {${cContactType}}, {${cName}}, {${cMidName}}, {${cSurname}}, {${cPhone}},
       | {${cEmail}}, {${cIdType}}, {${cIsVelocityUsr}}, {${cVelocityLevel}},  {${cIsDefault}},
       | {${cCreatedBy}}, {${cCreatedAt}}, {${cUpdatedBy}}, {${cUpdatedAt}})
     """.stripMargin

  private def getNamedParameters(dto: BUApplicPrimaryContactToInsert) = {
    Seq[NamedParameter](
      cUuid → UUID.randomUUID().toString,
      cApplicId → dto.applicationId,
      cContactType → dto.contactType,
      cName → dto.name,
      cMidName → dto.middleName,
      cSurname → dto.surname,
      cPhone → dto.phoneNumber,
      cEmail → dto.email,
      cIdType → dto.idType,
      cIsVelocityUsr → dto.isVelocityUser.toInt,
      cVelocityLevel → dto.velocityLevel,
      cIsDefault → dto.isDefaultContact.map(_.toInt).getOrElse(0),
      cCreatedBy → dto.createdBy,
      cCreatedAt → dto.createdAt,
      cUpdatedBy → dto.createdBy,
      cUpdatedAt → dto.createdAt)
  }

  def insert(dto: Seq[BUApplicPrimaryContactToInsert])(implicit txnConn: Option[Connection] = None) =
    withConnection({ conn ⇒

      if (dto.nonEmpty) {
        val ids = BatchSql(insertSql, getNamedParameters(dto.head), dto.tail.map(getNamedParameters(_)): _*).execute()(txnConn.getOrElse(conn))
        getByApplicationIdInternal(dto.map(_.applicationId))(txnConn.getOrElse(conn))
      } else {
        Nil
      }

    }, s"Unexpected error in insert business user application contacts")

  def deleteByApplicationId(applicationId: Int)(implicit txnConn: Option[Connection] = None) =
    withConnection({ conn ⇒

      SQL(s"DELETE FROM ${TableName} WHERE ${cApplicId} = $applicationId").executeUpdate()(txnConn.getOrElse(conn))

    }, s"Unexpected error in delete business user application contacts by application id [${applicationId}]")
}

object BUApplicPrimaryContactsSqlDao {
  val TableName = "business_user_application_primary_contacts"
  val TableAlias = "buapc"

  val cId = "id"
  val cUuid = "uuid"
  val cApplicId = "application_id"
  val cContactType = "contact_type"
  val cName = "name"
  val cMidName = "middle_name"
  val cSurname = "surname"
  val cPhone = "phone_number"
  val cEmail = "email"
  val cIdType = "id_type"
  val cIsVelocityUsr = "is_velocity_user"
  val cVelocityLevel = "velocity_level"
  val cIsDefault = "is_default_contact"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cUpdatedBy = "updated_by"
  val cUpdatedAt = "updated_at"
}
