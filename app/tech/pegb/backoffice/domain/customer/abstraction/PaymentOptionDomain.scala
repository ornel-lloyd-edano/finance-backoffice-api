package tech.pegb.backoffice.domain.customer.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.customer.implementation.PaymentOptionService
import tech.pegb.backoffice.domain.customer.model.PaymentOption

import scala.concurrent.Future

@ImplementedBy(classOf[PaymentOptionService])
trait PaymentOptionDomain extends BaseService {

  def getPaymentOptions(customerId: UUID)(implicit requestId: UUID): Future[ServiceResponse[Seq[PaymentOption]]]

}
