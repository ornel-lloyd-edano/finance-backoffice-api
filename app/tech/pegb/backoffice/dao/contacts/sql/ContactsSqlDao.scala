package tech.pegb.backoffice.dao.contacts.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm._
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.businessuserapplication.sql.BusinessUserApplicationSqlDao
import tech.pegb.backoffice.dao.contacts.abstraction.ContactsDao
import tech.pegb.backoffice.dao.contacts.dto.{ContactToInsert, ContactToUpdate, ContactsCriteria}
import tech.pegb.backoffice.dao.contacts.entity.Contact
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.entity.VelocityPortalUser
import tech.pegb.backoffice.dao.customer.sql.{UserSqlDao, VelocityPortalUserSqlDao}
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.dao.{DaoError, SqlDao}
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class ContactsSqlDao @Inject() (val dbApi: DBApi, userDao: UserDao) extends ContactsDao with SqlDao {
  import SqlDao._

  def get(uuid: String)(implicit txnConn: Option[Connection] = None): DaoResponse[Option[Contact]] =
    withConnection({ conn ⇒
      implicit val actualConn = txnConn.getOrElse(conn)
      val sql = SQL(
        s"""
           |SELECT ${ContactsSqlDao.TableName}.*,
           |  ${UserSqlDao.TableName}.${UserSqlDao.uuid} as ${Contact.cUsrUUID},
           |  ${BusinessUserApplicationSqlDao.TableName}.${BusinessUserApplicationSqlDao.cUuid} as ${Contact.cBuApplicUUID},
           |  ${VelocityPortalUserSqlDao.TableName}.${VelocityPortalUser.cUuid} as ${Contact.cVpUserUUID}
           |FROM ${ContactsSqlDao.TableName}
           |
           |LEFT JOIN ${UserSqlDao.TableName}
           |ON  ${ContactsSqlDao.TableName}.${Contact.cUsrId} = ${UserSqlDao.TableName}.${UserSqlDao.id}
           |
           |LEFT JOIN ${BusinessUserApplicationSqlDao.TableName}
           |ON ${ContactsSqlDao.TableName}.${Contact.cBuApplicId} = ${BusinessUserApplicationSqlDao.TableName}.${BusinessUserApplicationSqlDao.cId}
           |
           |LEFT JOIN ${VelocityPortalUserSqlDao.TableName}
           |ON ${ContactsSqlDao.TableName}.${Contact.cVpUserId} = ${VelocityPortalUserSqlDao.TableName}.${VelocityPortalUser.cId}
           |
           |WHERE ${ContactsSqlDao.TableName}.${Contact.cUuid} = {${Contact.cUuid}}
         """.stripMargin).on(Contact.cUuid → uuid)

      sql.executeQuery().as(parseRow.singleOpt)

    }, s"Unexpected error in get($uuid)")

  def getByCriteria(
    criteria: ContactsCriteria,
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int])(implicit txnConn: Option[Connection] = None): DaoResponse[Seq[Contact]] =
    withConnection({ conn ⇒
      implicit val actualConn = txnConn.getOrElse(conn)

      val filter = generateWhereClause(criteria)
      val ordering = generateOrderByClause(orderBy)
      val pagination = getPagination(limit, offset)
      val rawSql =
        s"""
          |SELECT ${ContactsSqlDao.TableName}.*,
          |  ${UserSqlDao.TableName}.${UserSqlDao.uuid} as ${Contact.cUsrUUID},
          |  ${BusinessUserApplicationSqlDao.TableName}.${BusinessUserApplicationSqlDao.cUuid} as ${Contact.cBuApplicUUID},
          |  ${VelocityPortalUserSqlDao.TableName}.${VelocityPortalUser.cUuid} as ${Contact.cVpUserUUID}
          |FROM ${ContactsSqlDao.TableName}
          |
          |LEFT JOIN ${UserSqlDao.TableName}
          |ON  ${ContactsSqlDao.TableName}.${Contact.cUsrId} = ${UserSqlDao.TableName}.${UserSqlDao.id}
          |
          |LEFT JOIN ${BusinessUserApplicationSqlDao.TableName}
          |ON ${ContactsSqlDao.TableName}.${Contact.cBuApplicId} = ${BusinessUserApplicationSqlDao.TableName}.${BusinessUserApplicationSqlDao.cId}
          |
          |LEFT JOIN ${VelocityPortalUserSqlDao.TableName}
          |ON ${ContactsSqlDao.TableName}.${Contact.cVpUserId} = ${VelocityPortalUserSqlDao.TableName}.${VelocityPortalUser.cId}
          |
          |$filter
          |$ordering
          |$pagination
        """.stripMargin
      logger.debug("query = " + rawSql)
      val sql = SQL(rawSql)

      sql.executeQuery().as(parseRow.*)

    }, s"Unexpected error in getByCriteria")

  def insert(dto: ContactToInsert)(implicit txnConn: Option[Connection] = None) =
    withConnectionAndFlatten({ conn ⇒
      implicit val actualConn = txnConn.getOrElse(conn)
      /*
      regex that I like:
      find: (\$\{[a-zA-Z\.]*\})
      replace: {$1}
       */
      for {
        userId ← userDao.getInternalUserId(dto.userUuid).fold(
          _.toLeft,
          {
            case Some(id) ⇒ Right(id)
            case None ⇒ Left(DaoError.EntityNotFoundError(s"Failed to insert contact. User uuid [${dto.userUuid}] not found."))
          })
        z ← Try {
          SQL(
            s"""
               |INSERT INTO ${ContactsSqlDao.TableName}
               |(${Contact.cUuid}, ${Contact.cBuApplicId}, ${Contact.cUsrId}, ${Contact.cContactType}, ${Contact.cName},
               |${Contact.cMidName}, ${Contact.cSurName}, ${Contact.cPhoneNum}, ${Contact.cEmail}, ${Contact.cIdType}, ${Contact.cIsActive},
               |${Contact.cCreatedBy}, ${Contact.cCreatedAt}, ${Contact.cUpdatedBy}, ${Contact.cUpdatedAt})
               |VALUES
               |({${Contact.cUuid}}, {${Contact.cBuApplicId}}, {${Contact.cUsrId}}, {${Contact.cContactType}}, {${Contact.cName}},
               |{${Contact.cMidName}}, {${Contact.cSurName}}, {${Contact.cPhoneNum}}, {${Contact.cEmail}}, {${Contact.cIdType}}, {${Contact.cIsActive}},
               |{${Contact.cCreatedBy}}, {${Contact.cCreatedAt}}, {${Contact.cUpdatedBy}}, {${Contact.cUpdatedAt}})
         """.stripMargin)
            .on(
              Contact.cUuid → dto.uuid,
              Contact.cBuApplicId → Option.empty[Int],
              Contact.cUsrId → userId,
              Contact.cContactType → dto.contactType,
              Contact.cName → dto.name,
              Contact.cMidName → dto.middleName,
              Contact.cSurName → dto.surname,
              Contact.cPhoneNum → dto.phoneNumber,
              Contact.cEmail → dto.email,
              Contact.cIdType → dto.idType,
              Contact.cIsActive → dto.isActive,
              Contact.cCreatedBy → dto.createdBy,
              Contact.cCreatedAt → dto.createdAt,
              Contact.cUpdatedBy → dto.createdBy,
              Contact.cUpdatedAt → dto.createdAt).executeInsert()
        }.toEither.fold(error ⇒ {
          logger.error("error encountered in [insert]", error)
          Left(DaoError.GenericDbError(s"Error encountered while inserting contact $dto"))
        }, _ ⇒ Right(()))
        result ← get(dto.uuid)(Some(actualConn)).fold(
          _.toLeft,
          {
            case Some(inserted) ⇒ Right(inserted)
            case None ⇒ Left(DaoError.GenericDbError(s"Contact with id [${dto.uuid}] was not inserted"))
          })
      } yield {
        result
      }
    }, s"Unexpected error in insert(${dto.toSmartString})")

  def update(uuid: String, dto: ContactToUpdate)(implicit txnConn: Option[Connection] = None) =
    withConnectionAndFlatten({ conn ⇒
      implicit val actualConn = txnConn.getOrElse(conn)

      val paramsBuffer = dto.paramsBuilder
      val filterParam = (Contact.cUuid → uuid)
      paramsBuffer += filterParam

      val preQuery = dto.createSqlString(ContactsSqlDao.TableName, Some(s"WHERE ${Contact.cUuid} = {${Contact.cUuid}}"))
      val params = paramsBuffer.result()
      val numRowsUpdated = SQL(preQuery).on(params: _*).executeUpdate()
      if (numRowsUpdated == 1) {
        get(uuid)(Some(actualConn))
      } else {
        Left(preconditionFailed(s"Update failed. Contact with id [$uuid] has been modified by another process."))
      }

    }, s"Unexpected error in update($uuid, ${dto.toSmartString})")

  private val parseRow: RowParser[Contact] = (row: Row) ⇒ {
    Try {
      Contact(
        id = row[Int](Contact.cId),
        uuid = row[String](s"${ContactsSqlDao.TableName}.${Contact.cUuid}"),
        buApplicationId = row[Option[Int]](Contact.cBuApplicId),
        buApplicationUUID = row[Option[String]](Contact.cBuApplicUUID),
        userId = row[Option[Int]](Contact.cUsrId),
        userUUID = row[Option[String]](Contact.cUsrUUID),
        contactType = row[String](Contact.cContactType),
        name = row[String](Contact.cName),
        middleName = row[Option[String]](Contact.cMidName),
        surname = row[String](Contact.cSurName),
        phoneNumber = row[String](Contact.cPhoneNum),
        email = row[String](Contact.cEmail),
        idType = row[String](Contact.cIdType),
        createdBy = row[String](Contact.cCreatedBy),
        createdAt = row[LocalDateTime](Contact.cCreatedAt),
        updatedBy = row[Option[String]](Contact.cUpdatedBy),
        updatedAt = row[Option[LocalDateTime]](Contact.cUpdatedAt),
        vpUserId = row[Option[Int]](Contact.cVpUserId),
        vpUserUUID = row[Option[String]](Contact.cVpUserUUID),
        isActive = row[Int](Contact.cIsActive).toBoolean)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(new Exception(s"Error in parsing row to Contact entity. Reason: ${exc.getMessage}"))),
      anorm.Success(_))
  }

  private def generateWhereClause(criteria: ContactsCriteria): String = {
    Seq(
      criteria.uuid.map(_.toSql(Some(Contact.cUuid), Some(ContactsSqlDao.TableName))),
      criteria.userUuid.map(_.toSql(Some(UserSqlDao.uuid), Some(UserSqlDao.TableName))),
      criteria.buApplicUuid.map(_.toSql(Some(BusinessUserApplicationSqlDao.cUuid), Some(BusinessUserApplicationSqlDao.TableName))),
      criteria.contactType.map(_.toSql()),
      criteria.name.map(_.toSql()),
      criteria.middleName.map(_.toSql()),
      criteria.surname.map(_.toSql()),
      criteria.phoneNumber.map(_.toSql()),
      criteria.email.map(_.toSql()),
      criteria.idType.map(_.toSql()),
      criteria.isActive.map(c ⇒ c.copy(value = c.value.toInt).toSql()),
      criteria.vpUserId.map(_.toSql()),
      criteria.vpUserUUID.map(_.toSql(Some(VelocityPortalUser.cUuid), Some(VelocityPortalUserSqlDao.TableName))),
      criteria.createdBy.map(_.toSql()),
      criteria.createdAt.map(_.toSql()),
      criteria.updatedBy.map(_.toSql()),
      criteria.updatedAt.map(_.toSql())).flatten.toSql
  }

  private def generateOrderByClause(maybeOrderBy: Option[OrderingSet]): String = {
    val defaultOrderBy = s" ORDER BY ${Contact.cId} ASC"

    maybeOrderBy.map(orderBy ⇒ {
      orderBy.underlying.map({
        case o if o.field === Contact.cUuid ⇒ o.copy(field = s"${ContactsSqlDao.TableName}.${Contact.cUuid}").toString
        case other ⇒ other.toString
      }).toSql
    }).getOrElse(defaultOrderBy)
  }

}

object ContactsSqlDao {
  val TableName = "contact_persons"
  val TableAlias = "con"
}
