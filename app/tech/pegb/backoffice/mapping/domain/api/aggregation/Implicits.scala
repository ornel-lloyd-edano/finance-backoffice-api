package tech.pegb.backoffice.mapping.domain.api.aggregation

import java.time.format.DateTimeFormatter

import tech.pegb.backoffice.api.aggregations.dto.AmountAggregation
import tech.pegb.backoffice.api.aggregations.dto.{Margin ⇒ ApiMargin}
import tech.pegb.backoffice.domain.aggregations.dto.{Margin, TransactionAggregationResult}
import tech.pegb.backoffice.domain.report.dto.CashFlowTotals
import tech.pegb.backoffice.api.aggregations.dto.{CashFlowTotals ⇒ ApiCashFlowTotals}

object Implicits {

  implicit class TransactionAggregationResultAdapter(val arg: TransactionAggregationResult) extends AnyVal {
    def asApi(aggregation: String) = AmountAggregation(
      aggregation = aggregation,
      //amount = fixDoubleTotalDueToDebitCredit(arg.sumAmount.getOrElse(BigDecimal("0")), aggregation),
      amount = arg.sumAmount.getOrElse(BigDecimal("0")),
      currencyCode = arg.criteria.flatMap(_.currencyCode).getOrElse("unknown"),
      transactionType = arg.transactionTypeGrouping,
      institution = arg.institutionGrouping,
      timePeriod = (arg.date, arg.week, arg.month) match {
        case (Some(date), _, _) ⇒
          Some(date.format(DateTimeFormatter.ISO_DATE))
        case (_, Some(week), _) ⇒
          Some(week.toString)
        case (_, _, Some(month)) ⇒
          Some(month.toString)
        case _ ⇒ None
      })
  }

  implicit class MarginAdapter(val arg: Margin) extends AnyVal {
    def asApi = ApiMargin(
      margin = arg.margin,
      currencyCode = arg.currencyCode,
      transactionType = arg.transactionType,
      institution = arg.institution,
      timePeriod = (arg.date, arg.week, arg.month) match {
        case (Some(date), _, _) ⇒
          Some(date.format(DateTimeFormatter.ISO_DATE))
        case (_, Some(week), _) ⇒
          Some(week.toString)
        case (_, _, Some(month)) ⇒
          Some(month.toString)
        case _ ⇒ None
      })
  }

  implicit class CashFlowTotalsAdapter(val arg: CashFlowTotals) extends AnyVal {
    def asApi = ApiCashFlowTotals(
      totalBankTransfer = arg.totalBankTransfer.setScale(2, BigDecimal.RoundingMode.HALF_EVEN),
      totalCashIn = arg.totalCashin.setScale(2, BigDecimal.RoundingMode.HALF_EVEN),
      totalCashOut = arg.totalCashout.setScale(2, BigDecimal.RoundingMode.HALF_EVEN),
      totalTransaction = arg.totalTransactions.setScale(2, BigDecimal.RoundingMode.HALF_EVEN))
  }
}
