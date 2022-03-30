package tech.pegb.backoffice.domain.customer.implementation

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.Inject
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.customer.abstraction.{BusinessUserDao, CustomerExtraAttributeDao, IndividualUserDao, UserDao}
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.{BaseService}
import tech.pegb.backoffice.domain.customer.abstraction.CustomerUpdate
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes
import tech.pegb.backoffice.util.{AppConfig, ExecutionContexts}
import tech.pegb.backoffice.domain.customer.dto.IndividualUserToUpdate
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.IndividualUser
import tech.pegb.backoffice.mapping.domain.dao.customer.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future

class CustomerUpdateService @Inject() (
    conf: AppConfig,
    executionContexts: ExecutionContexts,
    userDao: UserDao,
    businessUserDao: BusinessUserDao,
    individualUserDao: IndividualUserDao,
    accountDao: AccountDao,
    customerExtraAttributeDao: CustomerExtraAttributeDao) extends CustomerUpdate with BaseService {

  implicit val executionContext = executionContexts.blockingIoOperations

  def updateIndividualUser(userId: UUID, updatedIndividualUser: IndividualUserToUpdate, doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[IndividualUser]] = Future {
    individualUserDao.updateIndividualUser(
      userId.toString,
      updatedIndividualUser.asDao
        .copy(updatedAt = Some(doneAt), updatedBy = Some(doneByBackOfficeUserName)))(maybeTransaction = None).fold(
        _.asDomainError.toLeft,
        _ match {
          case Some(result) ⇒
            result.asDomain.fold(
              error ⇒ Left(dtoMappingError(s"IndividualUser ${userId.toString} could not be converted to domain object")),
              user ⇒ Right(user))

          case None ⇒ Left(notFoundError(s"IndividualUser ${userId.toString} was not found. Nothing to update."))
        })

  }

  def updateSegment(buIdToFind: UUID, newSegment: CustomerAttributes.CustomerSegment, doneByBackOfficeUserName: String, doneAt: LocalDateTime) = ???

  def updateTier(buIdToFind: UUID, newTier: CustomerAttributes.CustomerTier, doneByBackOfficeUserName: String, doneAt: LocalDateTime) = ???

  def updateSubscription(buIdToFind: UUID, newSubscription: CustomerAttributes.CustomerSubscription, doneByBackOfficeUserName: String, doneAt: LocalDateTime) = ???

  def updateEmails(buIdToFind: UUID, newEmails: Set[Email], doneByBackOfficeUserName: String, doneAt: LocalDateTime) = ???

  def updateBusinessUserType(buIdToFind: UUID, newBuType: CustomerAttributes.BusinessUserType, doneByBackOfficeUserName: String, doneAt: LocalDateTime) = ???

  def updateAddresses(buIdToFind: UUID, newAddresses: Set[CustomerAttributes.Address], doneByBackOfficeUserName: String, doneAt: LocalDateTime) = ???

  def updatePhoneNumbers(buIdToFind: UUID, newPhoneNumbers: Set[CustomerAttributes.PhoneNumber], doneByBackOfficeUserName: String, doneAt: LocalDateTime) = ???

}
