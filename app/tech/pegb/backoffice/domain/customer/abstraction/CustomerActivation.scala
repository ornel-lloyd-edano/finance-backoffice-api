package tech.pegb.backoffice.domain.customer.abstraction

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.application.model.NotificationMessage
import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.customer.implementation.CustomerActivationService
import tech.pegb.backoffice.domain.customer.model.BusinessUsers.{ActivatedBusinessUser, RegisteredButNotActivatedBusinessUser}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.IndividualUsers._

import scala.concurrent.Future

@ImplementedBy(classOf[CustomerActivationService])
trait CustomerActivation {

  def getBusinessUserActivationRequirements: Future[ServiceResponse[Set[ActivationDocumentType]]]

  def activateBusinessUserOnTheFly(
    userId: UUID,
    requiredDocuments: Seq[ActivationRequirement],
    doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]]

  def activateBusinessUser(userId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]]

  def deactivateBusinessUser(userId: UUID, reason: String, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[RegisteredButNotActivatedBusinessUser]]

  def notifyBusinessUserForActivation(userId: UUID): Future[ServiceResponse[NotificationMessage]]

  def activateIndividualUser(customerId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[IndividualUser]]

  def deactivateIndividualUser(customerId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[IndividualUser]]
}
