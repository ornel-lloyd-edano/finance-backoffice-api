package tech.pegb.backoffice.domain.fee.dto

import java.util.Currency

import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerSubscription
import tech.pegb.backoffice.domain.customer.model.{UserTier, UserType}
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.{FeeCalculationMethod, FeeMethod, FeeType, TaxInclusionType}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class FeeProfileCriteria(
    id: Option[UUIDLike] = None,
    feeType: Option[FeeType] = None,
    userType: Option[UserType] = None,
    tier: Option[UserTier] = None,
    subscription: Option[CustomerSubscription] = None,
    transactionType: Option[TransactionType] = None,
    channel: Option[Channel] = None,
    otherParty: Option[String] = None,
    instrument: Option[String] = None,
    calculationMethod: Option[FeeCalculationMethod] = None,
    currencyCode: Option[Currency] = None,
    feeMethod: Option[FeeMethod] = None,
    taxInclusion: Option[TaxInclusionType] = None,
    isDeleted: Option[Boolean] = Option(false),
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch {

}
