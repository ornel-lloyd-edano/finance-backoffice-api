package tech.pegb.backoffice.domain.customer.implementation

import java.time.LocalDate
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.customer.abstraction._
import tech.pegb.backoffice.dao.model.GroupingField
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.customer.abstraction.CustomerRead
import tech.pegb.backoffice.domain.customer.dto.{GenericUserCriteria, IndividualUserCriteria}
import tech.pegb.backoffice.domain.customer.model.BusinessUsers.{ActivatedBusinessUser, RegisteredButNotActivatedBusinessUser}
import tech.pegb.backoffice.domain.customer.model.{CustomerAttributes, GenericUser}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.IndividualUser
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.customer.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.domain.model.{CustomerAggregation, Ordering}
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits.TransactionCriteriaConverterNew
import tech.pegb.backoffice.mapping.dao.domain.customer.dto.Implicits._

import scala.concurrent.Future
import scala.util._

class CustomerReadService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    businessUserDao: BusinessUserDao,
    individualUserDao: IndividualUserDao,
    accountDao: AccountDao,
    customerExtraAttributeDao: CustomerExtraAttributeDao,
    customerDao: CustomerAttributesDao) extends CustomerRead with BaseService {

  implicit val executionContext = executionContexts.blockingIoOperations

  def getIndividualUser(customerId: UUID): Future[ServiceResponse[IndividualUser]] = Future {
    individualUserDao.getIndividualUser(customerId.toString).fold(
      _.asDomainError.toLeft,
      {
        case Some(result) ⇒
          result.asDomain match {
            case Success(result) ⇒ Right(result)
            case Failure(error) ⇒
              logger.error("Error in CustomerReadService.getIndividualUser", error)
              Left(dtoMappingError(s"IndividualUser ${customerId.toString} could not be converted to domain object"))
          }
        case None ⇒ Left(notFoundError(s"IndividualUser ${customerId.toString} was not found"))
      })
  }

  def getBusinessUserSubmittedActivationRequirements(userId: UUID): Future[ServiceResponse[Set[ActivationRequirement]]] = Future {
    getBusinessUserSubmittedActivationRequirements(userId.toString).left.map(_.asDomainError)
  }

  override def findIndividualUsersByCriteria(
    individualUserCriteria: IndividualUserCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): FSR[Seq[IndividualUser]] = Future {
    val daoCriteria = individualUserCriteria.asDao
    for {
      searchResp ← individualUserDao.getIndividualUsersByCriteria(daoCriteria, orderBy.map(_.asDao), limit, offset).asServiceResponse
    } yield {
      searchResp
        .foldLeft(Seq.empty[IndividualUser]) { (acc, u) ⇒
          u.asDomain.fold(
            exc ⇒ { logger.warn(s"Failed to convert ${u.uuid} to domain entity", exc); acc },
            du ⇒ acc :+ du)
        }
    }
  }

  override def aggregateCustomersByCriteriaAndPivots(
    criteria: IndividualUserCriteria,
    trxCriteria: TransactionCriteria,
    groupings: Seq[GroupingField]): Future[ServiceResponse[Seq[CustomerAggregation]]] = {
    Future {
      individualUserDao
        .aggregateCustomersByCriteriaAndPivots(criteria.asDao, trxCriteria.asDao(), groupings)
        .map(_.flatMap(_.asDomain.toOption))
        .asServiceResponse
    }
  }

  def countIndividualUsersByCriteria(individualUserCriteria: IndividualUserCriteria): Future[ServiceResponse[Int]] = Future {
    individualUserDao.countIndividualUserByCriteria(individualUserCriteria.asDao).left.map(_.asDomainError)
  }

  private def getBusinessUserSubmittedActivationRequirements(userId: String): Either[DaoError, Set[ActivationRequirement]] = {

    val maybeCustomerStatus = customerDao.getCustomerStatuses.right.map(_.find(_.statusName === conf.ActivatedBusinessUserStatus))

    val extraAttributeTypes = maybeCustomerStatus.fold(
      daoError ⇒ Left(daoError),
      {
        case Some(customerStatus) ⇒
          customerExtraAttributeDao.getExtraAttributesRequiredByCustomerStatus(customerStatus.statusName)
        case _ ⇒ Left(DaoError.EntityNotFoundError(s"Customer status ${conf.ActivatedBusinessUserStatus} could not be found"))
      })

    extraAttributeTypes.map(_.flatMap {
      attributeType ⇒

        val attributeSeq = customerExtraAttributeDao.getBusinessUserExtraAttributesByAttribute(userId.toString, attributeType.attributeName)
          .fold(
            daoError ⇒ Nil,
            attributeSeq ⇒ attributeSeq.map { userAttr ⇒
              ActivationRequirement(
                identifier = userAttr.attributeValue,
                documentType = ActivationDocumentType(userAttr.extraAttributeName),
                verifiedBy = userAttr.createdBy,
                verifiedAt = userAttr.createdAt)
            })

        attributeSeq
    })
  }

  def getActivatedBusinessUserById(id: UUID): Future[ServiceResponse[ActivatedBusinessUser]] = ???

  def getWaitingForActivationBusinessUserById(id: UUID): Future[ServiceResponse[RegisteredButNotActivatedBusinessUser]] = ???

  def getActivatedBusinessUserByName(username: CustomerAttributes.LoginUsername) = {
    Future.successful(Left(serviceUnimplementedError("Get BusinessUser by username is not yet supported")))
  }

  def getWaitingForActivationBusinessUserByName(username: CustomerAttributes.LoginUsername) = {
    Future.successful(Left(serviceUnimplementedError("Get waiting for activationBusinessUser by username is not yet supported")))
  }

  def findWaitingForActivationBusinessUsersByCriteria(
    createdDateFrom: Option[LocalDate],
    createdDateTo: Option[LocalDate],
    createdByBackofficeUserId: Option[UUID],
    company: Option[CustomerAttributes.NameAttribute],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[RegisteredButNotActivatedBusinessUser]]] = ???

  def countWaitingForActivationBusinessUsersByCriteria(
    createdDateFrom: Option[LocalDate],
    createdDateTo: Option[LocalDate],
    createdByBackOfficeUserId: Option[UUID],
    company: Option[CustomerAttributes.NameAttribute]): Future[ServiceResponse[Int]] = ???

  def findActivatedBusinessUsersByCriteria(
    createdDateFrom: Option[LocalDate],
    createdDateTo: Option[LocalDate],
    createdByBackofficeUserId: Option[UUID],
    company: Option[CustomerAttributes.NameAttribute],
    tier: Option[CustomerAttributes.CustomerTier],
    segment: Option[CustomerAttributes.CustomerSegment],
    subscription: Option[CustomerAttributes.CustomerSubscription],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[ActivatedBusinessUser]]] = ???

  def countActivatedBusinessUsersByCriteria(
    createdDateFrom: Option[LocalDate],
    createdDateTo: Option[LocalDate],
    createdByBackOfficeUserId: Option[UUID],
    company: Option[CustomerAttributes.NameAttribute],
    tier: Option[CustomerAttributes.CustomerTier],
    segment: Option[CustomerAttributes.CustomerSegment],
    subscription: Option[CustomerAttributes.CustomerSubscription]): Future[ServiceResponse[Int]] = ???

  def getUserByCriteria(
    criteria: GenericUserCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[GenericUser]]] = Future {
    userDao.getUserByCriteria(
      criteria.asDao,
      orderBy.asDao,
      limit,
      offset).map(_.flatMap(_.asDomain.toOption)).asServiceResponse
  }

  def countUserByCriteria(criteria: GenericUserCriteria): Future[ServiceResponse[Int]] = Future {
    userDao.countUserByCriteria(criteria.asDao).asServiceResponse
  }

  def getUser(customerId: UUID): Future[ServiceResponse[GenericUser]] = {
    (for {
      customerSeq ← EitherT.fromEither[Future](
        userDao.getUserByCriteria(
          GenericUserCriteria(userId = UUIDLike(customerId.toString).some).asDao, None, None, None).asServiceResponse)
      customer ← EitherT.fromOption[Future](customerSeq.headOption, notFoundError(s"Customer ${customerId.toString} was not found"))
      customerDomain ← EitherT.fromEither[Future](customer.asDomain.toEither
        .leftMap(t ⇒ dtoMappingError(s"Failed to parse customer entity")))
    } yield {
      customerDomain
    }).value
  }

  def validateUser(userId: UUID, customerType: CustomerType): Future[ServiceResponse[GenericUser]] = {
    (for {
      user ← {
        logger.debug(s"[validateUser] >>> check if user found, uuid = $userId")
        EitherT(getUser(userId))
      }
      _ ← {
        logger.debug(s"[validateUser] >>> check if user is $customerType, uuid - $userId")
        EitherT.cond[Future](user.customerType.contains(customerType), user, validationError(s"User $userId is not a business user"))
      }
      _ ← {
        logger.debug(s"[validateUser] >>> check if user is active, uuid - $userId")
        EitherT.cond[Future](user.status.contains(CustomerStatus("active")), user, validationError(s"User $userId is not active"))
      }
    } yield {
      user
    }).value
  }
}
