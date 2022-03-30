package tech.pegb.backoffice.domain.customer.abstraction

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.customer.implementation.CardMgmtService
import tech.pegb.backoffice.domain.customer.model.CardApplication
import tech.pegb.backoffice.domain.customer.model.CardApplicationAttributes.{Card, CardApplicationRequirement, CardPin, CardType}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{Address, NameAttribute}

import scala.concurrent.Future

@ImplementedBy(classOf[CardMgmtService])
trait CardManagement {

  def applyForCard(
    customerId: UUID,
    cardType: CardType,
    cardApplicationRequirements: Seq[CardApplicationRequirement],
    nameOnCard: NameAttribute,
    maybeCardPin: Option[CardPin],
    cardDeliveryAddress: Address,
    doneAt: LocalDateTime,
    doneByBackOfficeUserName: String): Future[ServiceResponse[CardApplication]]

  def getCardApplication(applicationId: UUID): Future[ServiceResponse[CardApplication]]

  def approveCardApplication(applicationId: UUID, doneByBackOfficeUserName: String): Future[ServiceResponse[CardApplication]]

  def rejectCardApplication(applicationId: UUID, reason: String, doneByBackOfficeUserName: String): Future[ServiceResponse[Unit]]

  def getCardApplications(dateFrom: Option[LocalDate], dateTo: Option[LocalDate]): Future[ServiceResponse[CardApplication]]

  def getCardDetails(cardNumber: String): Future[ServiceResponse[Card]]

  def resetCardPin(cardNumber: String, newPin: String): Future[ServiceResponse[Card]]

  def blockCard(cardNumber: String, doneByBackOfficeUserName: String): Future[ServiceResponse[Card]]

  def unblockCard(cardNumber: String, doneByBackOfficeUserName: String): Future[ServiceResponse[Card]]

  def activateCard(cardNumber: String, doneByBackOfficeUserName: String): Future[ServiceResponse[Card]]

  def cancelCard(cardNumber: String, reason: String, doneByBackOfficeUserName: String): Future[ServiceResponse[Unit]]
}
