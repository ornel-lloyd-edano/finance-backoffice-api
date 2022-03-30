package tech.pegb.backoffice.dao.transaction.dto

case class TransactionGroupings(
    primaryAccountId: Boolean = false,
    transactionType: Boolean = false,
    channel: Boolean = false,
    status: Boolean = false,
    currencyCode: Boolean = false,
    provider: Boolean = false)
