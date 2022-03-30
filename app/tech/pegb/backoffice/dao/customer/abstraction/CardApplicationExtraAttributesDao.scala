package tech.pegb.backoffice.dao.customer.abstraction

import java.sql.Connection

import tech.pegb.backoffice.dao.Dao

import tech.pegb.backoffice.dao.customer.entity.ExtraAttributeRequirements.{CardApplicationRequirement, ExtraAttributeType}

trait CardApplicationExtraAttributesDao extends Dao {
  def getCardApplicationRequirements(id: String): DaoResponse[Set[CardApplicationRequirement]]

  def getCardTypeExtraAttributes: DaoResponse[Set[ExtraAttributeType]]

  def getOperationTypeExtraAttributes: DaoResponse[Set[ExtraAttributeType]]

  def deleteCardApplicationRequirement(cardApplicationId: String, extraAttributeId: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Unit]

  def deactivateCardApplicationRequirement(cardApplicationId: String, extraAttributeId: String, deactivatedBy: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[CardApplicationRequirement]]

  def enableCardApplicationRequirement(cardApplicationId: String, extraAttributeId: String, enabledBy: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[CardApplicationRequirement]]

  def updateCardApplicationRequirement(cardApplicationId: String, extraAttributeId: String, newValue: String, updatedBy: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[CardApplicationRequirement]]
}

