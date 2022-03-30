package tech.pegb.backoffice.dao.currency.dto

case class CurrencyToUpsert(
    id: Option[Int],
    name: String,
    description: Option[String],
    isActive: Boolean,
    icon: Option[String])
