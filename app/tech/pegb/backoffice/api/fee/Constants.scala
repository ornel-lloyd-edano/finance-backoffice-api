package tech.pegb.backoffice.api.fee

object Constants {

  val feeProfileValidSorter = Set("id", "fee_type", "user_type", "subscription_type",
    "transaction_type", "channel", "other_party", "instrument", "calculation_method", "currency_code", "fee_method")

  val feeProfilePartialMatchFields = Set("disabled", "id", "other_party")

  val feeProfilesApiToTableColumnMapping = Map(
    "id" → "uuid").withDefault(identity)
}
