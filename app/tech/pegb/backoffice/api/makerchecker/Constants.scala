package tech.pegb.backoffice.api.makerchecker

object Constants {

  val makerCheckerSorter = Set("id", "module", "status",
    "action", "created_at", "created_by", "checked_at", "checked_by", "updated_at")

  val makerCheckerPartialMatchFields = Set("disabled", "id", "status", "module")

}

