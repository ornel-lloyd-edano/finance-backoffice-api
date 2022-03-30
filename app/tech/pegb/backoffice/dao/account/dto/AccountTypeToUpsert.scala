package tech.pegb.backoffice.dao.account.dto

case class AccountTypeToUpsert(
    id: Option[Int],
    accountTypeName: String,
    description: Option[String],
    isActive: Boolean)
