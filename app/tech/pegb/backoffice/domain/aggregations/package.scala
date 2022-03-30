package tech.pegb.backoffice.domain

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.aggregations.dto.{TransactionAggregationResult}

import scala.concurrent.Future

package object aggregations {

  type TxnAggServiceResponse = Future[ServiceResponse[Seq[TransactionAggregationResult]]]

}
