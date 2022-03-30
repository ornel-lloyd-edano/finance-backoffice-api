package tech.pegb.backoffice.dao.account.dto

import java.time.LocalDateTime

case class AccountToInsert(
    accountNumber: String,
    userId: String,
    accountName: String,
    accountType: String,
    isMainAccount: Boolean,
    currency: String,
    balance: BigDecimal,
    blockedBalance: BigDecimal,
    status: String,
    mainType: String,
    createdAt: LocalDateTime,
    createdBy: String)
