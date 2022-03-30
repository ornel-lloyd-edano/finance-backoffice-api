package tech.pegb.backoffice.domain.customer.implementation

import java.util.UUID

import cats.data.EitherT
import cats.implicits.{none, _}
import com.google.inject.Inject
import tech.pegb.backoffice.core.integration.abstraction.BusinessUserCoreApiClient
import tech.pegb.backoffice.dao.address.abstraction.AddressDao
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.CountryDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.domain.customer.abstraction.{AddressManagement, CustomerRead}
import tech.pegb.backoffice.domain.customer.dto.{AddressCriteria, ContactAddressToCreate, ContactAddressToUpdate}
import tech.pegb.backoffice.domain.customer.model.ContactAddress
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerType
import tech.pegb.backoffice.domain.{BaseService, ServiceError, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.customer.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts

import scala.concurrent.Future
import scala.util.Try

class AddressMgmtService @Inject() (
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    addressDao: AddressDao,
    countryDao: CountryDao,
    coreApiClient: BusinessUserCoreApiClient,
    customerRead: CustomerRead)
  extends AddressManagement with BaseService {

  implicit val ec = executionContexts.blockingIoOperations

  val BusinessUserType = CustomerType("business")

  //Address
  def getAddresses(
    userId: UUID,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[ContactAddress]]] = {
    (for {
      _ ← {
        logger.debug(s"[getAddresses] >>> validating user...")
        EitherT(customerRead.validateUser(userId, BusinessUserType))
      }
      addresses ← {
        logger.debug(s"[getAddresses] >>> fetching addresses for business_user, uuid - $userId")
        EitherT.fromEither[Future](addressDao.getByCriteria(
          AddressCriteria(userUuid = userId.some).asDao,
          ordering.asDao,
          limit,
          offset)(None).asServiceResponse)
      }
      domainEntities ← {
        logger.debug(s"[getAddresses] >>> converting contacts to domain entities, uuid - $userId")
        EitherT.fromEither[Future](addresses.map(_.asDomain).toList.sequence[Try, ContactAddress].toEither)
          .leftMap(t ⇒ {
            logger.error("[getAddresses] >>> exception encountered when mapping to domain", t)
            validationError(s"Error encountered on mapping dao address model to domain")
          })
      }

    } yield {
      domainEntities
    }).value
  }

  def getAddressById(userId: UUID, addressId: UUID): Future[ServiceResponse[ContactAddress]] = {
    (for {
      _ ← {
        logger.debug(s"[getAddressById] >>> validating user...")
        EitherT(customerRead.validateUser(userId, BusinessUserType))
      }
      getContactsResult ← {
        logger.debug(s"[getAddressById] >>> fetching address uuid - $addressId")
        EitherT.fromEither[Future](addressDao.getByCriteria(
          AddressCriteria(
            uuid = addressId.some,
            userUuid = userId.some).asDao, None, None, None)(None).asServiceResponse)
      }
      contact ← EitherT.fromOption[Future](getContactsResult.headOption, notFoundError(s"ContactAddress $addressId not found."))
      domainEntities ← {
        logger.debug(s"[getAddressById] >>> converting contacts to domain entities, uuid - $addressId")
        EitherT.fromEither[Future](contact.asDomain.toEither
          .leftMap(t ⇒ {
            logger.error("[getAddressById] >>> exception encountered when mapping to domain", t)
            validationError(s"Error encountered on mapping dao address model to domain")
          }))
      }
    } yield {
      domainEntities
    }).value
  }

  def insertAddress(addressToCreate: ContactAddressToCreate): Future[ServiceResponse[ContactAddress]] = {
    (for {
      _ ← {
        logger.debug(s"[insertAddress] >>> validating user...")
        EitherT(customerRead.validateUser(addressToCreate.userUuid, BusinessUserType))
      }
      address ← {
        logger.debug(s"[insertAddress] >>> calling dao...")
        EitherT.fromEither[Future](addressDao.insert(addressToCreate.asDao)(None).asServiceResponse)
      }
      domainEntities ← {
        logger.debug(s"[insertAddress] >>> converting address to domain entities, uuid - ${address.uuid}")
        EitherT.fromEither[Future](address.asDomain.toEither
          .leftMap(t ⇒ {
            logger.error("[insertAddress] >>> exception encountered when mapping to domain", t)
            validationError(s"Error encountered on mapping dao address model to domain")
          }))
      }
    } yield {
      domainEntities
    }).value
  }

  def updateAddress(userId: UUID, addressId: UUID, addressToUpdate: ContactAddressToUpdate): Future[ServiceResponse[ContactAddress]] = {
    (for {
      _ ← {
        logger.debug(s"[updateAddress] >>> validating user...")
        EitherT(customerRead.validateUser(userId, BusinessUserType))
      }
      countries ← {
        logger.debug(s"[updateAddress] >>> fetching countries")
        EitherT.fromEither[Future](countryDao.getCountries.asServiceResponse).map(_.map(x ⇒ (x.name → x.id)).toMap)
      }
      countryIdOption ← addressToUpdate.country match {
        case Some(name) ⇒ EitherT.cond[Future](countries.get(name).isDefined, countries.get(name), validationError(s"Country $name is not supported"))
        case None ⇒ EitherT.fromEither[Future](none[Int].asRight[ServiceError])
      }
      addressOption ← {
        logger.debug(s"[updateAddress] >>> calling dao...")
        EitherT.fromEither[Future](addressDao.update(
          addressId.toString,
          addressToUpdate.asDao(countryIdOption))(None).asServiceResponse)
      }
      address ← EitherT.fromOption[Future](addressOption, notFoundError(s"Address $addressId not found."))
      domainEntities ← {
        logger.debug(s"[updateAddress] >>> converting address to domain entities, uuid - ${address.uuid}")
        EitherT.fromEither[Future](address.asDomain.toEither
          .leftMap(t ⇒ {
            logger.error("[updateAddress] >>> exception encountered when mapping to domain", t)
            validationError(s"Error encountered on mapping dao address model to domain")
          }))
      }
    } yield {
      domainEntities
    }).value
  }

}
