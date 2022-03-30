package tech.pegb.backoffice.domain.customer.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.dto.IndividualUserToUpdate
import tech.pegb.backoffice.domain.customer.implementation.CustomerUpdateService
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.BusinessUsers.ActivatedBusinessUser
import tech.pegb.backoffice.domain.customer.model.IndividualUsers._

import scala.concurrent.Future

@ImplementedBy(classOf[CustomerUpdateService])
trait CustomerUpdate {

  def updateIndividualUser(userId: UUID, updatedIndividualUser: IndividualUserToUpdate, doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[IndividualUser]]

  def updateSegment(buIdToFind: UUID, newSegment: CustomerSegment, doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]]

  def updateTier(buIdToFind: UUID, newTier: CustomerTier, doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]]

  def updateSubscription(buIdToFind: UUID, newSubscription: CustomerSubscription, doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]]

  def updateEmails(buIdToFind: UUID, newEmails: Set[Email], doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]]

  def updateBusinessUserType(buIdToFind: UUID, newBuType: BusinessUserType, doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]]

  def updateAddresses(buIdToFind: UUID, newAddresses: Set[Address], doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]]

  def updatePhoneNumbers(buIdToFind: UUID, newPhoneNumbers: Set[PhoneNumber], doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]]

}
