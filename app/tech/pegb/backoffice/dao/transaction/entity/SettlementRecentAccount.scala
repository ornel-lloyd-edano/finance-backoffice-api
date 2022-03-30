package tech.pegb.backoffice.dao.transaction.entity

case class SettlementRecentAccount(
    accountId: Int,
    accountUUID: String,
    customerName: Option[String],
    accountNumber: String,
    accountName: Option[String],
    balance: BigDecimal,
    currency: String)
