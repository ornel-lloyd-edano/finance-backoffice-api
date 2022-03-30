package tech.pegb.backoffice.api.limit

object Constants {

  val limitProfileValidSorter = Set("id", "limit_type", "user_type", "tier", "subscription",
    "transaction_type", "channel", "other_party", "instrument", "max_amount_per_interval", "max_amount_per_txn",
    "min_amount_per_txn", "max_count_per_interval", "interval", "currency_code", "max_balance_amount", "created_at")

  val limitProfilePartialMatchFields = Set("disabled", "id", "other_party")

  val limitProfilesApiToTableColumnMapping = Map(
    "id" → "uuid",
    "max_amount_per_interval" → "max_interval_amount",
    "max_balance_amount" → "max_amount",
    "max_amount_per_txn" → "max_amount",
    "min_amount_per_txn" → "min_amount",
    "max_count_per_interval" → "max_count").withDefault(identity)
}
