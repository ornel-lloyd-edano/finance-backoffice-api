package tech.pegb.backoffice.domain.transaction.model

case class SettlementRecentAccount(
    accountId: Int,
    accountUUID: String,
    customerName: Option[String],
    accountNumber: String,
    accountName: Option[String],
    balance: BigDecimal,
    currency: String)
