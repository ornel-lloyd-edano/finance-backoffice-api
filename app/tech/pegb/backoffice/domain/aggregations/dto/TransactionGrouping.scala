package tech.pegb.backoffice.domain.aggregations.dto

case class TransactionGrouping(
    currencyCode: Boolean = false,
    institution: Boolean = false,
    transactionType: Boolean = false,
    primaryAccountNumber: Boolean = false,
    daily: Boolean = false,
    weekly: Boolean = false,
    monthly: Boolean = false) {

  def isOnlyWithCurrencyGrouping: Boolean = {
    currencyCode && !institution && !transactionType && !primaryAccountNumber && !daily && !weekly && !monthly
  }

}
