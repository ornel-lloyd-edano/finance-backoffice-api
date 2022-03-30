package tech.pegb.backoffice.api.reportsv2

object Constants {
  val reportDefinitionValidSorter = Set("id", "name", "title", "description",
    "created_at", "updated_at")

  val apiToDbNames = Map(
    "name" -> "report_name", "title" -> "report_title", "description" -> "report_description",
    "columns" -> "report_columns", "grouping" -> "grouping_columns", "sql" -> "raw_sql")

  val reportDefinitionPartialMatchFields = Set("disabled", "id", "description")

  private val reportDefinitionColumnMapping = Map(
    "customer" → "user_id",
    "is_main_account" → "is_main_account",
    "currency" → "currency",
    "status" → "status",
    "account_type" → "type",
    "balance" → "balance",
    "last_transaction_at" → "last_transaction_at",
    "created_at" → "created_at",
    "updated_at" → "updated_at")
}
