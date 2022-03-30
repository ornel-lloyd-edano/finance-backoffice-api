package tech.pegb.backoffice.domain.transaction.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.domain.transaction.model.TransactionStatus
import tech.pegb.backoffice.util.time.DateTimeRangeValidatable
import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class TransactionCriteria(
    id: Option[String] = None, //made it String for now, not sure if UUID or Int, String is more flexible
    anyCustomerName: Option[String] = None,
    customerId: Option[UUIDLike] = None,
    accountId: Option[UUIDLike] = None,
    accountNumbers: Seq[String] = Seq.empty,
    startDate: Option[LocalDateTime] = None,
    endDate: Option[LocalDateTime] = None,
    transactionType: Option[String] = None,
    channel: Option[String] = None,
    status: Option[TransactionStatus] = None,
    currencyCode: Option[String] = None,
    direction: Option[String] = None,
    accountType: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch with DateTimeRangeValidatable

object TransactionCriteria {
  val empty = new TransactionCriteria()
}
