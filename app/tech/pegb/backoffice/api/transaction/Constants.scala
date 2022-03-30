package tech.pegb.backoffice.api.transaction

object Constants {

  val defaultOrdering = "-created_at,sequence"

  val validOrderByFields = Set("id", "sequence", "primary_account_id", "direction", "type", "amount",
    "currency_id", "receiver_phone", "channel", "status", "created_at", "updated_at")

  val getTransactionsPartialMatchFields = Set("any_customer_name", "customer_id", "account_id")
}
