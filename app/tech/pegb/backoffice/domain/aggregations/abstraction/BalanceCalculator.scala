package tech.pegb.backoffice.domain.aggregations.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.aggregations.dto.{TransactionAggregationResult, TransactionGrouping}
import tech.pegb.backoffice.domain.aggregations.implementation.BalanceCalculatorService

@ImplementedBy(classOf[BalanceCalculatorService])
trait BalanceCalculator {

  def computeBalancePerTimePeriod(
    rawAggregationResult: Seq[TransactionAggregationResult],
    transactionGrouping: Option[TransactionGrouping],
    dateFrom: Option[LocalDateTime],
    dateTo: Option[LocalDateTime]): Seq[TransactionAggregationResult]
}
