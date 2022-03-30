package tech.pegb.backoffice.domain.aggregations.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.aggregations.TxnAggServiceResponse
import tech.pegb.backoffice.domain.aggregations.implementation.{TransactionAggregationService ⇒ TransactionAggregationServiceImpl}
import tech.pegb.backoffice.domain.aggregations.dto.{TransactionGrouping, TxnAggregationsCriteria}
import tech.pegb.backoffice.domain.model.Ordering

@ImplementedBy(classOf[TransactionAggregationServiceImpl])
trait TransactionAggregationFactory {

  def getAggregationFunction(name: String): Option[(TxnAggregationsCriteria, Option[TransactionGrouping], Seq[Ordering], Option[Boolean], Option[Int]) ⇒ TxnAggServiceResponse]

}
