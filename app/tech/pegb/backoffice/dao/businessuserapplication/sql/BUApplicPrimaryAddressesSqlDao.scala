package tech.pegb.backoffice.dao.businessuserapplication.sql

import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID

import anorm.{BatchSql, NamedParameter, Row, SQL}
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.BUAppPrimaryAddressesDao
import tech.pegb.backoffice.dao.businessuserapplication.dto.BUApplicPrimaryAddressToInsert
import tech.pegb.backoffice.dao.businessuserapplication.entity.BUApplicPrimaryAddress

import scala.collection.immutable
import scala.util.Try

@Singleton
class BUApplicPrimaryAddressesSqlDao @Inject() (val dbApi: DBApi) extends BUAppPrimaryAddressesDao with SqlDao {

  import BUApplicPrimaryAddressesSqlDao._

  private val selectSql =
    s"""
       |SELECT * FROM ${TableName} WHERE  ${cApplicId} IN ({${cApplicId}})
     """.stripMargin

  private def parseRow(row: Row) = Try {
    BUApplicPrimaryAddress(
      id = row[Int](cId),
      uuid = row[String](cUuid),
      applicationId = row[Int](cApplicId),
      addressType = row[String](cAddressType),
      countryId = row[Int](cCountryId),
      city = row[String](cCity),
      postalCode = row[Option[String]](cPostalCode),
      address = row[Option[String]](cAddress),
      coordinateX = row[Option[Double]](cCoordinateX),
      coordinateY = row[Option[Double]](cCoordinateY),
      createdBy = row[String](cCreatedBy),
      createdAt = row[LocalDateTime](cCreatedAt),
      updatedBy = row[Option[String]](cUpdatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt))
  }

  private val insertSql =
    s"""
       |INSERT INTO ${TableName}
       |(${cUuid},   ${cApplicId},   ${cAddressType},   ${cCountryId},   ${cCity},   ${cPostalCode},   ${cAddress},
       |${cCoordinateX},   ${cCoordinateY}, ${cCreatedBy},   ${cCreatedAt},   ${cUpdatedBy},   ${cUpdatedAt})
       |VALUES
       |({${cUuid}}, {${cApplicId}}, {${cAddressType}}, {${cCountryId}}, {${cCity}}, {${cPostalCode}}, {${cAddress}},
       | {${cCoordinateX}}, {${cCoordinateY}}, {${cCreatedBy}}, {${cCreatedAt}}, {${cUpdatedBy}}, {${cUpdatedAt}})
     """.stripMargin

  private def getNamedParameters(dto: BUApplicPrimaryAddressToInsert) = {
    Seq[NamedParameter](
      cUuid → UUID.randomUUID().toString,
      cApplicId → dto.applicationId,
      cAddressType → dto.addressType,
      cCountryId → dto.countryId,
      cCity → dto.city,
      cPostalCode → dto.postalCode,
      cAddress → dto.address,
      cCoordinateX → dto.coordinateX,
      cCoordinateY → dto.coordinateY,
      cCreatedBy → dto.createdBy,
      cCreatedAt → dto.createdAt,
      cUpdatedBy → dto.createdBy,
      cUpdatedAt → dto.createdAt)
  }

  private def getByApplicationIdInternal(applicationId: Seq[Int])(implicit conn: Connection): immutable.Seq[BUApplicPrimaryAddress] = {
    val sql = SQL(selectSql).on(cApplicId → applicationId)
    sql.as(sql.defaultParser.*)(conn).map(parseRow(_).get)
  }

  def getByApplicationId(applicationId: Int): DaoResponse[Seq[BUApplicPrimaryAddress]] = withConnection({ implicit conn ⇒
    getByApplicationIdInternal(Seq(applicationId))
  }, s"Unexpected error in get business user application address by applicationId [$applicationId]")

  def insert(dto: Seq[BUApplicPrimaryAddressToInsert])(implicit txnConn: Option[Connection]): DaoResponse[Seq[BUApplicPrimaryAddress]] =
    withConnection({ conn ⇒

      if (dto.nonEmpty) {
        BatchSql(insertSql, getNamedParameters(dto.head), dto.tail.map(getNamedParameters(_)): _*).execute()(txnConn.getOrElse(conn))
        getByApplicationIdInternal(dto.map(_.applicationId))(txnConn.getOrElse(conn))
      } else {
        Nil
      }

    }, s"Unexpected error in insert business user application address")

  def deleteByApplicationId(applicationId: Int)(implicit txnConn: Option[Connection]): DaoResponse[Unit] =
    withConnection({ conn ⇒

      SQL(s"DELETE FROM $TableName WHERE $cApplicId = $applicationId").executeUpdate()(txnConn.getOrElse(conn))

    }, s"Unexpected error in delete business user application address")

}

object BUApplicPrimaryAddressesSqlDao {

  val TableName = "business_user_application_primary_addresses"
  val TableAlias = "buapa"

  val cId = "id"
  val cUuid = "uuid"
  val cApplicId = "application_id"
  val cAddressType = "address_type"
  val cCountryId = "country_id"
  val cCity = "city"
  val cPostalCode = "postal_code"
  val cAddress = "address"
  val cCoordinateX = "coordinate_x"
  val cCoordinateY = "coordinate_y"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cUpdatedBy = "updated_by"
  val cUpdatedAt = "updated_at"
}

