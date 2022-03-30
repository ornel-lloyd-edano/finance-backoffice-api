package tech.pegb.backoffice.domain.businessuserapplication.implementation

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.core.integration.abstraction.BusinessUserCoreApiClient
import tech.pegb.backoffice.dao.businessuserapplication.abstraction._
import tech.pegb.backoffice.dao.businessuserapplication.dto
import tech.pegb.backoffice.dao.businessuserapplication.dto.{AccountConfigToInsert, ExternalAccountToInsert, TransactionConfigToInsert}
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.customer.abstraction.BusinessUserDao
import tech.pegb.backoffice.dao.customer.dto.BusinessUserCriteria
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.Implicits._
import tech.pegb.backoffice.domain._
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.{BusinessUserApplicationManagement, Stages, Status}
import tech.pegb.backoffice.domain.businessuserapplication.dto.{BusinessUserApplicationCriteria, BusinessUserApplicationToCreate, BusinessUserApplicationToUpdateStage, BusinessUserApplicationToUpdateStatus}
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes._
import tech.pegb.backoffice.domain.businessuserapplication.model._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.dto.DocumentCriteria
import tech.pegb.backoffice.domain.document.model.Document
import tech.pegb.backoffice.domain.parameter.abstraction.ParameterManagement
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.businessuserapplication.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.businessuserapplication.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.types.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}

import scala.concurrent.Future
import scala.util._

@Singleton
class BusinessUserApplicationMgmtService @Inject() (
    executionContexts: WithExecutionContexts,
    applicationDao: BusinessUserApplicationDao,
    applicationConfigDao: BusinessUserApplicationConfigDao,
    applicContactsDao: BUApplicPrimaryContactsDao,
    applicAddressDao: BUAppPrimaryAddressesDao,
    documentService: DocumentManagement,
    countryDao: CountryDao,
    businessUserDao: BusinessUserDao,
    currencyDao: CurrencyDao,
    coreApiClient: BusinessUserCoreApiClient,
    parameterManagement: ParameterManagement,
    val typesDao: TypesDao)
  extends BusinessUserApplicationManagement with BaseService with FieldValueValidation {
  import ContactAddress._
  import ContactPerson._

  implicit val ec = executionContexts.blockingIoOperations

  typesDao.bulkUpsert(
    existingKind = BusinessUserTiers.toString,
    updatedAt = LocalDateTime.now(),
    updatedBy = "backoffice_api",
    lastUpdatedAt = None,
    newValues = BusinessUserTiers.toSeq.map(_.asDao),
    disableOptimisticLockCheck = true)

  typesDao.bulkUpsert(
    existingKind = BusinessTypes.toString,
    updatedAt = LocalDateTime.now(),
    updatedBy = "backoffice_api",
    lastUpdatedAt = None,
    newValues = BusinessTypes.toSeq.map(_.asDao),
    disableOptimisticLockCheck = true)

  private def getDefaultCurrency: Future[ServiceResponse[Currency]] = {
    (for {
      parameterOption ← EitherT(parameterManagement.getParameters).map(p ⇒ p.find(p ⇒ p.key == "default_currency"))
      parameter ← EitherT.fromOption[Future](parameterOption, validationError("System default_currency is not set"))
      value ← EitherT.fromEither[Future]((parameter.value \ "value").validate[String].asEither
        .leftMap(errorSeq ⇒ {
          logger.error(s"error encountered in [getDefaultCurrency] ${errorSeq}")
          validationError("Error parsing system default_currency")
        }))
      currency ← EitherT.fromEither[Future](Try(Currency.getInstance(value)).toEither.leftMap(t ⇒ {
        logger.error(s"error encountered converting to java currency", t)
        validationError("Error parsing system defaul_currency")
      }))
    } yield {
      currency
    }).value
  }

  def createBusinessUserApplication(dto: BusinessUserApplicationToCreate): Future[ServiceResponse[BusinessUserApplication]] = {
    (for {
      _ ← EitherT.fromEither[Future](dto.validate)
      _ ← EitherT(validateApplicationFields(
        applicationCriteria = dto.asApplicationCriteriaDao(dto.uuid.some),
        businessUserCriteria = dto.asBusinessUserCriteriaDao,
        businessName = dto.businessName,
        businessCategory = dto.businessCategory,
        userTier = dto.userTier,
        businessType = dto.businessType))
      getByCriteriaResult ← EitherT(
        getBusinessUserApplicationByCriteria(BusinessUserApplicationCriteria(uuid = UUIDLike(dto.uuid.toString).some), Nil, None, None))
        .map(_.headOption)
      application ← getByCriteriaResult match {
        case Some(existingApp) ⇒
          logger.info(s"Existing BusinessUserApplication Found. Calling updateBusinessUserApplication on uuid ${dto.uuid}")
          for {
            updateRes ← EitherT.fromEither[Future](applicationDao.updateBusinessUserApplication(dto.uuid.asEntityId, dto.asUpdateDao(existingApp.updatedAt)).asServiceResponse)
            updated ← EitherT.fromOption[Future](updateRes, notFoundError("updated business user application not found"))
          } yield {
            updated
          }
        case None ⇒
          logger.info("No uuid in request. Calling insertBusinessUserApplication...")
          EitherT.fromEither[Future](applicationDao.insertBusinessUserApplication(dto.asDao).asServiceResponse)
      }
      defaultCurrency ← EitherT(getDefaultCurrency)
      domainDto ← EitherT.fromEither[Future](application.asDomain(defaultCurrency).toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert business_user_application entity to domain: ${t.getMessage}")))
    } yield {
      domainDto
    }).value
  }

  def createBusinessUserApplicationConfig(dto: businessuserapplication.dto.BusinessUserApplicationConfigToCreate): Future[ServiceResponse[BusinessUserApplication]] = {

    (for {
      buApplication ← EitherT(getBusinessUserApplicationById(dto.applicationUUID))
      currencies ← EitherT.fromEither[Future](currencyDao.getAll.asServiceResponse)
      currencyLookUp = currencies.map(c ⇒ (c.name → c.id)).toMap

      _ ← EitherT.fromEither[Future](dto.validateTransactionConfig(buApplication.getValidTransactionConfig))

      _ ← EitherT.fromEither[Future](dto.transactionConfig.map(x ⇒ validateFieldsFromKnownTypes(x.transactionType.underlying, "transaction_type")).toList.sequence[ServiceResponse, String])
      _ ← EitherT.fromEither[Future](dto.transactionConfig.map(x ⇒ Try(currencyLookUp.get(x.currency.getCurrencyCode).get)).toList.sequence[Try, Int].toEither)
        .leftMap(t ⇒ validationError(s"Unsupported currency found in transaction config"))
      _ ← EitherT.fromEither[Future](dto.accountConfig.map(x ⇒ Try(currencyLookUp.get(x.currency.getCurrencyCode).get)).toList.sequence[Try, Int].toEither)
        .leftMap(t ⇒ validationError(s"Unsupported currency found in account config"))
      _ ← EitherT.fromEither[Future](dto.externalAccounts.map(x ⇒ Try(currencyLookUp.get(x.currency.getCurrencyCode).get)).toList.sequence[Try, Int].toEither)
        .leftMap(t ⇒ validationError(s"Unsupported currency found in external accounts"))

      txnCofigToInsert ← EitherT.fromEither[Future](
        dto.transactionConfig.map(_.asDao(currencyLookUp)).toList.sequence[Try, TransactionConfigToInsert]
          .map(list ⇒ NonEmptyList(list.head, list.tail)).toEither
          .leftMap(t ⇒ {
            logger.error("[createBusinessUserApplicationConfig] Error encountered while transforming transaction config to dao entity", t)
            validationError(s"Error encountered while transforming transaction config to dao entity: ${dto.transactionConfig}")
          }))

      accountConfigToInsert ← EitherT.fromEither[Future](
        dto.accountConfig.map(_.asDao(currencyLookUp)).toList.sequence[Try, AccountConfigToInsert]
          .map(list ⇒ NonEmptyList(list.head, list.tail)).toEither
          .leftMap(t ⇒ {
            logger.error("[createBusinessUserApplicationConfig] Error encountered while transforming account config to dao entity", t)
            validationError(s"Error encountered while transforming account config to dao entity: ${dto.accountConfig}")
          }))

      externalAccountToInsert ← EitherT.fromEither[Future](
        dto.externalAccounts.map(_.asDao(currencyLookUp)).toList.sequence[Try, ExternalAccountToInsert]
          .map(list ⇒ NonEmptyList(list.head, list.tail)).toEither
          .leftMap(t ⇒ {
            logger.error("[createBusinessUserApplicationConfig] Error encountered while transforming external account to dao entity", t)
            validationError(s"Error encountered while transforming external account to dao entity: ${dto.externalAccounts}")
          }))

      txnConn ← EitherT.fromEither[Future](applicationConfigDao.startTransaction.asServiceResponse)

      _ ← EitherT.fromEither[Future](applicationConfigDao.deleteTxnConfig(buApplication.id)(txnConn.some).asServiceResponse)
      _ ← EitherT.fromEither[Future](applicationConfigDao.deleteAccountConfig(buApplication.id)(txnConn.some).asServiceResponse)
      _ ← EitherT.fromEither[Future](applicationConfigDao.deleteExternalAccount(buApplication.id)(txnConn.some).asServiceResponse)

      txnConfigCreateResult ← EitherT.fromEither[Future](applicationConfigDao.insertTxnConfig(
        buApplication.id,
        txnCofigToInsert,
        dto.createdBy,
        dto.createdAt)(txnConn.some).asServiceResponse)

      accountConfigCreateResult ← EitherT.fromEither[Future](applicationConfigDao.insertAccountConfig(
        buApplication.id,
        accountConfigToInsert,
        dto.createdBy,
        dto.createdAt)(txnConn.some).asServiceResponse)

      externalAccountCreateResult ← EitherT.fromEither[Future](applicationConfigDao.insertExternalAccount(
        buApplication.id,
        externalAccountToInsert,
        dto.createdBy,
        dto.createdAt)(txnConn.some).asServiceResponse)

      updateRes ← EitherT.fromEither[Future](applicationDao.updateBusinessUserApplication(
        dto.applicationUUID.asEntityId,
        BusinessUserApplicationToUpdateStage(
          stage = Stages.Config,
          updatedBy = dto.createdBy,
          updatedAt = dto.createdAt).asDao(buApplication.updatedAt))(txnConn.some).asServiceResponse)
      updated ← EitherT.fromOption[Future](updateRes, notFoundError("updated business user application not found"))
      defaultCurrency ← EitherT(getDefaultCurrency)
      domainDto ← EitherT.fromEither[Future](updated.asDomain(defaultCurrency).toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert business_user_application entity to domain: ${t.getMessage}")))

      _ ← EitherT.fromEither[Future](applicationConfigDao.endTransaction(txnConn).asServiceResponse)

    } yield {
      domainDto.addConfigs(
        transactionConfig = txnConfigCreateResult.flatMap(_.asDomain.toOption),
        accountConfig = accountConfigCreateResult.flatMap(_.asDomain.toOption),
        externalAccount = externalAccountCreateResult.flatMap(_.asDomain.toOption))
    }).value
  }

  def getBusinessUserApplicationById(uuid: UUID, stageDataIncluded: Seq[String] = Nil): Future[ServiceResponse[BusinessUserApplication]] = {
    (for {
      _ ← EitherT.fromEither[Future](if (!stageDataIncluded.containsOnly(Stages.toSeq))
        Left(validationError(s"1 or more business application stage is not known ${stageDataIncluded.defaultMkString}"))
      else Right(()))

      getByCriteriaResult ← EitherT(getBusinessUserApplicationByCriteria(BusinessUserApplicationCriteria(uuid = UUIDLike(uuid.toString).some), Nil, None, None))
      buUsrApplic ← EitherT.fromOption[Future](getByCriteriaResult.headOption, notFoundError(s"BusinessUserApplication $uuid is not found"))

      configs ← {
        if (stageDataIncluded.contains(Stages.Config)) {
          EitherT(getConfigs(buUsrApplic.id))
        } else {
          EitherT.fromEither[Future]((Seq.empty[TransactionConfig], Seq.empty[AccountConfig], Seq.empty[ExternalAccount]).asRight[ServiceError])
        }
      }

      contactInfo ← {
        if (stageDataIncluded.contains(Stages.Contact))
          EitherT(getContactInfo(buUsrApplic.id))
        else
          EitherT.fromEither[Future]((Seq.empty[ContactPerson], Seq.empty[ContactAddress]).asRight[ServiceError])
      }

      documents ← {
        if (stageDataIncluded.contains(Stages.Docs))
          EitherT(documentService.getDocumentsByCriteria(DocumentCriteria(businessApplicationId = Some(uuid)), Nil, None, None))
        else
          EitherT.fromEither[Future]((Seq.empty[Document]).asRight[ServiceError])
      }

    } yield {
      val completeBusinessUserApplic = buUsrApplic
        .addConfigs(configs._1, configs._2, configs._3)
        .addContactInfo(contactInfo._1, contactInfo._2)
        .addDocuments(documents)
      completeBusinessUserApplic.validate.left.foreach(err ⇒ {
        logger.warn(s"Inconsistent data found on business user application id [$uuid]. ${err.message}")
      })
      completeBusinessUserApplic

    }).value
  }

  private def getConfigs(applicationId: Int): Future[ServiceResponse[(Seq[TransactionConfig], Seq[AccountConfig], Seq[ExternalAccount])]] = {
    (for {
      transactionConfigs ← EitherT.fromEither[Future](applicationConfigDao.getTxnConfig(applicationId).asServiceResponse)
      accountConfigs ← EitherT.fromEither[Future](applicationConfigDao.getAccountConfig(applicationId).asServiceResponse)
      externalAccounts ← EitherT.fromEither[Future](applicationConfigDao.getExternalAccount(applicationId).asServiceResponse)
    } yield {
      (transactionConfigs.flatMap(_.asDomain.toOption), accountConfigs.flatMap(_.asDomain.toOption), externalAccounts.flatMap(_.asDomain.toOption))
    }).value
  }

  private def getContactInfo(applicationId: Int): Future[ServiceResponse[(Seq[ContactPerson], Seq[ContactAddress])]] = {
    (for {
      contactPersons ← EitherT.fromEither[Future](applicContactsDao.getByApplicationId(applicationId)
        .leftMap(_.asDomainError).map(c ⇒ c.map(_.asDomain)))

      countries ← EitherT.fromEither[Future](countryDao.getCountries.leftMap(_.asDomainError).map(_.map(c ⇒ c.id → c.name).toMap))

      addresses ← EitherT.fromEither[Future](applicAddressDao.getByApplicationId(applicationId)
        .leftMap(_.asDomainError).map(c ⇒ c.map(c ⇒ c.asDomain(countries.get(c.countryId).get))))
    } yield {
      (contactPersons, addresses)
    }).value
  }

  def getBusinessUserApplicationByCriteria(
    criteria: BusinessUserApplicationCriteria,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[BusinessUserApplication]]] = {
    (for {
      defaultCurrency ← EitherT(getDefaultCurrency)
      applications ← EitherT.fromEither[Future](applicationDao.getBusinessUserApplicationByCriteria(
        criteria.asDao,
        ordering.asDao,
        limit,
        offset).map(_.flatMap(_.asDomain(defaultCurrency).toOption)).asServiceResponse)
    } yield {
      applications
    }).value
  }

  def countBusinessUserApplicationByCriteria(criteria: BusinessUserApplicationCriteria): Future[ServiceResponse[Int]] = Future {
    applicationDao.countBusinessUserApplicationByCriteria(criteria.asDao).asServiceResponse
  }

  def submitBusinessUserApplication(
    applicationId: UUID,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    (for {
      application ← EitherT(getBusinessUserApplicationById(applicationId, Seq(Stages.Config)))
      _ ← EitherT.fromEither[Future](application.validateNewStatus(Status.Pending))
      _ ← EitherT.fromEither[Future](BusinessUserApplication.validateTransactionConfig(application.transactionConfig, application.getValidTransactionConfig))
      res ← EitherT(updateApplicationStatus(
        applicationId,
        BusinessUserApplicationToUpdateStatus(
          status = Status.Pending,
          submittedAt = updatedAt.some,
          submittedBy = updatedBy.some,
          updatedAt = updatedAt,
          updatedBy = updatedBy),
        lastUpdatedAt = lastUpdatedAt))
    } yield {
      res
    }).value
  }

  def approveBusinessUserApplication(
    applicationId: UUID,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    (for {
      application ← {
        logger.debug(">>>> [approveBusinessUserApplication] step 1: check if application exists")
        EitherT(getBusinessUserApplicationById(applicationId))
      }
      _ ← {
        logger.debug(">>>> [approveBusinessUserApplication] step 2: check application status")
        EitherT.fromEither[Future](application.validateNewStatus(Status.Approved))
      }
      _ ← {
        logger.debug(">>>> [approveBusinessUserApplication] step 3: validate checker of application")
        EitherT.fromEither[Future](application.validateChecker(updatedBy))
      }
      _ ← {
        logger.debug(">>>> [approveBusinessUserApplication] step 4: call core for business user creation")
        EitherT(coreApiClient.createBusinessUserApplication(application.id, updatedBy))
      }
      documentIds ← {
        logger.debug(">>>> [approveBusinessUserApplication] step 5: get all documents linked to this bu application id")
        val dto = DocumentCriteria(businessApplicationId = applicationId.some)
        EitherT(documentService.getDocumentsByCriteria(dto, Nil, None, None)).map(_.map(_.id))
          .leftMap(error ⇒ {
            logger.warn(s"Failed to get all documents linked to this business user application with id [$applicationId] but unable to rollback core. Fix this later with batch job.")
            partialSuccessError("Failed to get all documents linked to this business user application")
          })
      }

      _ ← {
        logger.debug(">>>> [approveBusinessUserApplication] step 6: transfer doc file from couchbase to hdfs")
        val result = Future.sequence(documentIds.map(documentService.persistDocument(_, updatedBy, updatedAt)))
          .map(_ ⇒ Right(()))
          .recover {
            case error ⇒
              logger.warn(s"Failed to complete HDFS transfer of all documents linked to this business user application with id [$applicationId] but unable to rollback core. Fix this later with batch job.", error)
              Left(partialSuccessError("Failed to transfer all documents to HDFS"))
          }
        EitherT(result)
      }
    } yield {
      ()
    }).fold({
      case ServiceError.PartialSuccess(id, code, message) ⇒ Right(())
      case other ⇒ Left(other)
    }, _.toRight)
  }

  def cancelBusinessUserApplication(
    applicationId: UUID,
    explanation: String,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {

    (for {
      application ← EitherT(getBusinessUserApplicationById(applicationId))
      _ ← EitherT.fromEither[Future](application.validateNewStatus(Status.Cancelled))
      res ← EitherT(updateApplicationStatus(
        applicationId,
        BusinessUserApplicationToUpdateStatus(
          status = Status.Cancelled,
          explanation = explanation.some,
          updatedAt = updatedAt,
          updatedBy = updatedBy),
        lastUpdatedAt = lastUpdatedAt))
    } yield {
      res
    }).value

  }

  def rejectBusinessUserApplication(
    applicationId: UUID,
    explanation: String,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {

    (for {
      application ← EitherT(getBusinessUserApplicationById(applicationId))
      _ ← EitherT.fromEither[Future](application.validateNewStatus(Status.Rejected))
      _ ← EitherT.fromEither[Future](application.validateChecker(updatedBy))
      res ← EitherT(updateApplicationStatus(
        applicationId,
        BusinessUserApplicationToUpdateStatus(
          status = Status.Rejected,
          explanation = explanation.some,
          checkedAt = updatedAt.some,
          checkedBy = updatedBy.some,
          updatedAt = updatedAt,
          updatedBy = updatedBy),
        lastUpdatedAt = lastUpdatedAt))
    } yield {
      res
    }).value

  }

  def sendForCorrectionBusinessUserApplication(
    applicationId: UUID,
    explanation: String,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {

    (for {
      application ← EitherT(getBusinessUserApplicationById(applicationId))
      _ ← EitherT.fromEither[Future](application.validateNewStatus(Status.Ongoing))
      res ← EitherT(updateApplicationStatus(
        applicationId,
        BusinessUserApplicationToUpdateStatus(
          status = Status.Ongoing,
          explanation = explanation.some,
          updatedAt = updatedAt,
          updatedBy = updatedBy),
        lastUpdatedAt = lastUpdatedAt))
    } yield {
      res
    }).value
  }

  private def updateApplicationStatus(
    applicationId: UUID,
    statusUpdate: BusinessUserApplicationToUpdateStatus,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {

    (for {
      updateRes ← EitherT.fromEither[Future](applicationDao.updateBusinessUserApplication(
        applicationId.asEntityId,
        statusUpdate.asDao(lastUpdatedAt)).asServiceResponse)
      updated ← EitherT.fromOption[Future](updateRes, notFoundError("updated business user application not found"))
      defaultCurrency ← EitherT(getDefaultCurrency)
      _ ← EitherT.fromEither[Future](updated.asDomain(defaultCurrency).toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert business_user_application entity to domain: ${t.getMessage}")))
    } yield {
      ()
    }).value
  }

  def validateFieldsFromKnownTypes(fieldvalue: String, fieldName: String): ServiceResponse[String] = {
    val result = fieldName match {
      case "business_user_tiers" ⇒ typesDao.getBusinessUserTiers.map(_.find(_._2 == fieldvalue).map(_._2))
      case "business_types" ⇒ typesDao.getBusinessTypes.map(_.find(_._2 == fieldvalue).map(_._2))
      case "transaction_type" ⇒ typesDao.getTransactionTypes.map(_.find(_._2 == fieldvalue).map(_._2))
      case _ ⇒ Right(Option.empty[String])
    }

    result.asServiceResponse
      .flatMap(_.toRight(ServiceError.validationError(s"provided element `$fieldvalue` is invalid for '$fieldName'")))

  }

  private def validateApplicationFields(
    applicationCriteria: dto.BusinessUserApplicationCriteria,
    businessUserCriteria: BusinessUserCriteria,
    businessName: NameAttribute,
    businessCategory: BusinessCategory,
    userTier: BusinessUserTier,
    businessType: BusinessType): Future[ServiceResponse[Unit]] = {

    (for {
      existingApplication ← EitherT.fromEither[Future](applicationDao.getBusinessUserApplicationByCriteria(
        applicationCriteria, None, None, None).asServiceResponse)
      _ ← EitherT.cond[Future](
        existingApplication.isEmpty,
        (),
        validationError(s"There’s an active business user application with the id you have provided for ${businessName.underlying}"))
      existingBusinessUser ← EitherT.fromEither[Future](businessUserDao.getBusinessUserByCriteria(
        businessUserCriteria, None, None, None).asServiceResponse)
      _ ← EitherT.cond[Future](
        existingBusinessUser.isEmpty,
        (),
        validationError(s"There’s an active business user application with the id you have provided for ${businessName.underlying}"))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(userTier.toString, BusinessUserTiers.toString))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(businessType.toString, BusinessTypes.toString))
    } yield {
      ()
    }).value
  }

  def createBusinessUserContactInfo(
    applicationId: UUID,
    contacts: Seq[ContactPerson],
    addresses: Seq[ContactAddress],
    createdBy: String,
    createdAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[BusinessUserApplication]] = {

    (for {

      _ ← {
        logger.debug(">>>> createBusinessUserContactInfo step 1.1: generic validation")
        EitherT.fromEither[Future]((contacts ++ addresses).map(_.validate).find(_.isLeft).getOrElse(Right(())))
      }

      _ ← {
        logger.debug(">>>> createBusinessUserContactInfo step 1.2: group validation")
        EitherT.fromEither[Future](Seq(addresses.validate, contacts.validate).find(_.isLeft).getOrElse(Right(())))
      }

      txnConn ← {
        logger.debug(">>>> createBusinessUserContactInfo step 2: start dao transaction")
        EitherT.fromEither[Future](applicationDao.startTransaction.leftMap(_.asDomainError))
      }

      businessUsrApplic ← {
        logger.debug(">>>> createBusinessUserContactInfo step 3: fetch the business user application and check status")
        EitherT.fromEither[Future](applicationDao.getBusinessUserApplicationByCriteria(
          BusinessUserApplicationCriteria(uuid = UUIDLike(applicationId.toString).some).asDao, None, None, None)
          .fold(_.asDomainError.toLeft, _.headOption match {
            case Some(entity) if (entity.status != Status.Ongoing) ⇒
              Left(validationError(s"Unable to create/update contact info and address for business user application with id [$applicationId] because it is not an ongoing application"))
            case Some(entity) ⇒ Right(entity)
            case None ⇒ Left(notFoundError(s"Business user application with this id [$applicationId] not found"))
          }))
      }

      _ ← {
        logger.debug(">>>> createBusinessUserContactInfo step 4: clear existing contact info for this application")
        EitherT.fromEither[Future](applicContactsDao.deleteByApplicationId(businessUsrApplic.id)(txnConn.some)
          .leftMap(_.asDomainError))
      }

      contactPersons ← {
        logger.debug(">>>> createBusinessUserContactInfo step 5: insert new contact info")
        val contactsDao = contacts.makeSureAtLeastOneDefaultContact.map(_.asDao(businessUsrApplic.id, createdBy, createdAt))
        EitherT.fromEither[Future](applicContactsDao.insert(contactsDao.toSeq)(txnConn.some)
          .bimap(_.asDomainError, _.map(_.asDomain)))
      }

      countries ← {
        logger.debug(">>>> createBusinessUserContactInfo step 6: fetch the existing countries")
        EitherT.fromEither[Future](countryDao.getCountries.leftMap(_.asDomainError).map(_.map(c ⇒ c.name → c.id).toMap))
      }

      _ ← {
        logger.debug(">>>> createBusinessUserContactInfo step 7: check if country in address exists")
        EitherT.fromEither[Future](if (addresses.map(_.country).forall(countries.contains(_))) Right(true) else Left(validationError("Unknown country in address found")))
      }

      _ ← {
        logger.debug(">>>> createBusinessUserContactInfo step 8: delete existing address for this application")
        EitherT.fromEither[Future](applicAddressDao.deleteByApplicationId(businessUsrApplic.id)(txnConn.some)
          .leftMap(_.asDomainError))
      }

      contactAddresses ← {
        logger.debug(">>>> createBusinessUserContactInfo step 9: insert new address")
        val addressesDao = addresses.map(addr ⇒ addr.asDao(
          businessUsrApplic.id,
          countryId = countries.get(addr.country).get,
          createdBy, createdAt))
        EitherT.fromEither[Future](applicAddressDao.insert(addressesDao)(txnConn.some).bimap(_.asDomainError, _.map(a ⇒ a.asDomain(countries.map(_.swap).get(a.countryId).get))))
      }

      defaultCurrency ← EitherT(getDefaultCurrency)

      updatedBUApplic ← {
        logger.debug(">>>> createBusinessUserContactInfo step 10: update the application stage and map to domain model")
        val dto = BusinessUserApplicationToUpdateStage(Stages.Contact, createdBy, createdAt).asDao(lastUpdatedAt.orElse(businessUsrApplic.updatedAt))
        EitherT.fromEither[Future](applicationDao.updateBusinessUserApplication(applicationId.asEntityId, dto)(txnConn.some)
          .asServiceResponse(_.map(_.asDomain(defaultCurrency)))
          .fold(
            _.toLeft,
            _ match {
              case Some(Success(result)) ⇒ Right(result)
              case Some(Failure(error)) ⇒ Left(dtoMappingError("Failed to map entity to domain model"))
              case None ⇒
                notFoundError(s"Contact info and address not created/updated because business user application with id [$applicationId] was not found").toLeft
            }))
      }

      _ ← {
        logger.debug(">>>> createBusinessUserContactInfo step 11: end the dao transaction")
        EitherT.fromEither[Future](applicationDao.endTransaction(txnConn).leftMap(_.asDomainError))
      }

    } yield {
      updatedBUApplic.copy(contactPersons = contactPersons, contactAddress = contactAddresses)
    }).value
  }
}
