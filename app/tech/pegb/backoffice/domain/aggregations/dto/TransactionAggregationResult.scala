package tech.pegb.backoffice.domain.aggregations.dto

import java.time.{LocalDate}

import tech.pegb.backoffice.util.time.{Month, Week}

case class TransactionAggregationResult(
    sumAmount: Option[BigDecimal] = None,
    avgAmount: Option[BigDecimal] = None,
    txnCount: Option[Long] = None,
    criteria: Option[TxnAggregationsCriteria] = None,
    currencyCodeGrouping: Option[String] = None,
    institutionGrouping: Option[String] = None,
    transactionTypeGrouping: Option[String] = None,
    date: Option[LocalDate] = None,
    week: Option[Week] = None,
    month: Option[Month] = None) {

}
