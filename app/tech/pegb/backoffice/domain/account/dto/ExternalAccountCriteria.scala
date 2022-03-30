package tech.pegb.backoffice.domain.account.dto

import java.util.UUID

import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class ExternalAccountCriteria(
    id: Option[UUIDLike] = None,
    anyIds: Option[Set[UUID]] = None,
    customerId: Option[UUIDLike] = None,
    customerName: Option[String] = None,
    externalProvider: Option[String] = None,
    externalAccountHolder: Option[String] = None,
    externalAccountNumber: Option[String] = None,
    currency: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch {

}
