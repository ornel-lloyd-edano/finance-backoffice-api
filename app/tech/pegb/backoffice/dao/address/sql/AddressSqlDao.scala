package tech.pegb.backoffice.dao.address.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm.{Row, RowParser, SQL, SqlRequestError}
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.{DaoError, SqlDao}
import tech.pegb.backoffice.dao.address.abstraction.AddressDao
import tech.pegb.backoffice.dao.address.dto.{AddressCriteria, AddressToInsert, AddressToUpdate}
import tech.pegb.backoffice.dao.address.entity.Address
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.CountryDao
import tech.pegb.backoffice.dao.businessuserapplication.sql.{BusinessUserApplicationSqlDao, CountrySqlDao}
import tech.pegb.backoffice.dao.contacts.entity.Contact
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class AddressSqlDao @Inject() (val dbApi: DBApi, userDao: UserDao, countryDao: CountryDao) extends AddressDao with SqlDao {
  import SqlDao._

  private val unfilteredSelectQuery =
    s"""
       |SELECT ${AddressSqlDao.TableName}.*,
       |  ${UserSqlDao.TableName}.${UserSqlDao.uuid} as ${Address.cUsrUuid},
       |  ${BusinessUserApplicationSqlDao.TableName}.${BusinessUserApplicationSqlDao.cUuid} as ${Address.cBuApplicUuid},
       |  ${CountrySqlDao.TableName}.${CountrySqlDao.cName} as ${Address.cCountry}
       |FROM ${AddressSqlDao.TableName}
       |
       |LEFT JOIN ${UserSqlDao.TableName}
       |ON  ${AddressSqlDao.TableName}.${Address.cUsrId} = ${UserSqlDao.TableName}.${UserSqlDao.id}
       |
       |LEFT JOIN ${BusinessUserApplicationSqlDao.TableName}
       |ON ${AddressSqlDao.TableName}.${Address.cBuApplicId} = ${BusinessUserApplicationSqlDao.TableName}.${BusinessUserApplicationSqlDao.cId}
       |
       |LEFT JOIN ${CountrySqlDao.TableName}
       |ON ${AddressSqlDao.TableName}.${Address.cCountryId} = ${CountrySqlDao.TableName}.${CountrySqlDao.cId}
     """.stripMargin

  def get(uuid: String)(implicit txnConn: Option[Connection] = None): DaoResponse[Option[Address]] =
    withConnection({ conn ⇒
      implicit val actualConn = txnConn.getOrElse(conn)
      val sql = SQL(
        s"""
           |$unfilteredSelectQuery
           |
           |WHERE ${AddressSqlDao.TableName}.${Address.cUuid} = {${Address.cUuid}}
         """.stripMargin).on(Address.cUuid → uuid)

      sql.executeQuery().as(parseRow.singleOpt)

    }, s"Unexpected error in get($uuid)")

  def getByCriteria(
    criteria: AddressCriteria,
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int])(implicit txnConn: Option[Connection] = None): DaoResponse[Seq[Address]] =
    withConnection({ conn ⇒
      implicit val actualConn = txnConn.getOrElse(conn)

      val filter = generateWhereClause(criteria)
      val ordering = generateOrderByClause(orderBy)
      val pagination = getPagination(limit, offset)
      val rawSql =
        s"""
           |$unfilteredSelectQuery
           |
           |$filter
           |$ordering
           |$pagination
        """.stripMargin
      logger.debug("query = " + rawSql)
      val sql = SQL(rawSql)

      sql.executeQuery().as(parseRow.*)

    }, s"Unexpected error in getByCriteria")

  def insert(dto: AddressToInsert)(implicit txnConn: Option[Connection] = None): DaoResponse[Address] =
    withConnectionAndFlatten({ conn ⇒
      implicit val actualConn = txnConn.getOrElse(conn)
      for {
        userId ← userDao.getInternalUserId(dto.userUuid).fold(
          _.toLeft,
          {
            case Some(id) ⇒ Right(id)
            case None ⇒ Left(DaoError.EntityNotFoundError(s"Failed to insert contact. User uuid [${dto.userUuid}] not found."))
          })
        countryId ← countryDao.getCountries.fold(_.toLeft, _.find(_.name === dto.country) match {
          case Some(country) ⇒ Right(country.id)
          case None ⇒ Left(entityNotFoundError(s"Address cannot be created because country [${dto.country}] was not found"))
        })
        _ ← Try {
          SQL(
            s"""
               |INSERT INTO ${AddressSqlDao.TableName}
               |(${Address.cUuid}, ${Address.cBuApplicId}, ${Address.cUsrId}, ${Address.cAddressType}, ${Address.cCountryId},
               |${Address.cCity}, ${Address.cPostalCode}, ${Address.cAddress}, ${Address.cCoordX}, ${Address.cCoordY}, ${Address.cIsActive},
               |${Address.cCreatedBy}, ${Address.cCreatedAt}, ${Address.cUpdatedBy}, ${Address.cUpdatedAt})
               |VALUES
               |({${Address.cUuid}}, {${Address.cBuApplicId}}, {${Address.cUsrId}}, {${Address.cAddressType}}, {${Address.cCountryId}},
               |{${Address.cCity}}, {${Address.cPostalCode}}, {${Address.cAddress}}, {${Address.cCoordX}}, {${Address.cCoordY}}, {${Address.cIsActive}},
               |{${Address.cCreatedBy}}, {${Address.cCreatedAt}}, {${Address.cUpdatedBy}}, {${Address.cUpdatedAt}})
         """.stripMargin)
            .on(
              Address.cUuid → dto.uuid,
              Address.cBuApplicId → Option.empty[Int],
              Address.cUsrId → userId,
              Address.cAddressType → dto.addressType,
              Address.cCountryId → countryId,
              Address.cCity → dto.city,
              Address.cPostalCode → dto.postalCode,
              Address.cAddress → dto.address,
              Address.cCoordX → dto.coordinateX,
              Address.cCoordY → dto.coordinateY,
              Address.cIsActive → dto.isActive,
              Address.cCreatedBy → dto.createdBy,
              Address.cCreatedAt → dto.createdAt,
              Address.cUpdatedBy → dto.createdBy,
              Address.cUpdatedAt → dto.createdAt).executeInsert()
        }.toEither.fold(error ⇒ {
          logger.error("error encountered in [insert]", error)
          Left(DaoError.GenericDbError("Error encountered while inserting address"))
        }, _ ⇒ Right(()))
        result ← get(dto.uuid)(Some(actualConn)).fold(
          _.toLeft,
          {
            case Some(inserted) ⇒ Right(inserted)
            case None ⇒ Left(DaoError.GenericDbError(s"Address with id [${dto.uuid}] was not inserted"))
          })
      } yield {
        result
      }
    }, s"Unexpected error in insert(${dto.toSmartString})")

  def update(uuid: String, dto: AddressToUpdate)(implicit txnConn: Option[Connection] = None): DaoResponse[Option[Address]] =
    withConnectionAndFlatten({ conn ⇒
      implicit val actualConn = txnConn.getOrElse(conn)

      val paramsBuffer = dto.paramsBuilder
      val filterParam = (Address.cUuid → uuid)
      paramsBuffer += filterParam

      val preQuery = dto.createSqlString(AddressSqlDao.TableName, Some(s"WHERE ${Address.cUuid} = {${Address.cUuid}}"))
      val params = paramsBuffer.result()
      val numRowsUpdated = SQL(preQuery).on(params: _*).executeUpdate()
      if (numRowsUpdated == 1) {
        get(uuid)(Some(actualConn))
      } else {
        Left(preconditionFailed(s"Update failed. Address with id [$uuid] has been modified by another process."))
      }

    }, s"Unexpected error in update($uuid, ${dto.toSmartString})")

  private val parseRow: RowParser[Address] = (row: Row) ⇒ {
    Try {
      Address(
        id = row[Int](Address.cId),
        uuid = row[String](s"${AddressSqlDao.TableName}.${Address.cUuid}"),
        buApplicationId = row[Option[Int]](Address.cBuApplicId),
        buApplicationUuid = row[Option[String]](Address.cBuApplicUuid),
        userId = row[Option[Int]](Address.cUsrId),
        userUuid = row[Option[String]](Address.cUsrUuid),
        addressType = row[String](Address.cAddressType),
        countryId = row[Int](Address.cCountryId),
        countryName = row[String](Address.cCountry),
        city = row[String](Address.cCity),
        postalCode = row[Option[String]](Address.cPostalCode),
        address = row[Option[String]](Address.cAddress),
        coordinateX = row[Option[BigDecimal]](Address.cCoordX),
        coordinateY = row[Option[BigDecimal]](Address.cCoordY),
        createdBy = row[String](Contact.cCreatedBy),
        createdAt = row[LocalDateTime](Contact.cCreatedAt),
        updatedBy = row[Option[String]](Contact.cUpdatedBy),
        updatedAt = row[Option[LocalDateTime]](Contact.cUpdatedAt),
        isActive = row[Int](Contact.cIsActive).toBoolean)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(new Exception(s"Error in parsing row to Address entity. Reason: ${exc.getMessage}"))),
      anorm.Success(_))
  }

  private def generateWhereClause(criteria: AddressCriteria): String = {
    Seq(
      criteria.uuid.map(_.toSql(Some(Contact.cUuid), Some(AddressSqlDao.TableName))),
      criteria.userUuid.map(_.toSql(Some(UserSqlDao.uuid), Some(UserSqlDao.TableName))),
      criteria.buApplicationUuid.map(_.toSql(Some(BusinessUserApplicationSqlDao.cUuid), Some(BusinessUserApplicationSqlDao.TableName))),
      criteria.addressType.map(_.toSql()),
      criteria.countryName.map(_.toSql()),
      criteria.city.map(_.toSql()),
      criteria.postalCode.map(_.toSql()),
      criteria.address.map(_.toSql()),
      criteria.coordinateX.map(_.toSql()),
      criteria.coordinateY.map(_.toSql()),
      criteria.isActive.map(c ⇒ c.copy(value = c.value.toInt).toSql()),
      criteria.createdBy.map(_.toSql()),
      criteria.createdAt.map(_.toSql()),
      criteria.updatedBy.map(_.toSql()),
      criteria.updatedAt.map(_.toSql())).flatten.toSql
  }

  private def generateOrderByClause(maybeOrderBy: Option[OrderingSet]): String = {
    val defaultOrderBy = s"ORDER BY ${Address.cId} ASC"

    maybeOrderBy.map(orderBy ⇒ {
      orderBy.underlying.map({
        case o if o.field === Address.cUuid ⇒ o.copy(field = s"${AddressSqlDao.TableName}.${Address.cUuid}").toString
        case other ⇒ other.toString
      }).toSql
    }).getOrElse(defaultOrderBy)
  }
}

object AddressSqlDao {
  val TableName = "addresses"
  val TableAlias = "adr"
}
