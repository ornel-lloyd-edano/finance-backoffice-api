package tech.pegb.backoffice.dao.businessuserapplication.dto

case class AccountConfigToInsert(
    accountType: String,
    accountName: String,
    currencyId: Int,
    isDefault: Int)
