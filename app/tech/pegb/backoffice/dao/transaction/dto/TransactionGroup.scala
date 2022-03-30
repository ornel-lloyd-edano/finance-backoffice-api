package tech.pegb.backoffice.dao.transaction.dto

case class TransactionGroup(
    accountId: Option[String] = None,
    transactionType: Option[String] = None,
    channel: Option[String] = None,
    status: Option[String] = None,
    currencyCode: Option[String] = None,
    provider: Option[String] = None)
