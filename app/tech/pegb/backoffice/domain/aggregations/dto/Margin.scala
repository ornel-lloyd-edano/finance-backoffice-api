package tech.pegb.backoffice.domain.aggregations.dto

import java.time.LocalDate

import tech.pegb.backoffice.util.time.{Month, Week}

case class Margin(
    margin: BigDecimal,
    currencyCode: String,
    transactionType: Option[String] = None,
    institution: Option[String] = None,
    date: Option[LocalDate] = None,
    week: Option[Week] = None,
    month: Option[Month] = None) {

}
