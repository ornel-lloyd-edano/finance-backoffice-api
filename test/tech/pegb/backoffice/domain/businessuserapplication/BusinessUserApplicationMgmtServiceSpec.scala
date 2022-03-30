package tech.pegb.backoffice.domain.businessuserapplication

import java.sql.Connection
import java.time._
import java.util.{Currency, UUID}

import cats.data.NonEmptyList
import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import play.api.libs.json.Json
import tech.pegb.backoffice.core.integration.abstraction.BusinessUserCoreApiClient
import tech.pegb.backoffice.core.integration.dto.BusinessUserCreateResponse
import tech.pegb.backoffice.dao.Dao.EntityId
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.{BUAppPrimaryAddressesDao, BUApplicPrimaryContactsDao, BusinessUserApplicationDao, CountryDao, _}
import tech.pegb.backoffice.dao.businessuserapplication.dto.{AccountConfigToInsert, BUApplicPrimaryAddressToInsert, BUApplicPrimaryContactToInsert, BusinessUserApplicationToUpdate, ExternalAccountToInsert, TransactionConfigToInsert}
import tech.pegb.backoffice.dao.businessuserapplication.entity.{BUApplicPrimaryAddress, BUApplicPrimaryContact, BusinessUserApplication, Country}
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.currency.entity
import tech.pegb.backoffice.dao.customer.abstraction.BusinessUserDao
import tech.pegb.backoffice.dao.{DaoError, businessuserapplication}
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountNumber, AccountType}
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.businessuserapplication.abstraction._
import tech.pegb.backoffice.domain.businessuserapplication.dto._
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes._
import tech.pegb.backoffice.domain.businessuserapplication.model._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{Msisdn, NameAttribute}
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.dto.DocumentCriteria
import tech.pegb.backoffice.domain.document.model.Document
import tech.pegb.backoffice.domain.parameter.abstraction.ParameterManagement
import tech.pegb.backoffice.domain.parameter.model.Parameter
import tech.pegb.backoffice.domain.transaction.model.TransactionType
import tech.pegb.backoffice.mapping.dao.domain.businessuserapplication.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.businessuserapplication.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class BusinessUserApplicationMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val dao = stub[BusinessUserApplicationDao]
  private val contactsDao = stub[BUApplicPrimaryContactsDao]
  private val addressDao = stub[BUAppPrimaryAddressesDao]
  private val countryDao = stub[CountryDao]
  private val businessUserDao = stub[BusinessUserDao]
  private val configDao = stub[BusinessUserApplicationConfigDao]
  private val currencyDao = stub[CurrencyDao]
  private val coreApiClient = stub[BusinessUserCoreApiClient]
  private val docMgmtService = stub[DocumentManagement]
  private val parameterManagement = stub[ParameterManagement]

  val businessUserApplicationService = inject[BusinessUserApplicationManagement]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val config = inject[AppConfig]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[BusinessUserApplicationDao].to(dao),
      bind[BusinessUserDao].to(businessUserDao),
      bind[BUApplicPrimaryContactsDao].to(contactsDao),
      bind[BUAppPrimaryAddressesDao].to(addressDao),
      bind[CountryDao].to(countryDao),
      bind[CurrencyDao].to(currencyDao),
      bind[BusinessUserApplicationConfigDao].to(configDao),
      bind[DocumentManagement].to(docMgmtService),
      bind[BusinessUserCoreApiClient].to(coreApiClient),
      bind[ParameterManagement].to(parameterManagement),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val jsonValue =
    """
      |{
      |"id": 20,
      |"key": "default_currency",
      |"value":  "KES",
      |"type": "json",
      |"forAndroid": false,
      |"forIOS": false,
      |"forBackoffice": false
      |}
    """.stripMargin

  val currency = Parameter(
    id = UUID.randomUUID(),
    key = "default_currency",
    value = Json.parse(jsonValue),
    explanation = none,
    metadataId = "system_settings",
    platforms = Vector.empty,
    createdAt = now.some,
    createdBy = "pegbuser".some,
    updatedAt = none,
    updatedBy = none)

  val KES = Currency.getInstance("KES")

  "BusinessUserApplicationMgmtService" should {
    "return created business user application in createBusinessUserApplication" in {
      val newUUID = UUID.randomUUID()
      val dto = BusinessUserApplicationToCreate(
        uuid = newUUID,
        businessName = NameAttribute("Universal Catering Co"),
        brandName = NameAttribute("Costa Coffee DSO"),
        businessCategory = BusinessCategory("Restaurants - 5812"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val expected = BusinessUserApplication(
        id = 1,
        uuid = UUID.randomUUID().toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (dao.getBusinessUserApplicationByCriteria _)
        .when(dto.asApplicationCriteriaDao(butNotThisId = newUUID.some), None, None, None)
        .returns(Nil.asRight[DaoError])

      (businessUserDao.getBusinessUserByCriteria _)
        .when(dto.asBusinessUserCriteriaDao, None, None, None)
        .returns(Nil.asRight[DaoError])

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(newUUID.toString).some)
      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Nil.asRight[DaoError])

      (typesDao.getBusinessCategories _).when()
        .returns(Right(List(
          (25, "Restaurants - 5812", None),
          (26, "Grocery - 1111", None))))

      (typesDao.getBusinessUserTiers _).when()
        .returns(Right(List(
          (25, "small", None),
          (26, "medium", None))))

      (typesDao.getBusinessTypes _).when()
        .returns(Right(List(
          (25, "merchant", None),
          (26, "corporation", None))))

      (dao.insertBusinessUserApplication _)
        .when(dto.asDao)
        .returns(expected.asRight[DaoError])

      val result = businessUserApplicationService.createBusinessUserApplication(dto)
      whenReady(result) { actual ⇒
        actual mustBe Right(expected.asDomain(KES).get)
      }
    }

    "return updated business user application in createBusinessUserApplication when dto has UUID" in {
      val existingUUID = UUID.randomUUID()
      val dto = BusinessUserApplicationToCreate(
        uuid = existingUUID,
        businessName = NameAttribute("Universal Catering Co"),
        brandName = NameAttribute("Costa Coffee DSO"),
        businessCategory = BusinessCategory("Restaurants - 5812"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val expected = BusinessUserApplication(
        id = 1,
        uuid = existingUUID.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (dao.getBusinessUserApplicationByCriteria _)
        .when(dto.asApplicationCriteriaDao(existingUUID.some), None, None, None)
        .returns(Nil.asRight[DaoError])

      (businessUserDao.getBusinessUserByCriteria _)
        .when(dto.asBusinessUserCriteriaDao, None, None, None)
        .returns(Nil.asRight[DaoError])

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(existingUUID.toString).some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(expected).asRight[DaoError])

      (typesDao.getBusinessCategories _).when()
        .returns(Right(List(
          (25, "Restaurants - 5812", None),
          (26, "Grocery - 1111", None))))

      (typesDao.getBusinessUserTiers _).when()
        .returns(Right(List(
          (25, "small", None),
          (26, "medium", None),
          (27, "medium", None))))

      (typesDao.getBusinessTypes _).when()
        .returns(Right(List(
          (25, "merchant", None),
          (26, "corporation", None))))

      (dao.updateBusinessUserApplication(_: EntityId, _: BusinessUserApplicationToUpdate)(_: Option[Connection]))
        .when(existingUUID.asEntityId, dto.asUpdateDao(expected.updatedAt), None)
        .returns(expected.some.asRight[DaoError])

      val result = businessUserApplicationService.createBusinessUserApplication(dto)
      whenReady(result) { actual ⇒
        actual mustBe Right(expected.asDomain(KES).get)
      }
    }

    "return list of user application in getBusinessUserApplicationByCriteria" in {
      val criteria = BusinessUserApplicationCriteria(
        businessName = NameAttribute("Universal Catering Co").some,
        partialMatchFields = Set("business_name"))

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = UUID.randomUUID().toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      val expected2 = BusinessUserApplication(
        id = 2,
        uuid = UUID.randomUUID().toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee International City",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "submitted",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "213/212EE",
        taxNumber = "C12342M".some,
        registrationDate = LocalDate.of(2019, 2, 1).some,
        explanation = None,
        userId = None,
        submittedBy = "pegbuser".some,
        submittedAt = now.some,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(expected1, expected2).asRight[DaoError])

      val result = businessUserApplicationService.getBusinessUserApplicationByCriteria(criteria, Nil, None, None)
      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(expected1, expected2).map(_.asDomain(KES).get))
      }
    }

    "return count of user application in countBusinessUserApplicationByCriteria" in {
      val criteria = BusinessUserApplicationCriteria(
        businessName = NameAttribute("Universal Catering Co").some,
        partialMatchFields = Set("business_name"))

      (dao.countBusinessUserApplicationByCriteria _)
        .when(criteria.asDao)
        .returns(2.asRight[DaoError])

      val result = businessUserApplicationService.countBusinessUserApplicationByCriteria(criteria)
      whenReady(result) { actual ⇒
        actual mustBe Right(2)
      }
    }

    "return user application in getBusinessUserApplicationById" in {
      val uuid = UUID.randomUUID()

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(expected1).asRight[DaoError])

      (countryDao.getCountries _).when().returns(Right(Nil))

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val result = businessUserApplicationService.getBusinessUserApplicationById(uuid, Nil)
      whenReady(result) { actual ⇒
        actual mustBe Right(expected1.asDomain(KES).get)
      }
    }

    "return user application in getBusinessUserApplicationByI stage contains (config)" in {
      val uuid = UUID.randomUUID()

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val businessUserApplication = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(businessUserApplication).asRight[DaoError])

      val expectedTxnConfig = businessuserapplication.entity.TransactionConfig(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        transactionType = "merchant_payment",
        currencyId = 1,
        currencyCode = "KES",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getTxnConfig _)
        .when(businessUserApplication.id)
        .returns(Seq(expectedTxnConfig).asRight[DaoError])

      val expectedAccountConfig = businessuserapplication.entity.AccountConfig(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        accountType = "distribution",
        accountName = "Default Distribution",
        currencyId = 1,
        currencyCode = "KES",
        isDefault = true,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getAccountConfig _)
        .when(businessUserApplication.id)
        .returns(Seq(expectedAccountConfig).asRight[DaoError])

      val expectedExternalAccounts = businessuserapplication.entity.ExternalAccount(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        provider = "mPesa",
        accountNumber = "955100",
        accountHolder = "Costa Coffee FZE",
        currencyId = 1,
        currencyCode = "KES",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getExternalAccount _)
        .when(businessUserApplication.id)
        .returns(Seq(expectedExternalAccounts).asRight[DaoError])

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val expected = businessUserApplication.asDomain(KES).get
        .addConfigs(
          transactionConfig = Seq(expectedTxnConfig.asDomain.toOption).flatten,
          accountConfig = Seq(expectedAccountConfig.asDomain.toOption).flatten,
          externalAccount = Seq(expectedExternalAccounts.asDomain.toOption).flatten)

      val result = businessUserApplicationService.getBusinessUserApplicationById(uuid, Seq(Stages.Config))
      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "return validation error in getBusinessUserApplicationById if stage is unknown" in {
      val uuid = UUID.randomUUID()

      val result = businessUserApplicationService.getBusinessUserApplicationById(uuid, Seq("stage 5 cancer"))
      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError("1 or more business application stage is not known [stage 5 cancer]"))
      }
    }

    "return BusinessUserApplication with config data in createBusinessUserApplicationConifg" in {
      val existingUUID = UUID.randomUUID()
      val dto = BusinessUserApplicationConfigToCreate(
        applicationUUID = existingUUID,
        transactionConfig = Seq(TransactionConfig(TransactionType("merchant_payment"), Currency.getInstance("KES"))),
        accountConfig = Seq(AccountConfig(
          accountType = AccountType("distribution"),
          accountName = NameAttribute("Default Distribution"),
          currency = Currency.getInstance("KES"),
          isDefault = true)),
        externalAccounts = Seq(ExternalAccount(
          provider = NameAttribute("mPesa"),
          accountNumber = AccountNumber("955100"),
          accountHolder = NameAttribute("Costa Coffee FZE"),
          currency = Currency.getInstance("KES"))),
        createdBy = "pegbuser",
        createdAt = now)

      val existingApplication = BusinessUserApplication(
        id = 1,
        uuid = existingUUID.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(existingUUID.toString).some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(existingApplication).asRight[DaoError])

      (typesDao.getTransactionTypes _).when()
        .returns(Right(List(
          (25, "merchant_payment", None),
          (26, "cashout", None))))

      (currencyDao.getAll _).when()
        .returns(
          Right(Set(
            entity.Currency(
              id = 1,
              name = "KES",
              description = "Kenyan Shilling".some,
              isActive = true,
              icon = "kes.jpeg".some,
              createdAt = now,
              createdBy = "pegbuser",
              updatedAt = now.some,
              updatedBy = "pegbuser".some),
            entity.Currency(
              id = 1,
              name = "PHP",
              description = "Philippine Peso".some,
              isActive = true,
              icon = "php.jpeg".some,
              createdAt = now,
              createdBy = "pegbuser",
              updatedAt = now.some,
              updatedBy = "pegbuser".some))))

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val mockTxnConn = mock[java.sql.Connection]
      (configDao.startTransaction _).when().returns(Right(mockTxnConn))

      (configDao.deleteTxnConfig(_: Int)(_: Option[Connection]))
        .when(1, mockTxnConn.some)
        .returns(Right(()))

      (configDao.deleteAccountConfig(_: Int)(_: Option[Connection]))
        .when(1, mockTxnConn.some)
        .returns(Right(()))

      (configDao.deleteExternalAccount(_: Int)(_: Option[Connection]))
        .when(1, mockTxnConn.some)
        .returns(Right(()))

      val expectedTxnConfig = businessuserapplication.entity.TransactionConfig(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        transactionType = "merchant_payment",
        currencyId = 1,
        currencyCode = "KES",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.insertTxnConfig(_: Int, _: NonEmptyList[TransactionConfigToInsert], _: String, _: LocalDateTime)(_: Option[Connection]))
        .when(1, NonEmptyList.fromList(dto.transactionConfig.flatMap(_.asDao(Map("KES" → 1, "PHP" → 2)).toOption).toList).get, "pegbuser", now, *)
        .returns(Right(Seq(expectedTxnConfig)))

      val expectedAccountConfig = businessuserapplication.entity.AccountConfig(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        accountType = "distribution",
        accountName = "Default Distribution",
        currencyId = 1,
        currencyCode = "KES",
        isDefault = true,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.insertAccountConfig(_: Int, _: NonEmptyList[AccountConfigToInsert], _: String, _: LocalDateTime)(_: Option[Connection]))
        .when(1, NonEmptyList.fromList(dto.accountConfig.flatMap(_.asDao(Map("KES" → 1, "PHP" → 2)).toOption).toList).get, "pegbuser", now, *)
        .returns(Right(Seq(expectedAccountConfig)))

      val expectedExternalAccounts = businessuserapplication.entity.ExternalAccount(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        provider = "mPesa",
        accountNumber = "955100",
        accountHolder = "Costa Coffee FZE",
        currencyId = 1,
        currencyCode = "KES",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.insertExternalAccount(_: Int, _: NonEmptyList[ExternalAccountToInsert], _: String, _: LocalDateTime)(_: Option[Connection]))
        .when(1, NonEmptyList.fromList(dto.externalAccounts.flatMap(_.asDao(Map("KES" → 1, "PHP" → 2)).toOption).toList).get, "pegbuser", now, *)
        .returns(Right(Seq(expectedExternalAccounts)))

      val businessUserApplicationToUpdate = BusinessUserApplicationToUpdate(
        stage = Stages.Config.some,
        updatedBy = dto.createdBy,
        updatedAt = dto.createdAt,
        lastUpdatedAt = existingApplication.updatedAt)
      val updated = existingApplication.copy(stage = Stages.Config)
      (dao.updateBusinessUserApplication(_: EntityId, _: BusinessUserApplicationToUpdate)(_: Option[Connection]))
        .when(existingUUID.asEntityId, businessUserApplicationToUpdate, mockTxnConn.some)
        .returns(updated.some.asRight[DaoError])

      (configDao.endTransaction(_: Connection)).when(mockTxnConn).returns(Right(()))

      val result = businessUserApplicationService.createBusinessUserApplicationConfig(dto)

      val expected = updated.asDomain(KES).get
        .addConfigs(
          transactionConfig = Seq(expectedTxnConfig.asDomain.toOption).flatten,
          accountConfig = Seq(expectedAccountConfig.asDomain.toOption).flatten,
          externalAccount = Seq(expectedExternalAccounts.asDomain.toOption).flatten)

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "return error in createBusinessUserApplicationConifg when transaction_config is not valid" in {
      val existingUUID = UUID.randomUUID()
      val dto = BusinessUserApplicationConfigToCreate(
        applicationUUID = existingUUID,
        transactionConfig = Seq(TransactionConfig(TransactionType("merchant_payment"), Currency.getInstance("KES"))),
        accountConfig = Seq(AccountConfig(
          accountType = AccountType("distribution"),
          accountName = NameAttribute("Default Distribution"),
          currency = Currency.getInstance("KES"),
          isDefault = true)),
        externalAccounts = Seq(ExternalAccount(
          provider = NameAttribute("mPesa"),
          accountNumber = AccountNumber("955100"),
          accountHolder = NameAttribute("Costa Coffee FZE"),
          currency = Currency.getInstance("KES"))),
        createdBy = "pegbuser",
        createdAt = now)

      val existingApplication = BusinessUserApplication(
        id = 1,
        uuid = existingUUID.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "super_merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(existingUUID.toString).some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(existingApplication).asRight[DaoError])

      (typesDao.getTransactionTypes _).when()
        .returns(Right(List(
          (25, "merchant_payment", None),
          (26, "cashout", None))))

      (currencyDao.getAll _).when()
        .returns(
          Right(Set(
            entity.Currency(
              id = 1,
              name = "KES",
              description = "Kenyan Shilling".some,
              isActive = true,
              icon = "kes.jpeg".some,
              createdAt = now,
              createdBy = "pegbuser",
              updatedAt = now.some,
              updatedBy = "pegbuser".some),
            entity.Currency(
              id = 1,
              name = "PHP",
              description = "Philippine Peso".some,
              isActive = true,
              icon = "php.jpeg".some,
              createdAt = now,
              createdBy = "pegbuser",
              updatedAt = now.some,
              updatedBy = "pegbuser".some))))

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val result = businessUserApplicationService.createBusinessUserApplicationConfig(dto)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Invalid transaction_config List(TransactionConfig(merchant_payment,KES)) for business user, expected config: List(TransactionConfig(merchant_payment,KES), TransactionConfig(cashin,KES), TransactionConfig(cashout,KES))"))
      }
    }

    "return BusinessUserApplication with contact person and address data in createBusinessUserContactInfo" in {

      val mockApplicationId = UUID.randomUUID()
      val contactPersons = Seq(ContactPerson(
        contactType = ContactTypes.Owner,
        name = "Lloyd",
        middleName = None,
        surname = "Edano",
        phoneNumber = Some(Msisdn("+971544451679")),
        email = Some(Email("o.lloyd@pegb.tech")),
        idType = "Emirates ID", //TODO objectify
        velocityUser = None,
        isDefault = None))

      val addresses = Seq(
        ContactAddress(
          addressType = AddressTypes.Primary,
          country = "Philippines",
          city = "Manila",
          postalCode = Some("02013"),
          address = "123 Odersky Street Scala Village ",
          coordinates = None),
        ContactAddress(
          addressType = AddressTypes.Secondary,
          country = "UAE",
          city = "Dubai",
          postalCode = Some("0000"),
          address = "Flat 102 Bafta Bldg Muraqabat Road",
          coordinates = None))

      val mockBUApplication = BusinessUserApplication(
        id = 3,
        uuid = mockApplicationId.toString,
        businessName = "Monkey Business",
        brandName = "Bananas R Us",
        businessCategory = "Toys",
        stage = "stage1",
        status = "ongoing",
        userTier = "small",
        businessType = "merchant",
        registrationNumber = "12335",
        taxNumber = "3647492".some,
        registrationDate = LocalDate.of(2020, 1, 27).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "test_user",
        createdAt = now,
        updatedBy = None,
        updatedAt = None)

      val mockTxnConn = mock[java.sql.Connection]
      (dao.startTransaction _).when().returns(Right(mockTxnConn))

      (dao.getBusinessUserApplicationByCriteria _).when(BusinessUserApplicationCriteria(
        uuid = UUIDLike(mockApplicationId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(mockBUApplication)))

      (contactsDao.deleteByApplicationId(_: Int)(_: Option[Connection]))
        .when(mockBUApplication.id, *).returns(Right(()))

      val mockContactsDaoResult = Seq(
        BUApplicPrimaryContact(
          id = 1,
          uuid = UUID.randomUUID().toString,
          applicationId = mockBUApplication.id,
          contactType = ContactTypes.Owner,
          name = "Lloyd",
          middleName = None,
          surname = "Edano",
          phoneNumber = "+971544451679",
          email = "o.lloyd@pegb.tech",
          idType = "Emirates ID",
          isVelocityUser = false,
          velocityLevel = None,
          isDefaultContact = Some(true),
          createdBy = mockBUApplication.createdBy,
          createdAt = mockBUApplication.createdAt,
          updatedBy = None,
          updatedAt = None))

      (contactsDao.insert(_: Seq[BUApplicPrimaryContactToInsert])(_: Option[Connection]))
        .when(contactPersons.makeSureAtLeastOneDefaultContact.map(_.asDao(mockBUApplication.id, mockBUApplication.createdBy, mockBUApplication.createdAt)).toSeq, *)
        .returns(Right(mockContactsDaoResult))

      val mockCountries = Seq(
        Country(
          id = 1,
          name = "Philippines",
          officialName = Some("Republic of the Philippines"),
          abbrev = Some("PH"),
          label = None,
          icon = None,
          isActive = Some(true),
          createdBy = "test_user",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        Country(
          id = 2,
          name = "UAE",
          officialName = Some("United Arab Emirates"),
          abbrev = Some("UAE"),
          label = None,
          icon = None,
          isActive = Some(true),
          createdBy = "test_user",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None))
      (countryDao.getCountries _).when().returns(Right(mockCountries))

      (addressDao.deleteByApplicationId(_: Int)(_: Option[Connection]))
        .when(mockBUApplication.id, *).returns(Right(()))

      val expectedAddress: Seq[BUApplicPrimaryAddressToInsert] = addresses.map(a ⇒ {
        a.asDao(mockBUApplication.id, mockCountries.find(_.name == a.country).map(_.id).get, mockBUApplication.createdBy, mockBUApplication.createdAt)
      })

      val mockAddressDaoResult = Seq(
        BUApplicPrimaryAddress(
          id = 1,
          uuid = UUID.randomUUID().toString,
          applicationId = mockBUApplication.id,
          addressType = AddressTypes.Primary,
          countryId = 1,
          city = "Manila",
          postalCode = Some("02013"),
          address = Some("123 Odersky Street Scala Village "),
          coordinateX = None,
          coordinateY = None,
          createdBy = mockBUApplication.createdBy,
          createdAt = mockBUApplication.createdAt,
          updatedBy = None,
          updatedAt = None),
        BUApplicPrimaryAddress(
          id = 1,
          uuid = UUID.randomUUID().toString,
          applicationId = mockBUApplication.id,
          addressType = AddressTypes.Secondary,
          countryId = 2,
          city = "Dubai",
          postalCode = Some("0000"),
          address = Some("Flat 102 Bafta Bldg Muraqabat Road"),
          coordinateX = None,
          coordinateY = None,
          createdBy = mockBUApplication.createdBy,
          createdAt = mockBUApplication.createdAt,
          updatedBy = None,
          updatedAt = None))

      (addressDao.insert(_: Seq[BUApplicPrimaryAddressToInsert])(_: Option[Connection]))
        .when(expectedAddress, *).returns(Right(mockAddressDaoResult))

      val mockResult = BusinessUserApplication(
        id = 3,
        uuid = mockApplicationId.toString,
        businessName = "Monkey Business",
        brandName = "Bananas R Us",
        businessCategory = "Toys",
        stage = "contact_info",
        status = "ongoing",
        userTier = "small",
        businessType = "merchant",
        registrationNumber = "12335",
        taxNumber = "3647492".some,
        registrationDate = mockBUApplication.registrationDate,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockBUApplication.createdBy,
        createdAt = mockBUApplication.createdAt,
        updatedBy = None,
        updatedAt = None)
      (dao.updateBusinessUserApplication(_: EntityId, _: BusinessUserApplicationToUpdate)(_: Option[Connection]))
        .when(
          mockApplicationId.asEntityId,
          BusinessUserApplicationToUpdateStage(Stages.Contact, mockBUApplication.createdBy, mockBUApplication.createdAt).asDao(None),
          mockTxnConn.some)
        .returns(Right(mockResult.some))

      (dao.endTransaction(_: Connection)).when(mockTxnConn).returns(Right(()))

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val result = businessUserApplicationService.createBusinessUserContactInfo(
        mockApplicationId,
        contactPersons, addresses, mockBUApplication.createdBy, mockBUApplication.createdAt, None)

      val expected = tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplication(
        id = 3, uuid = UUID.fromString(mockBUApplication.uuid), businessName = NameAttribute("Monkey Business"),
        brandName = NameAttribute("Bananas R Us"), businessCategory = BusinessCategory("Toys"), stage = ApplicationStage("contact_info"),
        status = ApplicationStatus("ongoing"), userTier = BusinessUserTiers.Small, businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("12335"), taxNumber = TaxNumber("3647492").some,
        registrationDate = mockBUApplication.registrationDate, explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons =
          Seq(ContactPerson(ContactTypes.Owner, "Lloyd", None, "Edano", Some(Msisdn("+971544451679")), Some(Email("o.lloyd@pegb.tech", true)), "Emirates ID", None, Some(true))),
        contactAddress = Seq(
          ContactAddress(AddressTypes.Primary, "Philippines", "Manila", Some("02013"), "123 Odersky Street Scala Village ", None),
          ContactAddress(AddressTypes.Secondary, "UAE", "Dubai", Some("0000"), "Flat 102 Bafta Bldg Muraqabat Road", None)),
        defaultCurrency = KES,
        submittedBy = None, submittedAt = None, checkedBy = None, checkedAt = None,
        createdBy = mockBUApplication.createdBy, createdAt = mockBUApplication.createdAt, updatedBy = None, updatedAt = None)

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "return validation error in createBusinessUserContactInfo if owner type is unknown" in {
      val uuid = UUID.randomUUID()
      val contactPersons = Seq(ContactPerson(
        contactType = "illegal owner",
        name = "Lloyd",
        middleName = None,
        surname = "Edano",
        phoneNumber = Some(Msisdn("+971544451679")),
        email = Some(Email("o.lloyd@pegb.tech")),
        idType = "Emirates ID", //TODO objectify
        velocityUser = None,
        isDefault = None))

      val result = businessUserApplicationService.createBusinessUserContactInfo(
        uuid, contactPersons, Nil, "test_user", LocalDateTime.now, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"invalid contact_type [${contactPersons.head.contactType}]. Valid choices: ${ContactTypes.toSeq.defaultMkString}"))
      }
    }

    "return validation error in createBusinessUserContactInfo if velocity user type is unknown" in {
      val uuid = UUID.randomUUID()
      val contactPersons = Seq(ContactPerson(
        contactType = ContactTypes.Owner,
        name = "Lloyd",
        middleName = None,
        surname = "Edano",
        phoneNumber = Some(Msisdn("+971544451679")),
        email = Some(Email("o.lloyd@pegb.tech")),
        idType = "Emirates ID", //TODO objectify
        velocityUser = Some("Velo City"),
        isDefault = None))

      val result = businessUserApplicationService.createBusinessUserContactInfo(
        uuid, contactPersons, Nil, "test_user", LocalDateTime.now, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"invalid velocity user level [${contactPersons.head.velocityUser.get}]. Valid choices: ${VelocityUserLevels.toSeq.defaultMkString}"))
      }
    }

    "return validation error in createBusinessUserContactInfo if more than 1 isDefaultContact=true" in {
      val uuid = UUID.randomUUID()
      val contactPersons = Seq(
        ContactPerson(
          contactType = ContactTypes.Owner,
          name = "Lloyd",
          middleName = None,
          surname = "Edano",
          phoneNumber = Some(Msisdn("+971544451679")),
          email = Some(Email("o.lloyd@pegb.tech")),
          idType = "Emirates ID", //TODO objectify
          velocityUser = Some("admin"),
          isDefault = Some(true)),
        ContactPerson(
          contactType = ContactTypes.Employee,
          name = "Ornel",
          middleName = None,
          surname = "Edano",
          phoneNumber = Some(Msisdn("+971544451789")),
          email = Some(Email("o.ornel@pegb.tech")),
          idType = "Emirates ID", //TODO objectify
          velocityUser = Some("operator"),
          isDefault = Some(true)))

      val result = businessUserApplicationService.createBusinessUserContactInfo(
        uuid, contactPersons, Nil, "test_user", LocalDateTime.now, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Only 1 default contact is allowed"))
      }
    }

    "return validation error in createBusinessUserContactInfo if duplicate combo of name, middlename, surname, email found" in {
      val uuid = UUID.randomUUID()
      val contactPersons = Seq(
        ContactPerson(
          contactType = ContactTypes.Owner,
          name = "Lloyd",
          middleName = None,
          surname = "Edano",
          phoneNumber = Some(Msisdn("+971544451679")),
          email = Some(Email("o.lloyd@pegb.tech")),
          idType = "Emirates ID", //TODO objectify
          velocityUser = Some("admin"),
          isDefault = Some(true)),
        ContactPerson(
          contactType = ContactTypes.Employee,
          name = "Lloyd",
          middleName = None,
          surname = "Edano",
          phoneNumber = Some(Msisdn("+971544451789")),
          email = Some(Email("o.lloyd@pegb.tech")),
          idType = "Emirates ID", //TODO objectify
          velocityUser = Some("operator"),
          isDefault = Some(false)))

      val result = businessUserApplicationService.createBusinessUserContactInfo(
        uuid, contactPersons, Nil, "test_user", LocalDateTime.now, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Duplicate contact person details found"))
      }
    }

    "return validation error in createBusinessUserContactInfo if address type is unknown" in {
      val uuid = UUID.randomUUID()
      val contactAddress = Seq(ContactAddress(
        addressType = "tertiary",
        country = "Philippines",
        city = "Manila",
        postalCode = Some("02013"),
        address = "123 Odersky Street Scala Village ",
        coordinates = None))

      val result = businessUserApplicationService.createBusinessUserContactInfo(
        uuid, Nil, contactAddress, "test_user", LocalDateTime.now, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"invalid address_type [${contactAddress.head.addressType}]. Valid choices: ${AddressTypes.toSeq.defaultMkString}"))
      }
    }

    "return validation error in createBusinessUserContactInfo if coordinates is out of range" in {
      val uuid = UUID.randomUUID()
      val contactAddress = Seq(ContactAddress(
        addressType = AddressTypes.Primary,
        country = "Philippines",
        city = "Manila",
        postalCode = Some("02013"),
        address = "123 Odersky Street Scala Village ",
        coordinates = Some(AddressCoordinates(0, 360))))

      val result = businessUserApplicationService.createBusinessUserContactInfo(
        uuid, Nil, contactAddress, "test_user", LocalDateTime.now, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"invalid value for y coordinate [${contactAddress.head.coordinates.get.y}]. Valid range from -180 to 180"))
      }
    }

    "return validation error in createBusinessUserContactInfo if list of address given has duplicate postal code" in {
      val uuid = UUID.randomUUID()
      val contactAddress = Seq(
        ContactAddress(
          addressType = AddressTypes.Primary,
          country = "Philippines",
          city = "Manila",
          postalCode = Some("02013"),
          address = "123 Odersky Street Scala Village ",
          coordinates = Some(AddressCoordinates(5.123, 45.321))),
        ContactAddress(
          addressType = AddressTypes.Primary,
          country = "Philippines",
          city = "Davao",
          postalCode = Some("02013"),
          address = "Lot 10 Blk 10, Duterte Street, DDS Subdivision",
          coordinates = None))

      val result = businessUserApplicationService.createBusinessUserContactInfo(
        uuid, Nil, contactAddress, "test_user", LocalDateTime.now, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Duplicate postal code found"))
      }
    }

    "return validation error in createBusinessUserContactInfo if list of address given has duplicate coordinates" in {
      val uuid = UUID.randomUUID()
      val contactAddress = Seq(
        ContactAddress(
          addressType = AddressTypes.Primary,
          country = "Philippines",
          city = "Manila",
          postalCode = Some("02013"),
          address = "123 Odersky Street Scala Village ",
          coordinates = Some(AddressCoordinates(5.123, 45.321))),
        ContactAddress(
          addressType = AddressTypes.Primary,
          country = "UAE",
          city = "Dubai",
          postalCode = Some("02013"),
          address = "Lot 10 Blk 10, Duterte Street, DDS Subdivision",
          coordinates = Some(AddressCoordinates(5.123, 45.321))))

      val result = businessUserApplicationService.createBusinessUserContactInfo(
        uuid, Nil, contactAddress, "test_user", LocalDateTime.now, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Duplicate coordinates found"))
      }
    }

    "return validation error in createBusinessUserContactInfo if there is duplicate address identity in the list of address" in {
      val uuid = UUID.randomUUID()
      val contactAddress = Seq(
        ContactAddress(
          addressType = AddressTypes.Primary,
          country = "UAE",
          city = "Dubai",
          postalCode = Some("02013"),
          address = "123 Odersky Street Scala Village",
          coordinates = Some(AddressCoordinates(5.123, 45.321))),
        ContactAddress(
          addressType = AddressTypes.Secondary,
          country = "UAE",
          city = "Dubai",
          postalCode = Some("00001"),
          address = "123 Odersky Street Scala Village",
          coordinates = Some(AddressCoordinates(61.0034, 80.00021))))

      val result = businessUserApplicationService.createBusinessUserContactInfo(
        uuid, Nil, contactAddress, "test_user", LocalDateTime.now, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Duplicate address identity found"))
      }
    }

    "return success when on cancelling ongoing application" in {
      val uuid = UUID.randomUUID()
      val explanation = "No explanation, just want to cancel"
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(expected1).asRight[DaoError])

      val updateDto = dto.BusinessUserApplicationToUpdateStatus(
        status = Status.Cancelled,
        explanation = explanation.some,
        updatedAt = updatedAt,
        updatedBy = updatedBy)
      (dao.updateBusinessUserApplication(_: EntityId, _: BusinessUserApplicationToUpdate)(_: Option[Connection]))
        .when(uuid.asEntityId, updateDto.asDao(lastUpdatedAt), None)
        .returns(expected1.some.asRight[DaoError])

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val result = businessUserApplicationService.cancelBusinessUserApplication(
        applicationId = uuid,
        explanation = explanation,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }

    "return left when on cancelling pending application" in {
      val uuid = UUID.randomUUID()
      val explanation = "No explanation, just want to cancel"
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "pending",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(expected1).asRight[DaoError])

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val result = businessUserApplicationService.cancelBusinessUserApplication(
        applicationId = uuid,
        explanation = explanation,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Status transition from pending to cancelled is not allowed."))
      }
    }

    "return success when on rejecting pending application" in {
      val uuid = UUID.randomUUID()
      val explanation = "No explanation, just want to reject"
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "pending",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(expected1).asRight[DaoError])

      val updateDto = dto.BusinessUserApplicationToUpdateStatus(
        status = Status.Rejected,
        explanation = explanation.some,
        checkedAt = updatedAt.some,
        checkedBy = updatedBy.some,
        updatedAt = updatedAt,
        updatedBy = updatedBy)
      (dao.updateBusinessUserApplication(_: EntityId, _: BusinessUserApplicationToUpdate)(_: Option[Connection]))
        .when(uuid.asEntityId, updateDto.asDao(lastUpdatedAt), None)
        .returns(expected1.some.asRight[DaoError])

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val result = businessUserApplicationService.rejectBusinessUserApplication(
        applicationId = uuid,
        explanation = explanation,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }

    "return left when on rejecting ongoing application" in {
      val uuid = UUID.randomUUID()
      val explanation = "No explanation, just want to cancel"
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(expected1).asRight[DaoError])

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val result = businessUserApplicationService.rejectBusinessUserApplication(
        applicationId = uuid,
        explanation = explanation,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Status transition from ongoing to rejected is not allowed."))
      }
    }

    "return left when on rejecting application where checker is creator" in {
      val uuid = UUID.randomUUID()
      val explanation = "No explanation, just want to cancel"
      val updatedAt = now
      val updatedBy = "system"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "pending",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = "system".some,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(expected1).asRight[DaoError])
      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val result = businessUserApplicationService.rejectBusinessUserApplication(
        applicationId = uuid,
        explanation = explanation,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Checker cannot be the submitter of application, applicationId: $uuid"))
      }
    }

    "return success when on sending back pending application" in {
      val uuid = UUID.randomUUID()
      val explanation = "No explanation, just want to send back"
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "pending",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(expected1).asRight[DaoError])
      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      val updateDto = dto.BusinessUserApplicationToUpdateStatus(
        status = Status.Ongoing,
        explanation = explanation.some,
        updatedAt = updatedAt,
        updatedBy = updatedBy)
      (dao.updateBusinessUserApplication(_: EntityId, _: BusinessUserApplicationToUpdate)(_: Option[Connection]))
        .when(uuid.asEntityId, updateDto.asDao(lastUpdatedAt), None)
        .returns(expected1.some.asRight[DaoError])

      val result = businessUserApplicationService.sendForCorrectionBusinessUserApplication(
        applicationId = uuid,
        explanation = explanation,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }
    "return left when on sending back ongoing application" in {
      val uuid = UUID.randomUUID()
      val explanation = "No explanation, just want to cancel"
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)
      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(expected1).asRight[DaoError])

      val result = businessUserApplicationService.sendForCorrectionBusinessUserApplication(
        applicationId = uuid,
        explanation = explanation,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Status transition from ongoing to ongoing is not allowed."))
      }
    }
    "return success when on submitting ongoing application" in {
      val uuid = UUID.randomUUID()
      val explanation = "No explanation, just want to reject"
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val application = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "config",
        status = "ongoing",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(application).asRight[DaoError])

      val expectedTxnConfig = businessuserapplication.entity.TransactionConfig(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        transactionType = "merchant_payment",
        currencyId = 1,
        currencyCode = "KES",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getTxnConfig _)
        .when(application.id)
        .returns(Seq(expectedTxnConfig).asRight[DaoError])

      val expectedAccountConfig = businessuserapplication.entity.AccountConfig(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        accountType = "distribution",
        accountName = "Default Distribution",
        currencyId = 1,
        currencyCode = "KES",
        isDefault = true,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getAccountConfig _)
        .when(application.id)
        .returns(Seq(expectedAccountConfig).asRight[DaoError])

      val expectedExternalAccounts = businessuserapplication.entity.ExternalAccount(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        provider = "mPesa",
        accountNumber = "955100",
        accountHolder = "Costa Coffee FZE",
        currencyId = 1,
        currencyCode = "KES",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getExternalAccount _)
        .when(application.id)
        .returns(Seq(expectedExternalAccounts).asRight[DaoError])

      val updateDto = dto.BusinessUserApplicationToUpdateStatus(
        status = Status.Pending,
        submittedAt = updatedAt.some,
        submittedBy = updatedBy.some,
        updatedAt = updatedAt,
        updatedBy = updatedBy)
      (dao.updateBusinessUserApplication(_: EntityId, _: BusinessUserApplicationToUpdate)(_: Option[Connection]))
        .when(uuid.asEntityId, updateDto.asDao(lastUpdatedAt), None)
        .returns(application.some.asRight[DaoError])

      val result = businessUserApplicationService.submitBusinessUserApplication(
        applicationId = uuid,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }
    "return left when on submitting already pending application" in {
      val uuid = UUID.randomUUID()
      val explanation = "No explanation, just want to cancel"
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val application = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "pending",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)
      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(application).asRight[DaoError])

      val expectedTxnConfig = businessuserapplication.entity.TransactionConfig(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        transactionType = "merchant_payment",
        currencyId = 1,
        currencyCode = "KES",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getTxnConfig _)
        .when(application.id)
        .returns(Seq(expectedTxnConfig).asRight[DaoError])

      val expectedAccountConfig = businessuserapplication.entity.AccountConfig(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        accountType = "distribution",
        accountName = "Default Distribution",
        currencyId = 1,
        currencyCode = "KES",
        isDefault = true,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getAccountConfig _)
        .when(application.id)
        .returns(Seq(expectedAccountConfig).asRight[DaoError])

      val expectedExternalAccounts = businessuserapplication.entity.ExternalAccount(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        provider = "mPesa",
        accountNumber = "955100",
        accountHolder = "Costa Coffee FZE",
        currencyId = 1,
        currencyCode = "KES",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getExternalAccount _)
        .when(application.id)
        .returns(Seq(expectedExternalAccounts).asRight[DaoError])

      val updateDto = dto.BusinessUserApplicationToUpdateStatus(
        status = Status.Pending,
        submittedAt = updatedAt.some,
        submittedBy = updatedBy.some,
        updatedAt = updatedAt,
        updatedBy = updatedBy)
      (dao.updateBusinessUserApplication(_: EntityId, _: BusinessUserApplicationToUpdate)(_: Option[Connection]))
        .when(uuid.asEntityId, updateDto.asDao(lastUpdatedAt), None)
        .returns(application.some.asRight[DaoError])

      val result = businessUserApplicationService.submitBusinessUserApplication(
        applicationId = uuid,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Status transition from pending to pending is not allowed."))
      }
    }

    "return left when on submitting application with invalid txn config" in {
      val uuid = UUID.randomUUID()
      val explanation = "No explanation, just want to cancel"
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val application = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "ongoing",
        userTier = "basic",
        businessType = "agent",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)
      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(application).asRight[DaoError])

      val expectedTxnConfig = businessuserapplication.entity.TransactionConfig(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        transactionType = "merchant_payment",
        currencyId = 1,
        currencyCode = "KES",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getTxnConfig _)
        .when(application.id)
        .returns(Seq(expectedTxnConfig).asRight[DaoError])

      val expectedAccountConfig = businessuserapplication.entity.AccountConfig(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        accountType = "distribution",
        accountName = "Default Distribution",
        currencyId = 1,
        currencyCode = "KES",
        isDefault = true,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getAccountConfig _)
        .when(application.id)
        .returns(Seq(expectedAccountConfig).asRight[DaoError])

      val expectedExternalAccounts = businessuserapplication.entity.ExternalAccount(
        id = 1,
        uuid = UUID.randomUUID().toString,
        applicationId = 1,
        provider = "mPesa",
        accountNumber = "955100",
        accountHolder = "Costa Coffee FZE",
        currencyId = 1,
        currencyCode = "KES",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "pegbuser".some)
      (configDao.getExternalAccount _)
        .when(application.id)
        .returns(Seq(expectedExternalAccounts).asRight[DaoError])

      val updateDto = dto.BusinessUserApplicationToUpdateStatus(
        status = Status.Pending,
        submittedAt = updatedAt.some,
        submittedBy = updatedBy.some,
        updatedAt = updatedAt,
        updatedBy = updatedBy)
      (dao.updateBusinessUserApplication(_: EntityId, _: BusinessUserApplicationToUpdate)(_: Option[Connection]))
        .when(uuid.asEntityId, updateDto.asDao(lastUpdatedAt), None)
        .returns(application.some.asRight[DaoError])

      val result = businessUserApplicationService.submitBusinessUserApplication(
        applicationId = uuid,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Invalid transaction_config List(TransactionConfig(merchant_payment,KES)) for business user, expected config: List(TransactionConfig(cashin,KES), TransactionConfig(cashout,KES))"))
      }
    }

    "return success when on approving pending application" in {
      val uuid = UUID.randomUUID()
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val mockBuUsrApplication = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "pending",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(mockBuUsrApplication).asRight[DaoError])

      val mockCoreApiClientResponse = BusinessUserCreateResponse(
        id = 55,
        uuid = UUID.fromString("1ea7377c-8224-41ef-9719-b084236f3b66"),
        userId = 1,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = Some("B12342M"),
        registrationDate = Some("2019-01-01"),
        createdBy = "superuser",
        createdAt = now.toString,
        updatedBy = Some("superuser"),
        updatedAt = Some(now.toString))

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (coreApiClient.createBusinessUserApplication _)
        .when(mockBuUsrApplication.id, updatedBy)
        .returns(Future.successful(Right(mockCoreApiClientResponse)))

      val expectedCriteria = DocumentCriteria(businessApplicationId = uuid.some)
      val mockDocumentIdsResult = Seq(
        Document.empty.copy(applicationId = uuid.some),
        Document.empty.copy(applicationId = uuid.some),
        Document.empty.copy(applicationId = uuid.some))
      (docMgmtService.getDocumentsByCriteria _)
        .when(expectedCriteria, Nil, None, None)
        .returns(Future.successful(Right(mockDocumentIdsResult)))

      (docMgmtService.persistDocument _)
        .when(mockDocumentIdsResult(0).id, updatedBy, updatedAt)
        .returns(Future.successful(Right(())))
      (docMgmtService.persistDocument _)
        .when(mockDocumentIdsResult(1).id, updatedBy, updatedAt)
        .returns(Future.successful(Right(())))
      (docMgmtService.persistDocument _)
        .when(mockDocumentIdsResult(2).id, updatedBy, updatedAt)
        .returns(Future.successful(Right(())))

      val result = businessUserApplicationService.approveBusinessUserApplication(
        applicationId = uuid,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }

    "return error when application approved by same user who submitted" in {
      val uuid = UUID.randomUUID()
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val mockBuUsrApplication = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "pending",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = Some(updatedBy),
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(mockBuUsrApplication).asRight[DaoError])

      val result = businessUserApplicationService.approveBusinessUserApplication(
        applicationId = uuid,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Checker cannot be the submitter of application, applicationId: $uuid"))
      }
    }

    "return success when approving pending application even if no documents were found for this application" in {
      val uuid = UUID.randomUUID()
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val mockBuUsrApplication = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "pending",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(mockBuUsrApplication).asRight[DaoError])

      val mockCoreApiClientResponse = BusinessUserCreateResponse(
        id = 55,
        uuid = UUID.fromString("1ea7377c-8224-41ef-9719-b084236f3b66"),
        userId = 1,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = Some("B12342M"),
        registrationDate = Some("2019-01-01"),
        createdBy = "superuser",
        createdAt = now.toString,
        updatedBy = Some("superuser"),
        updatedAt = Some(now.toString))

      (coreApiClient.createBusinessUserApplication _)
        .when(mockBuUsrApplication.id, updatedBy)
        .returns(Future.successful(Right(mockCoreApiClientResponse)))

      val expectedCriteria = DocumentCriteria(businessApplicationId = uuid.some)
      (docMgmtService.getDocumentsByCriteria _)
        .when(expectedCriteria, Nil, None, None)
        .returns(Future.successful(Right(Nil)))

      val result = businessUserApplicationService.approveBusinessUserApplication(
        applicationId = uuid,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }

    "return success when approving pending application even if getting document metadata from mysql failed" in {
      val uuid = UUID.randomUUID()
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val mockBuUsrApplication = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "pending",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(mockBuUsrApplication).asRight[DaoError])

      val mockCoreApiClientResponse = BusinessUserCreateResponse(
        id = 55,
        uuid = UUID.fromString("1ea7377c-8224-41ef-9719-b084236f3b66"),
        userId = 1,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = Some("B12342M"),
        registrationDate = Some("2019-01-01"),
        createdBy = "superuser",
        createdAt = now.toString,
        updatedBy = Some("superuser"),
        updatedAt = Some(now.toString))

      (coreApiClient.createBusinessUserApplication _)
        .when(mockBuUsrApplication.id, updatedBy)
        .returns(Future.successful(Right(mockCoreApiClientResponse)))

      val expectedCriteria = DocumentCriteria(businessApplicationId = uuid.some)
      (docMgmtService.getDocumentsByCriteria _)
        .when(expectedCriteria, Nil, None, None)
        .returns(Future.successful(Left(ServiceError.unknownError("mysql failed"))))

      val result = businessUserApplicationService.approveBusinessUserApplication(
        applicationId = uuid,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }

    "return success when approving pending application even if couchbase to hdfs document transfer failed" in {
      val uuid = UUID.randomUUID()
      val updatedAt = now
      val updatedBy = "pegbuser"
      val lastUpdatedAt = now.some

      val criteria = BusinessUserApplicationCriteria(
        uuid = UUIDLike(uuid.toString).some)

      val mockBuUsrApplication = BusinessUserApplication(
        id = 1,
        uuid = uuid.toString,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        stage = "identity_info",
        status = "pending",
        userTier = "basic",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = "B12342M".some,
        registrationDate = LocalDate.of(2019, 1, 1).some,
        explanation = None,
        userId = None,
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "system",
        createdAt = now,
        updatedBy = "system".some,
        updatedAt = now.some)

      (dao.getBusinessUserApplicationByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Seq(mockBuUsrApplication).asRight[DaoError])

      val mockCoreApiClientResponse = BusinessUserCreateResponse(
        id = 55,
        uuid = UUID.fromString("1ea7377c-8224-41ef-9719-b084236f3b66"),
        userId = 1,
        businessName = "Universal Catering Co",
        brandName = "Costa Coffee DSO",
        businessCategory = "Restaurants - 5182",
        businessType = "merchant",
        registrationNumber = "212/212EE",
        taxNumber = Some("B12342M"),
        registrationDate = Some("2019-01-01"),
        createdBy = "superuser",
        createdAt = now.toString,
        updatedBy = Some("superuser"),
        updatedAt = Some(now.toString))

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(currency))))

      (coreApiClient.createBusinessUserApplication _)
        .when(mockBuUsrApplication.id, updatedBy)
        .returns(Future.successful(Right(mockCoreApiClientResponse)))

      val expectedCriteria = DocumentCriteria(businessApplicationId = uuid.some)
      val mockDocumentIdsResult = Seq(
        Document.empty.copy(applicationId = uuid.some),
        Document.empty.copy(applicationId = uuid.some),
        Document.empty.copy(applicationId = uuid.some))
      (docMgmtService.getDocumentsByCriteria _)
        .when(expectedCriteria, Nil, None, None)
        .returns(Future.successful(Right(mockDocumentIdsResult)))

      (docMgmtService.persistDocument _)
        .when(mockDocumentIdsResult(0).id, updatedBy, updatedAt)
        .returns(Future.successful(Right(()))).once()
      (docMgmtService.persistDocument _)
        .when(mockDocumentIdsResult(1).id, updatedBy, updatedAt)
        .returns(Future.successful(Right(()))).once()
      (docMgmtService.persistDocument _)
        .when(mockDocumentIdsResult(2).id, updatedBy, updatedAt)
        .returns(Future.successful(Left(ServiceError.unknownError("couchbase to hdfs failed")))).once()

      val result = businessUserApplicationService.approveBusinessUserApplication(
        applicationId = uuid,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }

  }
}
