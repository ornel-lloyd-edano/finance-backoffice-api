package tech.pegb.backoffice.domain.aggregations.implementation

import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.aggregations.abstraction.{TransactionAggregationService ⇒ TransactionAggregationServiceT, RevenueMarginCalculator ⇒ RevenueMarginCalculatorTrait}
import tech.pegb.backoffice.domain.aggregations.dto.{Margin, TransactionGrouping, TxnAggregationsCriteria}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RevenueMarginCalculator @Inject() (
    executionContexts: WithExecutionContexts,
    appConfig: AppConfig,
    txnAggService: TransactionAggregationServiceT) extends RevenueMarginCalculatorTrait with BaseService {

  implicit val ec: ExecutionContext = executionContexts.blockingIoOperations

  def getRevenueMargin(
    mayBeGrossRevenue: Option[BigDecimal],
    mayBeTurnover: Option[BigDecimal],
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Margin]]] =

    (mayBeGrossRevenue, mayBeTurnover) match {

      case (Some(revenue), Some(turnover)) ⇒
        val margin = (turnover * revenue) / 100
        Future.successful(Seq(Margin(
          margin = margin,
          currencyCode = criteria.currencyCode.getOrElse("Unknown"),
          transactionType = criteria.transactionType,
          institution = criteria.institution,
          date = criteria.startDate.fold(criteria.endDate.map(_.toLocalDate))(date ⇒ Some(date.toLocalDate)),
          week = None,
          month = None)).toRight)

      case _ ⇒ (for {

        turnovers ← EitherT(txnAggService.getTurnOver(criteria, grouping, orderBy, None, None))
        revenues ← EitherT(txnAggService.getGrossRevenue(criteria, grouping, orderBy, None, None))
      } yield {
        for {
          revenue ← revenues
          turnover ← turnovers
        } yield {
          val margin = (turnover.sumAmount.getOrElse(BigDecimal(0.0)) * revenue.sumAmount.getOrElse(BigDecimal(0.0))) / 100

          Margin(
            margin = margin,
            currencyCode = criteria.currencyCode.getOrElse("Unknown"),
            transactionType = criteria.transactionType,
            institution = criteria.institution,
            date = criteria.startDate.fold(criteria.endDate.map(_.toLocalDate))(date ⇒ Some(date.toLocalDate)),
            week = None,
            month = None)
        }
      }).value
    }

}
