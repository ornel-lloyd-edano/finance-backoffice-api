package tech.pegb.backoffice.domain.aggregations.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.aggregations.TxnAggServiceResponse
import tech.pegb.backoffice.domain.aggregations.dto.{TransactionGrouping, TxnAggregationsCriteria}
import tech.pegb.backoffice.domain.aggregations._
import tech.pegb.backoffice.domain.model.Ordering

@ImplementedBy(classOf[implementation.TransactionAggregationService])
trait TransactionAggregationService {

  def getGrossRevenue(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean],
    numDaysWhenToSwitchDataSource: Option[Int]): TxnAggServiceResponse

  def getTurnOver(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean],
    numDaysWhenToSwitchDataSource: Option[Int]): TxnAggServiceResponse

  def getThirdPartyFees(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean],
    numDaysWhenToSwitchDataSource: Option[Int]): TxnAggServiceResponse

  def getTotalBalance(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean],
    numDaysWhenToSwitchDataSource: Option[Int]): TxnAggServiceResponse

  def getTotalAmount(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean],
    numDaysWhenToSwitchDataSource: Option[Int]): TxnAggServiceResponse

}
