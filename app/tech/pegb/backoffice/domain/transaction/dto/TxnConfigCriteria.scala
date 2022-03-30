package tech.pegb.backoffice.domain.transaction.dto

import java.util.UUID

import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class TxnConfigCriteria(
    id: Option[UUIDLike] = None,
    anyIds: Option[Set[UUID]] = None,
    customerId: Option[UUIDLike] = None,
    customerName: Option[String] = None,
    transactionType: Option[String] = None,
    currency: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch {

}
