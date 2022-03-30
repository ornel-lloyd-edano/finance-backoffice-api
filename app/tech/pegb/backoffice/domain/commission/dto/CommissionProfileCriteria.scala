package tech.pegb.backoffice.domain.commission.dto

import java.util.Currency

import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessType, BusinessUserTier}
import tech.pegb.backoffice.domain.transaction.model.TransactionType
import tech.pegb.backoffice.domain.types.enum.CommissionCalculationMethod
import tech.pegb.backoffice.util.UUIDLike

case class CommissionProfileCriteria(
    uuid: Option[UUIDLike] = None,
    businessType: Option[BusinessType] = None,
    tier: Option[BusinessUserTier] = None,
    subscriptionType: Option[String] = None,
    transactionType: Option[TransactionType] = None,
    currency: Option[Currency] = None,
    channel: Option[String] = None,
    instrument: Option[String] = None,
    calculationMethod: Option[CommissionCalculationMethod] = None,
    partialMatchFields: Set[String] = Set.empty)

