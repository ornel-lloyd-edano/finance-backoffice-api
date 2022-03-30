package tech.pegb.backoffice.domain.customer.implementation

import java.time.{Clock, LocalDateTime}
import java.util.UUID

import cats.data.OptionT
import cats.instances.either._
import cats.syntax.either._
import com.google.inject.Inject
import tech.pegb.backoffice.application.CommunicationService
import tech.pegb.backoffice.application.model.NotificationMessage
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.customer.abstraction.{BusinessUserDao, CustomerExtraAttributeDao, IndividualUserDao, UserDao}
import tech.pegb.backoffice.dao.customer.entity.User
import tech.pegb.backoffice.dao.customer.dto._
import tech.pegb.backoffice.domain.customer.abstraction.{CustomerActivation, CustomerRead}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.ActivationDocumentType
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future
import tech.pegb.backoffice.domain.account.abstraction.AccountManagement
import tech.pegb.backoffice.domain.{BaseService, ServiceError}
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.error._
import tech.pegb.backoffice.domain.customer.model.BusinessUsers.{ActivatedBusinessUser, RegisteredButNotActivatedBusinessUser}
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.IndividualUser
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._

class CustomerActivationService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    businessUserDao: BusinessUserDao,
    individualUserDao: IndividualUserDao,
    userDao: UserDao,
    accountDao: AccountDao,
    customerExtraAttribDao: CustomerExtraAttributeDao,
    customerReadService: CustomerRead,
    accountManagement: AccountManagement,
    notificationService: CommunicationService) extends CustomerActivation with BaseService {

  implicit val executionContext = executionContexts.blockingIoOperations

  val clock = Clock.systemDefaultZone()

  def getBusinessUserActivationRequirements: Future[ServiceResponse[Set[ActivationDocumentType]]] = Future {

    //TODO confirm id method required statusId
    customerExtraAttribDao.getExtraAttributesRequiredByCustomerStatus(conf.ActivatedBusinessUserStatus)
      .right.map(_.map(extraAttrib ⇒ ActivationDocumentType(extraAttrib.attributeName))).left.map(_.asDomainError)

  }

  def activateBusinessUserOnTheFly(
    userId: UUID,
    submittedDocuments: Seq[CustomerAttributes.ActivationRequirement],
    doneByBackOfficeUserName: String,
    doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]] = ???

  def activateBusinessUser(userId: UUID, doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[ActivatedBusinessUser]] = ???

  def deactivateBusinessUser(userId: UUID, reason: String, doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[RegisteredButNotActivatedBusinessUser]] = Future {
    userDao.getUser(userId.toString) match {
      case Right(Some(foundUser)) if !foundUser.status.contains(conf.NotYetActivatedBusinessUserStatus) ⇒
        userDao.updateUser(
          userId.toString,
          UserToUpdate.getEmpty.copy(
            status = Some(conf.NotYetActivatedBusinessUserStatus),
            updatedBy = doneByBackOfficeUserName,
            updatedAt = doneAt)) match {
            case Right(updatedUser) ⇒ Right(updatedUser)
            case Left(daoError) ⇒ daoError.asDomainError.toLeft
          }
      case Right(Some(foundUser)) ⇒
        Left(CustomerAlreadyDeactivated(foundUser))

      case Right(None) ⇒ Left(notFoundError(s"BusinessUser with id $userId was not found"))

      case Left(daoError) ⇒ daoError.asDomainError.toLeft
    }
  }.flatMap {
    case Right(_) ⇒ customerReadService.getWaitingForActivationBusinessUserById(userId)
    case Left(serviceError) ⇒ Future.successful(Left(serviceError))
  }

  protected def notifyBusinessUserForActivation(user: ActivatedBusinessUser): Future[ServiceResponse[NotificationMessage]] = Future {
    val maybePrimaryEmail: Option[Email] = user.emails.find(_.isPrimary == true).orElse(user.emails.headOption)
    maybePrimaryEmail match {
      case Some(email) ⇒
        val message = conf.ActivationEmailTemplate
          .replace("#name", user.name.underlying.toUpperCase)
          .replace("#subscription", user.subscription.underlying.toUpperCase)
          .replace("#segment", user.segment.map(_.underlying).getOrElse("NEW"))
          .replace("#tier", user.tier.underlying.toUpperCase)

        logger.info(message)

        notificationService.notify(email, "Customer Activation", message)
      case None ⇒
        Left(notFoundError("Unable to notify customer for activation. Email is missing"))
    }
  }

  def notifyBusinessUserForActivation(userId: UUID): Future[ServiceResponse[NotificationMessage]] = Future {
    userDao.getUser(userId.toString) match {
      case Right(Some(foundUser)) if foundUser.status.contains(conf.ActivatedBusinessUserStatus) ⇒
        Right(foundUser)
      case Right(Some(foundUser)) ⇒
        Left(CustomerActivationNotificationViolation(foundUser, reason = "Customer is not in active status"))

      case Right(None) ⇒ Left(notFoundError(s"BusinessUser with id $userId was not found"))

      case Left(daoError) ⇒ daoError.asDomainError.toLeft
    }
  }.flatMap {
    case Right(_) ⇒
      customerReadService.getActivatedBusinessUserById(userId).flatMap {
        case Right(activatedBusinessUser) ⇒ notifyBusinessUserForActivation(activatedBusinessUser)
        case Left(serviceError) ⇒ Future.successful(Left(serviceError))
      }
    case Left(serviceError) ⇒ Future.successful(Left(serviceError))
  }

  def activateIndividualUser(customerId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[IndividualUser]] = Future {
    userDao.getUser(customerId.toString)
      .leftMap(_.asDomainError)
      .flatMap { maybeUser ⇒
        for {
          foundUser ← maybeUser.toRight(notFoundError(s"IndividualUser with id $customerId was not found"))
          userToUpdate ← if (foundUser.status.exists(_.equalsIgnoreCase(conf.WaitingForActivationUserStatus))) {
            Right(UserToUpdate.getEmpty.copy(
              status = Some(conf.ActiveUserStatus),
              activatedAt = Some(doneAt),
              updatedBy = doneBy,
              updatedAt = doneAt))
          } else {
            Left(CustomerAlreadyActivated(foundUser))
          }
          updateResult ← userDao.updateUser(customerId.toString, userToUpdate)
            .fold(
              _.asDomainError.asLeft[User],
              _.toRight {
                notFoundError(s"Unable to update IndividualUser with id $customerId. User was not found")
              })
          getResult ← individualUserDao.getIndividualUser(updateResult.uuid.toString)
            .fold(
              _.asDomainError.asLeft[tech.pegb.backoffice.dao.customer.entity.IndividualUser],
              _.toRight {
                notFoundError(s"Unable to update IndividualUser with id $customerId. User was not found")
              })
          domainEntity ← getResult.asDomain.toEither.leftMap { exc ⇒
            val errorId = UUID.randomUUID()
            logger.error("Failed to convert IndividualUser from dao to domain", exc)
            ServiceError.dtoMappingError(exc.getMessage, errorId.toOption)
          }
        } yield domainEntity
      }
  }

  def deactivateIndividualUser(customerId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[IndividualUser]] = {
    userDao.getUser(customerId.toString) match {
      case Left(daoError) ⇒ daoError.asDomainError.toLeft.toFuture

      case Right(None) ⇒ Future.successful(Left(notFoundError(s"IndividualUser with id $customerId was not found")))

      case Right(Some(foundUser)) if !foundUser.status.contains(conf.ActiveUserStatus) ⇒ Future.successful(Left(CustomerAlreadyDeactivated(foundUser)))

      case Right(Some(foundUser)) ⇒
        accountDao.getAccountsByUserId(customerId.toString) match {
          case Left(daoError) ⇒ daoError.asDomainError.toLeft.toFuture
          case Right(accountSet) ⇒ {
            val seqFuture = Future.sequence(accountSet.filter(_.status !== conf.AccountCloseStatus).map(a ⇒ accountManagement.deleteAccount(UUID.fromString(a.uuid), doneBy, doneAt)))
            seqFuture.map { resultSet ⇒
              resultSet.partition(_.isLeft) match {
                case (failed, accts) if failed.isEmpty ⇒ {
                  val userToUpdate = UserToUpdate.getEmpty.copy(
                    status = Some(conf.PassiveUserStatus),
                    updatedBy = doneBy,
                    updatedAt = doneAt)
                  (for {
                    updateResult ← OptionT(userDao.updateUser(customerId.toString, userToUpdate))
                    getResult ← OptionT(individualUserDao.getIndividualUser(updateResult.uuid.toString))
                  } yield getResult.asDomain.toOption).value.map(_.flatten) match {
                    case Right(Some(updatedUser)) ⇒ Right(updatedUser)
                    case Right(None) ⇒ Left(notFoundError(s"Unable to update IndividualUser with id $customerId. User was not found"))
                    case Left(daoError) ⇒ daoError.asDomainError.toLeft
                  }
                }
                case (failed, _) ⇒ Left(failed.head.left.get)
              }
            }
          }
        }
    }
  }
}
