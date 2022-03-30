package tech.pegb.backoffice.dao.currency.dto

import java.time.LocalDateTime

//TODO add lastUpdatedAt: Option[LocalDateTime]
case class CurrencyToUpdate(
    currencyName: Option[String],
    description: Option[String],
    isActive: Option[Boolean],
    icon: Option[String],
    updatedAt: LocalDateTime,
    updatedBy: String)
