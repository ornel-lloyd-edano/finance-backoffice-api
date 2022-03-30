package tech.pegb.backoffice.mapping.dao.domain.aggregation

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import tech.pegb.backoffice.dao.aggregations.dto.AggregationResult
import tech.pegb.backoffice.dao.fee.entity.ThirdPartyFeeProfileRange
import tech.pegb.backoffice.domain.aggregations.dto.{FeeRange, TransactionAggregationResult}
import tech.pegb.backoffice.dao.DbConstants._
import tech.pegb.backoffice.util.time.{Month, Week}
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

object Implicits {

  implicit class AmountAggregationResultAdapter(val arg: AggregationResult) extends AnyVal {
    def asDomain(amountColumnOrAlias: String) = Try(TransactionAggregationResult(
      sumAmount = arg.aggregations.find(_.columnOrAlias === amountColumnOrAlias).map(v ⇒ BigDecimal(v.value)),
      currencyCodeGrouping = arg.grouping.find(_.column === currencyName).map(_.value),
      institutionGrouping = arg.grouping.collectFirst { case p if p.column === txnProviderAlias && !p.value.trim.isEmpty ⇒ p.value },
      transactionTypeGrouping = arg.grouping.find(_.column === txnType).map(_.value),
      date = arg.grouping.find(_.column === "date").map(g ⇒ LocalDate.parse(g.value, DateTimeFormatter.ISO_DATE)),
      week = arg.grouping.find(_.column === "week").map(g ⇒ Week.parse(g.value)),
      month = arg.grouping.find(_.column === "month").map(g ⇒ Month.parse(g.value))))
  }

  implicit class ThirdPartyFeeProfileRangesAdapter(val arg: ThirdPartyFeeProfileRange) extends AnyVal {
    def asDomain(isPercentage: Boolean) = FeeRange(
      min = arg.min,
      max = arg.max,
      value = if (isPercentage) {
        arg.feeRatio.getOrElse(BigDecimal("0.0"))
      } else {
        arg.feeAmount.getOrElse(BigDecimal("0.0"))
      })
  }

}
