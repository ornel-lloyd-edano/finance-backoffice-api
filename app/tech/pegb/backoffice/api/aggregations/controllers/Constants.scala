package tech.pegb.backoffice.api.aggregations.controllers

object Constants {

  object Aggregations {
    val GrossRevenue = "gross_revenue"
    val ThirdPartyFees = "third_party_fees"
    val TurnOver = "turnover"
    val AccountBalance = "account_balance"
    val Balance = "balance"
    val Amount = "amount"
    val ProviderTurnover = "get_provider_turnover"
    val BankTransfer = "bank_transfer"
    val TotalCashIn = "total_cash_in"
    val TotalCashOut = "total_cash_out"
    val TotalEtcTxns = "total_etc_txns"
  }

  object Frequencies {
    val Daily = "daily"
    val Weekly = "weekly"
    val Monthly = "monthly"
  }

  object Grouping {
    val CurrencyCode = "currency_code"
    val TxnType = "transaction_type"
    val Institution = "institution"
    val TimePeriod = "time_period"
  }

  object Order {
    val CurrencyCode = "currency_code"
    val TxnType = "transaction_type"
    val Institution = "institution"
    val TimePeriod = "time_period"
    val Amount = "amount"
  }

  object TxnType {
    val CashIn = "cashin"
    val CashOut = "cashout"
    val EtcTxns = "etc_transactions"
    // required for the cashflow report aggregation
    val BankTransfer = "bank_transfer"
  }

  object AccType {
    val Collection = "collection"
    val Distribution = "distribution"
    //val EndCustomer = "end_customer"
    val UserBalanceWallet = "standard_wallet"
    val UserBalanceSaving = "standard_saving"
  }

  object UsersType {
    val Individual = "individual"
    val Provider = "provider"
  }

  object AccountNumber {
    val FilterNotPegbFees = "pegb_fees."
  }

  val ClosingUserBalance = "closing_user_balance"

  val ValidAccntType = Set(AccType.Collection, AccType.Distribution, AccType.UserBalanceWallet, AccType.UserBalanceSaving)

  val ValidTxnType = Set(TxnType.BankTransfer, TxnType.CashIn, TxnType.CashOut, TxnType.EtcTxns)

  val ValidOrderBy = Set(Order.CurrencyCode, Order.TxnType, Order.Institution, Order.TimePeriod, Order.Amount)

  val ValidGroupBy = Set(Grouping.CurrencyCode, Grouping.TxnType, Grouping.Institution, Grouping.TimePeriod)

  val ValidUserType = Set(UsersType.Individual, UsersType.Provider)

  val ValidFrequency = Set(Frequencies.Daily, Frequencies.Weekly, Frequencies.Monthly)

  val ValidAggregations = Set(Aggregations.TurnOver, Aggregations.GrossRevenue, Aggregations.ThirdPartyFees, Aggregations.AccountBalance, Aggregations.Balance, Aggregations.Amount, Aggregations.ProviderTurnover)

  val ValidCriteria = Set()

}
