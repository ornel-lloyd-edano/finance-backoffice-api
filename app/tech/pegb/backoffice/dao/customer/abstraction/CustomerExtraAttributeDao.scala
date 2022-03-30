package tech.pegb.backoffice.dao.customer.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao

import tech.pegb.backoffice.dao.customer.entity.ExtraAttributeRequirements.{ExtraAttributeType, ExtraAttributeTypeToCreate, UserExtraAttribute, UserExtraAttributeToAdd}
import tech.pegb.backoffice.dao.customer.sql.CustomerExtraAttributeSqlDao

@ImplementedBy(classOf[CustomerExtraAttributeSqlDao])
trait CustomerExtraAttributeDao extends Dao {
  def addExtraAttributeType(newAttributeType: ExtraAttributeTypeToCreate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[UserExtraAttribute]

  def getExtraAttributeTypes: DaoResponse[Set[ExtraAttributeType]]

  def getBusinessUserExtraAttributes(userId: String): DaoResponse[Set[UserExtraAttribute]]

  def getBusinessUserExtraAttributesByAttribute(userId: String, attributeName: String): DaoResponse[Seq[UserExtraAttribute]]

  def addBusinessUserExtraAttribute(userId: String, attributeName: String, value: String, createdBy: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Unit]

  def addBusinessUserExtraAttributes(attributesToAdd: Seq[UserExtraAttributeToAdd])(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Unit]

  def updateBusinessUserExtraAttribute(userId: String, attributeName: String, newValue: String, updatedBy: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[UserExtraAttribute]]

  def enableBusinessUserExtraAttribute(userId: String, attributeName: String, enabledBy: String): DaoResponse[Option[UserExtraAttribute]]

  def disableBusinessUserExtraAttribute(userId: String, attributeName: String, disabledBy: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[UserExtraAttribute]]

  def deleteBusinessUserExtraAttribute(userId: String, attributeName: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Unit]

  def getExtraAttributesRequiredByCustomerStatus(statusName: String): DaoResponse[Set[ExtraAttributeType]]

  def getExtraAttributesRequiredByTier(tierName: String): DaoResponse[Set[ExtraAttributeType]]

  def getExtraAttributesRequiredBySubscription(subscriptionName: String): DaoResponse[Set[ExtraAttributeType]]

  def getExtraAttributesRequiredByBusinessUserType(buType: String): DaoResponse[Set[ExtraAttributeType]]

  def getExtraAttributesRequiredBySegmentType(segmentType: String): DaoResponse[Set[ExtraAttributeType]]
}
