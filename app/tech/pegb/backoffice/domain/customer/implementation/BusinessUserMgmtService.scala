package tech.pegb.backoffice.domain.customer.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.core.integration.abstraction.BusinessUserCoreApiClient
import tech.pegb.backoffice.dao.address.abstraction.AddressDao
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.CountryDao
import tech.pegb.backoffice.dao.contacts.abstraction.ContactsDao
import tech.pegb.backoffice.dao.customer.abstraction.{UserDao, VelocityPortalUserDao}
import tech.pegb.backoffice.dao.customer.dto.VelocityPortalUsersCriteria
import tech.pegb.backoffice.domain.customer.abstraction.{BusinessUserManagement, CustomerRead}
import tech.pegb.backoffice.domain.customer.dto._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerType
import tech.pegb.backoffice.domain.customer.model.VelocityPortalUser
import tech.pegb.backoffice.domain.{BaseService, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.customer.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts

import scala.concurrent.Future
import scala.util.Try

class BusinessUserMgmtService @Inject() (
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    contactsDao: ContactsDao,
    addressDao: AddressDao,
    velocityPortalUserDao: VelocityPortalUserDao,
    countryDao: CountryDao,
    coreApiClient: BusinessUserCoreApiClient,
    customerRead: CustomerRead)
  extends BusinessUserManagement with BaseService {

  implicit val ec = executionContexts.blockingIoOperations

  val BusinessUserType = CustomerType("business")

  //Txn Config
  def getTxnConfig(userId: UUID): Future[ServiceResponse[_]] = ???

  def countTxnConfig(userId: UUID): Future[ServiceResponse[Int]] = ???

  //VP Users
  def getVelocityUsers(
    userId: UUID,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[VelocityPortalUser]]] = {
    (for {
      user ← {
        logger.debug(s"[getVelocityUsers] >>> validating user...")
        EitherT(customerRead.validateUser(userId, BusinessUserType))
      }
      vpUsers ← {
        logger.debug(s"[getVelocityUsers] >>> fetching velocity users for business_user, uuid - $userId")
        EitherT.fromEither[Future](velocityPortalUserDao.getVelocityPortalUsersByCriteria(
          user.dbUserId.userIdToDaoCriteria[VelocityPortalUsersCriteria],
          ordering.asDao,
          limit,
          offset).asServiceResponse)
      }
      domainEntities ← {
        logger.debug(s"[getVelocityUsers] >>> converting vp users to domain entities, uuid - $userId")
        EitherT.fromEither[Future](vpUsers.map(_.asDomain).toList.sequence[Try, VelocityPortalUser].toEither)
          .leftMap(t ⇒ {
            logger.error("[getVelocityUsers] >>> exception encountered when mapping to domain", t)
            validationError(s"Error encountered on mapping dao velocity portal user model to domain")
          })
      }

    } yield {
      domainEntities
    }).value
  }

  def countVelocityUsers(userId: UUID): Future[ServiceResponse[Int]] = {
    (for {
      user ← {
        logger.debug(s"[countVelocityUsers] >>> validating user...")
        EitherT(customerRead.validateUser(userId, BusinessUserType))
      }
      count ← {
        logger.debug(s"[countVelocityUsers] >>> fetching velocity users count for business_user, uuid - $userId")
        EitherT.fromEither[Future](velocityPortalUserDao.countVelocityPortalUserByCriteria(user.dbUserId.userIdToDaoCriteria[VelocityPortalUsersCriteria]).asServiceResponse)
      }
    } yield {
      count
    }).value
  }

  def getVelocityUsersById(userId: UUID, vpUserId: UUID): Future[ServiceResponse[VelocityPortalUser]] = {
    (for {
      _ ← {
        logger.debug(s"[getVelocityUsersById] >>> validating user...")
        EitherT(customerRead.validateUser(userId, BusinessUserType))
      }
      getVpUserResult ← {
        logger.debug(s"[getVelocityUsersById] >>> fetching velocity users for business_user, uuid - $userId")
        EitherT.fromEither[Future](velocityPortalUserDao.getVelocityPortalUsersByCriteria(
          vpUserId.asDao[VelocityPortalUsersCriteria], None, None, None).asServiceResponse)
      }
      vpUser ← EitherT.fromOption[Future](getVpUserResult.headOption, notFoundError(s"Velocity Portal $vpUserId not found."))
      domainEntities ← {
        logger.debug(s"[getVelocityUsersById] >>> converting vp user to domain entity, vpUserId - $vpUserId")
        EitherT.fromEither[Future](vpUser.asDomain.toEither.leftMap(t ⇒ {
          logger.error("[getVelocityUsersById] >>> exception encountered when mapping to domain", t)
          validationError(s"Error encountered on mapping dao velocity portal user model to domain")
        }))
      }
    } yield {
      domainEntities
    }).value
  }

  def resetVelocityUserPin(
    userId: UUID,
    vpUserId: UUID,
    reason: String,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    (for {
      user ← {
        logger.debug(s"[resetVelocityUserPin] >>> validating user...")
        EitherT(customerRead.validateUser(userId, BusinessUserType))
      }
      getVpUserResult ← {
        logger.debug(s"[resetVelocityUserPin] >>> fetching velocity users for business_user, uuid - $userId")
        EitherT.fromEither[Future](velocityPortalUserDao.getVelocityPortalUsersByCriteria(
          vpUserId.asDao[VelocityPortalUsersCriteria], None, None, None).asServiceResponse)
      }
      vpUser ← EitherT.fromOption[Future](getVpUserResult.headOption, notFoundError(s"Velocity Portal $vpUserId not found."))
      _ ← EitherT.cond[Future](vpUser.status == "active", (), validationError(s"Velocity Portal $vpUserId is not active."))
      getContactResut ← {
        logger.debug(s"[resetVelocityUserPin] >>> fetching contacts matching Velocity Portal user - $vpUserId")
        EitherT.fromEither[Future](contactsDao.getByCriteria(
          ContactsCriteria(vpUserId = vpUser.id.some).asDao, None, None, None)(None).asServiceResponse)
      }
      contact ← EitherT.fromOption[Future](getContactResut.headOption, notFoundError(s"Contact for Velocity Portal $vpUserId not found."))
      _ ← EitherT.cond[Future](contact.isActive, (), validationError(s"Contact for Velocity Portal $vpUserId is not active."))
      _ ← EitherT(coreApiClient.resetVelocityPortalUserPin(vpUser.id, reason, updatedBy, lastUpdatedAt))
    } yield {
      ()
    }).value
  }

  //Documents
  def getDocuments(userId: UUID): Future[ServiceResponse[_]] = ???

  def countDocuments(userId: UUID): Future[ServiceResponse[Int]] = ???

}
