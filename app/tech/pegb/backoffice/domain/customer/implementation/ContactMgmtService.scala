package tech.pegb.backoffice.domain.customer.implementation

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.core.integration.abstraction.BusinessUserCoreApiClient
import tech.pegb.backoffice.dao.contacts.abstraction.ContactsDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.domain.{BaseService, model}
import tech.pegb.backoffice.domain.customer.abstraction.{ContactManagement, CustomerRead}
import tech.pegb.backoffice.domain.customer.dto.{ContactToCreate, ContactToUpdate, ContactsCriteria}
import tech.pegb.backoffice.domain.customer.model.Contact
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerType
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.customer.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts

import scala.concurrent.Future
import scala.util.Try

class ContactMgmtService @Inject() (
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    contactsDao: ContactsDao,
    coreApiClient: BusinessUserCoreApiClient,
    customerRead: CustomerRead)
  extends ContactManagement with BaseService {

  implicit val ec = executionContexts.blockingIoOperations

  val BusinessUserType = CustomerType("business")

  //Contacts
  def getContactInfo(
    userId: UUID,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Contact]]] = {
    (for {
      _ ← {
        logger.debug(s"[getContactInfo] >>> validating user...")
        EitherT(customerRead.validateUser(userId, BusinessUserType))
      }
      contacts ← {
        logger.debug(s"[getContactInfo] >>> fetching contacts for business_user, uuid - $userId")
        EitherT.fromEither[Future](contactsDao.getByCriteria(
          ContactsCriteria(userUuid = userId.some).asDao,
          ordering.asDao,
          limit,
          offset)(None).asServiceResponse)
      }
      domainEntities ← {
        logger.debug(s"[getContactInfo] >>> converting contacts to domain entities, uuid - $userId")
        EitherT.fromEither[Future](contacts.map(_.asDomain).toList.sequence[Try, Contact].toEither)
          .leftMap(t ⇒ {
            logger.error("[getContactInfo] >>> exception encountered when mapping to domain", t)
            validationError(s"Error encountered on mapping dao contact model to domain")
          })
      }

    } yield {
      domainEntities
    }).value
  }

  def getContactInfoById(userId: UUID, contactId: UUID): Future[ServiceResponse[Contact]] = {
    (for {
      _ ← {
        logger.debug(s"[getContactInfoById] >>> validating user...")
        EitherT(customerRead.validateUser(userId, BusinessUserType))
      }
      getContactsResult ← {
        logger.debug(s"[getContactInfoById] >>> fetching contacts uuid - $contactId")
        EitherT.fromEither[Future](contactsDao.getByCriteria(
          ContactsCriteria(
            uuid = contactId.some,
            userUuid = userId.some).asDao, None, None, None)(None).asServiceResponse)
      }
      contact ← EitherT.fromOption[Future](getContactsResult.headOption, notFoundError(s"Contact $contactId not found."))
      domainEntities ← {
        logger.debug(s"[getContactInfoById] >>> converting contacts to domain entities, uuid - $contactId")
        EitherT.fromEither[Future](contact.asDomain.toEither
          .leftMap(t ⇒ {
            logger.error("[getContactInfo] >>> exception encountered when mapping to domain", t)
            validationError(s"Error encountered on mapping dao contact model to domain")
          }))
      }
    } yield {
      domainEntities
    }).value
  }

  def insertContactInfo(contactToCreate: ContactToCreate): Future[ServiceResponse[Contact]] = {
    (for {
      _ ← {
        logger.debug(s"[insertContactInfo] >>> validating user...")
        EitherT(customerRead.validateUser(contactToCreate.userUuid, BusinessUserType))
      }
      contact ← {
        logger.debug(s"[insertContactInfo] >>> calling dao...")
        EitherT.fromEither[Future](contactsDao.insert(contactToCreate.asDao)(None).asServiceResponse)
      }
      domainEntities ← {
        logger.debug(s"[insertContactInfo] >>> converting contacts to domain entities, uuid - ${contact.uuid}")
        EitherT.fromEither[Future](contact.asDomain.toEither
          .leftMap(t ⇒ {
            logger.error("[insertContactInfo] >>> exception encountered when mapping to domain", t)
            validationError(s"Error encountered on mapping dao contact model to domain")
          }))
      }
    } yield {
      domainEntities
    }).value
  }

  def updateContactInfo(userId: UUID, contactId: UUID, contactToUpdate: ContactToUpdate): Future[ServiceResponse[Contact]] = {
    (for {
      getContactResult ← {
        logger.debug(s"[updateContactInfo] >>> checking contact if exist...")
        EitherT(getContactInfoById(userId, contactId))
      }
      contactOption ← {
        val updateDto = contactToUpdate.lastUpdatedAt.fold(contactToUpdate.copy(lastUpdatedAt = getContactResult.updatedAt))(_ ⇒ contactToUpdate)
        logger.debug(s"[updateContactInfo] >>> calling dao...")
        EitherT.fromEither[Future](contactsDao.update(contactId.toString, updateDto.asDao)(None).asServiceResponse)
      }
      contact ← EitherT.fromOption[Future](contactOption, notFoundError(s"Contact $contactId not found."))
      domainEntities ← {
        logger.debug(s"[updateContactInfo] >>> converting contacts to domain entities, uuid - ${contact.uuid}")
        EitherT.fromEither[Future](contact.asDomain.toEither
          .leftMap(t ⇒ {
            logger.error("[updateContactInfo] >>> exception encountered when mapping to domain", t)
            validationError(s"Error encountered on mapping dao contact model to domain")
          }))
      }
    } yield {
      domainEntities
    }).value
  }

}
