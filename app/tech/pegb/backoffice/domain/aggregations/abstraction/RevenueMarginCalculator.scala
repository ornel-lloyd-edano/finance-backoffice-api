package tech.pegb.backoffice.domain.aggregations.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.aggregations.dto.Margin
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.aggregations.implementation.{RevenueMarginCalculator ⇒ RevenueMarginCalculatorImpl}
import tech.pegb.backoffice.domain.aggregations.dto.{TransactionGrouping, TxnAggregationsCriteria}
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[RevenueMarginCalculatorImpl])
trait RevenueMarginCalculator {

  def getRevenueMargin(
    mayBeGrossRevenue: Option[BigDecimal],
    mayBeTurnover: Option[BigDecimal],
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Margin]]]

}
