package tech.pegb.backoffice.domain.customer.implementation

import java.util.UUID

import com.google.inject.Inject
import tech.pegb.backoffice.dao.customer.abstraction.PaymentOptionDao
import tech.pegb.backoffice.domain.customer.abstraction.PaymentOptionDomain
import tech.pegb.backoffice.domain.customer.model.PaymentOption
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits.PaymentOptionAdapter
import tech.pegb.backoffice.util.WithExecutionContexts

import scala.concurrent.{ExecutionContext, Future}

class PaymentOptionService @Inject() (
    executionContexts: WithExecutionContexts,
    dao: PaymentOptionDao) extends PaymentOptionDomain {

  private implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  override def getPaymentOptions(
    customerId: UUID)(
    implicit
    requestId: UUID): Future[ServiceResponse[Seq[PaymentOption]]] = Future {
    dao.fetchPaymentOptions(customerId)
      .map(_.map(_.asDomain))
      .asServiceResponse
  }

}
