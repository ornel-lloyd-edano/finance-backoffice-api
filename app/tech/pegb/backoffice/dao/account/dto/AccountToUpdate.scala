package tech.pegb.backoffice.dao.account.dto

import java.time.LocalDateTime

case class AccountToUpdate(
    accountNumber: Option[String] = None,
    accountName: Option[String] = None,
    accountType: Option[String] = None,
    isMainAccount: Option[Boolean] = None,
    currency: Option[String] = None,
    balance: Option[BigDecimal] = None,
    blockedBalance: Option[BigDecimal] = None,
    status: Option[String] = None,
    updatedAt: LocalDateTime,
    updatedBy: String)
