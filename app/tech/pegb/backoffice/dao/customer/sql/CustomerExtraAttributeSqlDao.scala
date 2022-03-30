package tech.pegb.backoffice.dao.customer.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm.{Row, SQL}
import com.google.inject.Inject
import play.api.db.DBApi

import tech.pegb.backoffice.dao.{DaoError, SqlDao}
import tech.pegb.backoffice.dao.customer.abstraction.{CustomerExtraAttributeDao, UserDao}
import tech.pegb.backoffice.dao.customer.entity.ExtraAttributeRequirements._

import scala.util.Try

class CustomerExtraAttributeSqlDao @Inject() (
    val dbApi: DBApi,
    userDao: UserDao) extends CustomerExtraAttributeDao with SqlDao {

  import CustomerExtraAttributeSqlDao._

  def addExtraAttributeType(newAttributeType: ExtraAttributeTypeToCreate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[UserExtraAttribute] = ???

  def getExtraAttributeTypes: DaoResponse[Set[ExtraAttributeType]] = ???

  def getBusinessUserExtraAttributes(userId: String): DaoResponse[Set[UserExtraAttribute]] = ???

  def getBusinessUserExtraAttributesByAttribute(userId: String, attributeName: String): DaoResponse[Seq[UserExtraAttribute]] =
    withConnectionAndFlatten(
      { implicit connection: Connection ⇒
        for {
          id ← userDao.getInternalUserId(userId).fold(
            error ⇒ Left(error),
            {
              case Some(userInternalId) ⇒ Right(userInternalId)
              case _ ⇒ Left(DaoError.EntityNotFoundError(s"Could get business user extra attributes. User id [${userId}] not found"))
            })
        } yield {
          findBusinessUserExtraAttrByUserIdAndAttrName(id, attributeName).map(_.copy(userUuid = userId))
        }
      }, s"Error while retrieving business user extra attribute by userId:$userId and attribute id:$attributeName")

  def addBusinessUserExtraAttribute(userId: String, attributeName: String, value: String, createdBy: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Unit] = ???

  def addBusinessUserExtraAttributes(attributesToAdd: Seq[UserExtraAttributeToAdd])(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Unit] = ???

  def updateBusinessUserExtraAttribute(userId: String, attributeName: String, newValue: String, updatedBy: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[UserExtraAttribute]] = ???

  def enableBusinessUserExtraAttribute(userId: String, attributeName: String, enabledBy: String): DaoResponse[Option[UserExtraAttribute]] = ???

  def disableBusinessUserExtraAttribute(userId: String, attributeName: String, disabledBy: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[UserExtraAttribute]] = ???

  def deleteBusinessUserExtraAttribute(userId: String, attributeName: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Unit] = ???

  def getExtraAttributesRequiredByCustomerStatus(statusName: String): DaoResponse[Set[ExtraAttributeType]] =
    withConnection({ implicit connection: Connection ⇒

      findExtraAttributeTypesWithStatus(statusName)
    }, "Error while retrieving extra attribute type with user status id")

  def getExtraAttributesRequiredByTier(tierName: String): DaoResponse[Set[ExtraAttributeType]] = ???

  def getExtraAttributesRequiredBySubscription(subscriptionName: String): DaoResponse[Set[ExtraAttributeType]] = ???

  def getExtraAttributesRequiredByBusinessUserType(buTypeName: String): DaoResponse[Set[ExtraAttributeType]] = ???

  def getExtraAttributesRequiredBySegmentType(segmentTypeName: String): DaoResponse[Set[ExtraAttributeType]] = ???
}

object CustomerExtraAttributeSqlDao {
  private[sql] final val UserStatusTable = "user_status"
  private[sql] final val ExtraAttributeTypesTable = "extra_attribute_types"
  private[sql] final val BusinessUserExtraAttrTable = "business_users_has_extra_attributes"
  private[sql] final val UserStatusRequirementsTable = "user_status_has_requirements"
  private[sql] final val UserStatusTableAlias = "us"
  private[sql] final val ExtraAttributeTypesAlias = "eat"
  private[sql] final val BusinessUserExtraAttrAlias = "bushea"
  private[sql] final val ExtraAttributeTypesWithStatusAlias = "eaws"

  //TODO optimize this query, use inner join
  private[sql] final val FindBusinessUserExtraAttributeByUserIdAndAttrName =
    SQL(
      s"""
         |SELECT * FROM $BusinessUserExtraAttrTable
         |WHERE extra_attribute_type={extra_attribute_type_name} AND
         |business_user_id = {business_user_id};
       """.stripMargin)

  private[sql] final val FindAttributesRequiredByStatusName = SQL(
    s"""
       |SELECT * FROM $UserStatusRequirementsTable a
       |JOIN $ExtraAttributeTypesTable b ON a.requirement_type = b.attribute_type_name
       |WHERE a.user_status = {status_name};
     """.stripMargin)

  private def convertRowToUserExtraAttribute(row: Row) = Try {
    UserExtraAttribute(
      userId = row[Int]("business_user_id"),
      userUuid = "", //populate on the calling public method where userId is already there, else modify the sql query to include uuid from users table
      extraAttributeName = row[String]("extra_attribute_type"),
      attributeValue = row[String]("attribute_value"),
      createdAt = row[LocalDateTime]("created_at"),
      createdBy = row[String]("created_by"),
      updatedAt = row[Option[LocalDateTime]]("updated_at"),
      updatedBy = row[Option[String]]("updated_by"))
  }

  private def convertRowToExtraAttributeType(row: Row) = Try {
    ExtraAttributeType(
      attributeName = row[String]("attribute_type_name"),
      description = row[Option[String]]("description"),
      isActive = row[Boolean]("is_active"),
      createdAt = row[LocalDateTime]("created_at"),
      createdBy = row[String]("created_by"),
      updatedAt = row[Option[LocalDateTime]]("updated_at"),
      updatedBy = row[Option[String]]("updated_by"))
  }

  private def findBusinessUserExtraAttrByUserIdAndAttrName(
    userId: Int,
    attributeName: String)(implicit connection: Connection): Seq[UserExtraAttribute] = {
    FindBusinessUserExtraAttributeByUserIdAndAttrName
      .on(
        'business_user_id → userId,
        'extra_attribute_type_name → attributeName)
      .as(FindBusinessUserExtraAttributeByUserIdAndAttrName.defaultParser.*)
      .map(row ⇒ convertRowToUserExtraAttribute(row).get)

  }

  private def findExtraAttributeTypesWithStatus(statusName: String)(implicit connection: Connection) = {
    FindAttributesRequiredByStatusName
      .on('status_name → statusName)
      .as(FindAttributesRequiredByStatusName.defaultParser.*)
      .map(row ⇒ convertRowToExtraAttributeType(row).get).toSet
  }
}
