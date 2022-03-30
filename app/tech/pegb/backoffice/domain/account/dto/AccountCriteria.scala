package tech.pegb.backoffice.domain.account.dto

import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{MsisdnLike, NameAttribute}
import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class AccountCriteria(
    customerId: Option[UUIDLike] = None,
    customerFullName: Option[String] = None,
    anyCustomerName: Option[NameAttribute] = None,
    msisdn: Option[MsisdnLike] = None,
    isMainAccount: Option[Boolean] = None,
    currency: Option[String] = None,
    status: Option[String] = None,
    accountType: Option[String] = None,
    accountNumber: Option[String] = None,
    accountNumbers: Option[Set[String]] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch
