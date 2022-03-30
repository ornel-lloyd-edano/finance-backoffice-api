package tech.pegb.backoffice.api.currencyexchange

object Constants {
  val validCurrencyExchangesPartialMatchFields = Set("disabled", "id", "currency_code", "base_currency", "provider")

  val currencyExchangeValidSorter = Set("id", "currency_code", "base_currency", "rate",
    "provider", "balance", "status", "updated_at")

  val validSpreadsPartialMatchFields = Set("disabled", "id", "currency_exchange_id")

  val validSpreadsOrderByFields = Set("id", "currency_exchange_id", "currency", "transaction_type",
    "channel", "institution", "spread", "created_at", "updated_at")

  //Api Fields to Database column mapping
  val currencyExchangeColumnMapping = Map(
    "provider" → "provider_name",
    "last_updated" → "updated_at")
}
