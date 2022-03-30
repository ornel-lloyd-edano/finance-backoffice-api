package tech.pegb.backoffice.domain.customer.implementation

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import com.google.inject.Inject
import tech.pegb.backoffice.application.CommunicationService
import tech.pegb.backoffice.dao.customer.abstraction.{CardApplicationDao, UserDao}
import tech.pegb.backoffice.dao.customer.dto.CardApplicationToInsert
import tech.pegb.backoffice.domain.{BaseService}
import tech.pegb.backoffice.domain.customer.abstraction.{CardManagement}
import tech.pegb.backoffice.domain.customer.model.{CardApplication, CardApplicationAttributes, CustomerAttributes}
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class CardMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    cardApplicationDao: CardApplicationDao,
    userDao: UserDao,
    notificationService: CommunicationService) extends CardManagement with BaseService {

  implicit val executionContext = executionContexts.blockingIoOperations

  import CardApplicationAttributes._

  def applyForCard(
    customerId: UUID,
    cardType: CardType,
    cardApplicationRequirements: Seq[CardApplicationRequirement],
    nameOnCard: CustomerAttributes.NameAttribute,
    maybeCardPin: Option[CardPin],
    cardDeliveryAddress: CustomerAttributes.Address,
    doneAt: LocalDateTime,
    doneByBackOfficeUserName: String): Future[ServiceResponse[CardApplication]] = {
    Future {
      userDao.getUser(customerId.toString).fold(
        _.asDomainError.toLeft,
        {
          case Some(customer) ⇒ {
            cardApplicationDao.insertCardApplication(
              CardApplicationToInsert(
                userId = customerId.toString,
                operationType = conf.OperationTypeForNewCardApplication,
                cardType = cardType.underlying,
                nameOnCard = nameOnCard.underlying,
                cardPin = maybeCardPin.map(_.underlying.toString).getOrElse(CardPin.generate.underlying.toString),
                deliveryAddress = cardDeliveryAddress.underlying,
                status = conf.DefaultNewCardApplicationStatus,
                createdAt = doneAt,
                createdBy = doneByBackOfficeUserName)).fold(
                _.asDomainError.toLeft,
                result ⇒ result.asDomain match {
                  case Success(cardApplication) ⇒
                    Right(cardApplication)
                  case Failure(error) ⇒
                    logger.error(s"Error in ${this.getClass.getSimpleName}.applyForNewCard", error)
                    Left(dtoMappingError(s"Failed to convert card application entity to domain."))
                })
          }
          case None ⇒
            Left(notFoundError(s"Customer ${customerId.toString} not found. Unable to create card application"))
        })
    }.recover {
      case error: Exception ⇒
        logger.error("Error encountered in [applyForCard]", error)
        Left(unknownError("Error encountered in card application"))
    }
  }

  def getCardApplication(applicationId: UUID) = ???

  def approveCardApplication(applicationId: UUID, doneByBackOfficeUserName: String) = ???

  def rejectCardApplication(applicationId: UUID, reason: String, doneByBackOfficeUserName: String) = ???

  def getCardApplications(dateFrom: Option[LocalDate], dateTo: Option[LocalDate]) = ???

  def getCardDetails(cardNumber: String) = ???

  def resetCardPin(cardNumber: String, newPin: String) = ???

  def blockCard(cardNumber: String, doneByBackOfficeUserName: String) = ???

  def unblockCard(cardNumber: String, doneByBackOfficeUserName: String) = ???

  def activateCard(cardNumber: String, doneByBackOfficeUserName: String) = ???

  def cancelCard(cardNumber: String, reason: String, doneByBackOfficeUserName: String) = ???

}
