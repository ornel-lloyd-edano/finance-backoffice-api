package tech.pegb.backoffice.domain.currencyexchange.dto

import java.util.Currency

import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class SpreadCriteria(
    id: Option[UUIDLike] = None,
    currencyExchangeId: Option[UUIDLike] = None,
    currencyCode: Option[Currency] = None,
    transactionType: Option[TransactionType] = None,
    channel: Option[Channel] = None,
    recipientInstitution: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty,
    isDeleted: Option[Boolean] = None) extends HasPartialMatch
