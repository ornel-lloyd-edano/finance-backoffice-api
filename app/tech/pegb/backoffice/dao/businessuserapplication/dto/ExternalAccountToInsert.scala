package tech.pegb.backoffice.dao.businessuserapplication.dto

case class ExternalAccountToInsert(
    provider: String,
    accountNumber: String,
    accountHolder: String,
    currencyId: Int)
