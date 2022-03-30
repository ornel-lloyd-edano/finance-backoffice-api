package tech.pegb.backoffice.api.businessuserapplication

import java.time._
import java.util.{Currency, UUID}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HeaderNames
import play.api.inject.{Binding, bind}
import play.api.libs.{Files ⇒ PlayFiles}
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Headers, MultipartFormData}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.businessuserapplication.controllers.BusinessUserApplicationController
import tech.pegb.backoffice.api.businessuserapplication.controllers.impl.{BusinessUserApplicationController ⇒ BusinessUserApplicationControllerImpl}
import tech.pegb.backoffice.api.businessuserapplication.dto.BusinessUserApplicationContactInfoToCreate
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountNumber, AccountType}
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.{BusinessUserApplicationManagement, Stages, Status}
import tech.pegb.backoffice.domain.businessuserapplication.dto._
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes._
import tech.pegb.backoffice.domain.businessuserapplication.model._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{Msisdn, NameAttribute}
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.dto.DocumentToCreate
import tech.pegb.backoffice.domain.document.model._
import tech.pegb.backoffice.domain.transaction.model.TransactionType
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.businessuserapplication.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class BusinessUserApplicationControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  private val documentService = stub[DocumentManagement]
  private val businessUserApplicationManagement = stub[BusinessUserApplicationManagement]
  private val latestVersion = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[BusinessUserApplicationManagement].to(businessUserApplicationManagement),
      bind[DocumentManagement].to(documentService),
      bind[LatestVersionService].to(latestVersion),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val endpoint = inject[BusinessUserApplicationController].getRoute

  "BusinessUserApplicationController" should {
    "create business user application in POST /business_user_applications" in {

      val dto = BusinessUserApplicationToCreate(
        uuid = UUID.randomUUID(),
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

      val jsonRequest =
        s"""{
           |  "business_name": "${dto.businessName.underlying}",
           |  "brand_name": "${dto.brandName.underlying}",
           |  "business_category": "${dto.businessCategory.underlying}",
           |  "user_tier": "${dto.userTier.toString}",
           |  "business_type": "${dto.businessType.toString}",
           |  "registration_number": "${dto.registrationNumber.underlying}",
           |  "tax_number": "${dto.taxNumber.get.underlying}",
           |  "registration_date": "${dto.registrationDate.get}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val expected = BusinessUserApplication(
        id = 1,
        uuid = UUID.randomUUID(),
        businessName = dto.businessName,
        brandName = dto.brandName,
        businessCategory = dto.businessCategory,
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = dto.userTier,
        businessType = dto.businessType,
        registrationNumber = dto.registrationNumber,
        taxNumber = dto.taxNumber,
        registrationDate = dto.registrationDate,
        explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Nil,
        contactAddress = Nil,
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      (businessUserApplicationManagement.createBusinessUserApplication _)
        .when(dto)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val fakeRequest = FakeRequest(POST, s"/$endpoint/${dto.uuid}", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""{
           |"id":"${expected.uuid}",
           |"business_name":"Universal Catering Co",
           |"brand_name":"Costa Coffee DSO",
           |"business_category":"Restaurants - 5812",
           |"stage":"identity_info",
           |"status":"ongoing",
           |"user_tier":"small",
           |"business_type":"merchant",
           |"registration_number":"213/564654EE",
           |"tax_number":"A213546468977M",
           |"registration_date":"1996-01-01",
           |"explanation":null,
           |"submitted_by":null,
           |"submitted_at":null,
           |"checked_by":null,
           |"checked_at":null,
           |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"KES"}],
           |"created_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "return business_user_applications json in GET /business_user_applications/{id}" in {
      val id = UUID.randomUUID()

      val expected = BusinessUserApplication(
        id = 1,
        uuid = id,
        businessName = NameAttribute("Universal Catering Co"),
        brandName = NameAttribute("Costa Coffee DSO"),
        businessCategory = BusinessCategory("Restaurants - 5812"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Nil,
        contactAddress = Nil,
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      (businessUserApplicationManagement.getBusinessUserApplicationById _).when(id, Nil)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/$endpoint/$id")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""{
           |"id":"${expected.uuid}",
           |"business_name":"Universal Catering Co",
           |"brand_name":"Costa Coffee DSO",
           |"business_category":"Restaurants - 5812",
           |"stage":"identity_info",
           |"status":"ongoing",
           |"user_tier":"small",
           |"business_type":"merchant",
           |"registration_number":"213/564654EE",
           |"tax_number":"A213546468977M",
           |"registration_date":"1996-01-01",
           |"explanation":null,
           |"submitted_by":null,
           |"submitted_at":null,
           |"checked_by":null,
           |"checked_at":null,
           |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"KES"}],
           |"created_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson

    }
    "redirect to /business_user_applications/{id} and return business_user_applications json if /stage/identity_info?status=ongoing" in {
      val id = UUID.randomUUID()

      val expected = BusinessUserApplication(
        id = 1,
        uuid = id,
        businessName = NameAttribute("Universal Catering Co"),
        brandName = NameAttribute("Costa Coffee DSO"),
        businessCategory = BusinessCategory("Restaurants - 5812"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Nil,
        contactAddress = Nil,
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      (businessUserApplicationManagement.getBusinessUserApplicationById _).when(id, Nil)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/$endpoint/$id/stage/${Stages.Identity}?status=${Status.Ongoing}")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""{
           |"id":"${expected.uuid}",
           |"business_name":"Universal Catering Co",
           |"brand_name":"Costa Coffee DSO",
           |"business_category":"Restaurants - 5812",
           |"stage":"identity_info",
           |"status":"ongoing",
           |"user_tier":"small",
           |"business_type":"merchant",
           |"registration_number":"213/564654EE",
           |"tax_number":"A213546468977M",
           |"registration_date":"1996-01-01",
           |"explanation":null,
           |"submitted_by":null,
           |"submitted_at":null,
           |"checked_by":null,
           |"checked_at":null,
           |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"KES"}],
           |"created_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      //status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson

    }
    "return all applications and count as paginated result in GET /business_user_applications" in {

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = UUID.randomUUID(),
        businessName = NameAttribute("Universal Catering Co"),
        brandName = NameAttribute("Costa Coffee DSO"),
        businessCategory = BusinessCategory("Restaurants - 5812"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.SuperMerchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Nil,
        contactAddress = Nil,
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val expected2 = BusinessUserApplication(
        id = 2,
        uuid = UUID.randomUUID(),
        businessName = NameAttribute("Aling Nena"),
        brandName = NameAttribute("Tindahan ni Aling Nena"),
        businessCategory = BusinessCategory("Store - 1111"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Agent,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Nil,
        contactAddress = Nil,
        submittedBy = None,
        submittedAt = None,
        defaultCurrency = Currency.getInstance("KES"),
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val mockLatestVersion = LocalDateTime.now.toString
      val criteria = BusinessUserApplicationCriteria(
        partialMatchFields = BusinessUserApplicationControllerImpl.businessUSerApplicationPartialMatchFields.filterNot(_ == "disabled"))

      (businessUserApplicationManagement.getBusinessUserApplicationByCriteria _)
        .when(criteria, Nil, None, None)
        .returns(Future.successful(Seq(expected1, expected2).asRight[ServiceError]))
      (businessUserApplicationManagement.countBusinessUserApplicationByCriteria _)
        .when(criteria)
        .returns(Future.successful(2.asRight[ServiceError]))
      (latestVersion.getLatestVersion _).when(criteria).returns(Future.successful(Right(mockLatestVersion.some)))

      val resp = route(app, FakeRequest(GET, s"/$endpoint")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{
           |"id":"${expected1.uuid}",
           |"business_name":"Universal Catering Co",
           |"brand_name":"Costa Coffee DSO",
           |"business_category":"Restaurants - 5812",
           |"stage":"identity_info",
           |"status":"ongoing",
           |"user_tier":"small",
           |"business_type":"super_merchant",
           |"registration_number":"213/564654EE",
           |"tax_number":"A213546468977M",
           |"registration_date":"1996-01-01",
           |"explanation":null,
           |"submitted_by":null,
           |"submitted_at":null,
           |"checked_by":null,
           |"checked_at":null,
           |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"KES"},{"transaction_type":"cashin","currency_code":"KES"},{"transaction_type":"cashout","currency_code":"KES"}],
           |"created_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |},
           |{
           |"id":"${expected2.uuid}",
           |"business_name":"Aling Nena",
           |"brand_name":"Tindahan ni Aling Nena",
           |"business_category":"Store - 1111",
           |"stage":"identity_info",
           |"status":"ongoing",
           |"user_tier":"small",
           |"business_type":"agent",
           |"registration_number":"213/564654EE",
           |"tax_number":"A213546468977M",
           |"registration_date":"1996-01-01",
           |"explanation":null,
           |"submitted_by":null,
           |"submitted_at":null,
           |"checked_by":null,
           |"checked_at":null,
           |"valid_transaction_config":[{"transaction_type":"cashin","currency_code":"KES"},{"transaction_type":"cashout","currency_code":"KES"}],
           |"created_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson

    }

    "return applications and count as paginated result in GET /business_user_applications with date filter" in {

      val expected1 = BusinessUserApplication(
        id = 1,
        uuid = UUID.randomUUID(),
        businessName = NameAttribute("Universal Catering Co"),
        brandName = NameAttribute("Costa Coffee DSO"),
        businessCategory = BusinessCategory("Restaurants - 5812"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Nil,
        contactAddress = Nil,
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val expected2 = BusinessUserApplication(
        id = 2,
        uuid = UUID.randomUUID(),
        businessName = NameAttribute("Aling Nena"),
        brandName = NameAttribute("Tindahan ni Aling Nena"),
        businessCategory = BusinessCategory("Store - 1111"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Nil,
        contactAddress = Nil,
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val mockLatestVersion = LocalDateTime.now.toString
      val criteria = BusinessUserApplicationCriteria(
        createdAtFrom = LocalDateTime.of(2020, 1, 1, 0, 0, 0).some,
        createdAtTo = LocalDateTime.of(2021, 1, 1, 23, 59, 59).some,
        partialMatchFields = BusinessUserApplicationControllerImpl.businessUSerApplicationPartialMatchFields.filterNot(_ == "disabled"))

      (businessUserApplicationManagement.getBusinessUserApplicationByCriteria _)
        .when(criteria, Nil, None, None)
        .returns(Future.successful(Seq(expected1, expected2).asRight[ServiceError]))
      (businessUserApplicationManagement.countBusinessUserApplicationByCriteria _)
        .when(criteria)
        .returns(Future.successful(2.asRight[ServiceError]))
      (latestVersion.getLatestVersion _).when(criteria).returns(Future.successful(Right(mockLatestVersion.some)))

      val resp = route(app, FakeRequest(GET, s"/$endpoint?date_from=2020-01-01&date_to=2021-01-01")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{
           |"id":"${expected1.uuid}",
           |"business_name":"Universal Catering Co",
           |"brand_name":"Costa Coffee DSO",
           |"business_category":"Restaurants - 5812",
           |"stage":"identity_info",
           |"status":"ongoing",
           |"user_tier":"small",
           |"business_type":"merchant",
           |"registration_number":"213/564654EE",
           |"tax_number":"A213546468977M",
           |"registration_date":"1996-01-01",
           |"explanation":null,
           |"submitted_by":null,
           |"submitted_at":null,
           |"checked_by":null,
           |"checked_at":null,
           |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"KES"}],
           |"created_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |},
           |{
           |"id":"${expected2.uuid}",
           |"business_name":"Aling Nena",
           |"brand_name":"Tindahan ni Aling Nena",
           |"business_category":"Store - 1111",
           |"stage":"identity_info",
           |"status":"ongoing",
           |"user_tier":"small",
           |"business_type":"merchant",
           |"registration_number":"213/564654EE",
           |"tax_number":"A213546468977M",
           |"registration_date":"1996-01-01",
           |"explanation":null,
           |"submitted_by":null,
           |"submitted_at":null,
           |"checked_by":null,
           |"checked_at":null,
           |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"KES"}],
           |"created_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson

    }

    "create resources for POST /business_user_applications/{id}/stage/config" in {
      val mockId = UUID.randomUUID()
      val jsonRequest =
        s"""
           |{
           |  "transaction_config": [
           |    {
           |      "transaction_type": "merchant_payment",
           |      "currency_code": "KES"
           |    }
           |  ],
           |  "account_config": [
           |    {
           |      "account_type": "distribution",
           |      "account_name": "Default Distribution",
           |      "currency_code": "USD",
           |      "is_default": true
           |    }
           |  ],
           |  "external_accounts": [
           |    {
           |      "provider": "mPesa",
           |      "account_number": "955100",
           |      "account_holder": "Costa Coffee FZE",
           |      "currency_code": "KES"
           |    }
           |  ]
           |}""".stripMargin

      val dto = BusinessUserApplicationConfigToCreate(
        applicationUUID = mockId,
        transactionConfig = Seq(
          TransactionConfig(
            TransactionType("merchant_payment"),
            Currency.getInstance("KES"))),
        accountConfig = Seq(
          AccountConfig(
            accountType = AccountType("distribution"),
            accountName = NameAttribute("Default Distribution"),
            currency = Currency.getInstance("USD"),
            isDefault = true)),
        externalAccounts = Seq(
          ExternalAccount(
            provider = NameAttribute("mPesa"),
            accountNumber = AccountNumber("955100"),
            accountHolder = NameAttribute("Costa Coffee FZE"),
            currency = Currency.getInstance("KES"))),
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val expected = BusinessUserApplication(
        id = 1,
        uuid = mockId,
        businessName = NameAttribute("Aling Nena"),
        brandName = NameAttribute("Tindahan ni Aling Nena"),
        businessCategory = BusinessCategory("Store - 1111"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Seq(
          TransactionConfig(
            TransactionType("merchant_payment"),
            Currency.getInstance("KES"))),
        accountConfig = Seq(
          AccountConfig(
            accountType = AccountType("distribution"),
            accountName = NameAttribute("Default Distribution"),
            currency = Currency.getInstance("USD"),
            isDefault = true)),
        externalAccounts = Seq(
          ExternalAccount(
            provider = NameAttribute("mPesa"),
            accountNumber = AccountNumber("955100"),
            accountHolder = NameAttribute("Costa Coffee FZE"),
            currency = Currency.getInstance("KES"))),
        contactPersons = Nil,
        contactAddress = Nil,
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      (businessUserApplicationManagement.createBusinessUserApplicationConfig _)
        .when(dto)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val fakeRequest = FakeRequest(POST, s"/$endpoint/$mockId/stage/config", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""{
           |"id":"$mockId",
           |"status":"ongoing",
           |"transaction_config":[{"transaction_type":"merchant_payment","currency_code":"KES"}],
           |"account_config":[{"account_type":"distribution","account_name":"Default Distribution","currency_code":"USD","is_default":true}],
           |"external_accounts":[{"provider":"mPesa","account_number":"955100","account_holder":"Costa Coffee FZE","currency_code":"KES"}],
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"submitted_by":null}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "return config in GET /business_user_applications/{id}/stage/config" in {
      val mockId = UUID.randomUUID()

      val expected = BusinessUserApplication(
        id = 1,
        uuid = mockId,
        businessName = NameAttribute("Aling Nena"),
        brandName = NameAttribute("Tindahan ni Aling Nena"),
        businessCategory = BusinessCategory("Store - 1111"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Seq(
          TransactionConfig(
            TransactionType("merchant_payment"),
            Currency.getInstance("KES"))),
        accountConfig = Seq(
          AccountConfig(
            accountType = AccountType("distribution"),
            accountName = NameAttribute("Default Distribution"),
            currency = Currency.getInstance("USD"),
            isDefault = true)),
        externalAccounts = Seq(
          ExternalAccount(
            provider = NameAttribute("mPesa"),
            accountNumber = AccountNumber("955100"),
            accountHolder = NameAttribute("Costa Coffee FZE"),
            currency = Currency.getInstance("KES"))),
        contactPersons = Nil,
        contactAddress = Nil,
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      (businessUserApplicationManagement.getBusinessUserApplicationById _)
        .when(mockId, Seq(Stages.Config))
        .returns(Future.successful(expected.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/$endpoint/$mockId/stage/config")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""{
           |"id":"$mockId",
           |"status":"ongoing",
           |"transaction_config":[{"transaction_type":"merchant_payment","currency_code":"KES"}],
           |"account_config":[{"account_type":"distribution","account_name":"Default Distribution","currency_code":"USD","is_default":true}],
           |"external_accounts":[{"provider":"mPesa","account_number":"955100","account_holder":"Costa Coffee FZE","currency_code":"KES"}],
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"submitted_by":null}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "create resources for POST /business_user_applications/{id}/stage/contact_info" in {
      val mockId = UUID.randomUUID()

      val jsonPayload =
        s"""
           |{
           |"contacts":[
           |{"contact_type":"Business Owner",
           |"name":"Lloyd",
           |"middle_name":"Pepito",
           |"surname":"Edano",
           |"phone_number":"+971544451679",
           |"email":"o.lloyd@pegb.tech",
           |"id_type":"National ID",
           |"is_velocity_user":true,
           |"velocity_level":"admin"}
           |],
           |"addresses":[
           |{"address_type":"primary",
           |"country":"UAE",
           |"city":"Dubai",
           |"postal_code":"00000",
           |"address":"22B Street Muraqabat Road Bafta Building Flat 102",
           |"coordinate_x":128.34566,
           |"coordinate_y":36.12334}
           |],
           |"updated_at":null
           |}
         """.stripMargin

      val mockContact: BusinessUserApplicationContactInfoToCreate = jsonPayload.as(classOf[BusinessUserApplicationContactInfoToCreate], isStrict = false).get
      val mockContactPersons = mockContact.contacts.map(_.asDomain.get)
      val mockContactAddress = mockContact.addresses.map(_.asDomain.get)

      val mockResult = BusinessUserApplication(
        id = 1,
        uuid = mockId,
        businessName = NameAttribute("Universal Catering Co"),
        brandName = NameAttribute("Costa Coffee DSO"),
        businessCategory = BusinessCategory("Restaurants - 5812"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Seq(ContactPerson(
          contactType = "Business Owner",
          name = "Lloyd",
          middleName = Some("Pepito"),
          surname = "Edano",
          phoneNumber = Some(Msisdn("+971544451679")),
          email = Some(Email("o.lloyd@pegb.tech")),
          idType = "National ID",
          velocityUser = Some("admin"),
          isDefault = Some(true))),
        contactAddress = Seq(ContactAddress(
          addressType = "primary",
          country = "UAE",
          city = "Dubai",
          postalCode = Some("00000"),
          address = "22B Street Muraqabat Road Bafta Building Flat 102",
          coordinates = Some(AddressCoordinates(128.34566, 36.12334)))),
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = None,
        updatedAt = None)

      (businessUserApplicationManagement.createBusinessUserContactInfo _)
        .when(mockId, mockContactPersons, mockContactAddress, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None)
        .returns(Future(Right(mockResult)))

      val resp = route(app, FakeRequest("POST", s"/$endpoint/$mockId/stage/${Stages.Contact}")
        .withBody(jsonPayload)
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""
           |{
           |"id":"$mockId",
           |"status":"ongoing",
           |"contacts":[
           |{"contact_type":"Business Owner",
           |"name":"Lloyd",
           |"middle_name":"Pepito",
           |"surname":"Edano",
           |"phone_number":"+971544451679",
           |"email":"o.lloyd@pegb.tech",
           |"id_type":"National ID",
           |"is_velocity_user":true,
           |"velocity_level":"admin",
           |"is_default_contact":true}
           |],
           |"addresses":[
           |{"address_type":"primary",
           |"country":"UAE",
           |"city":"Dubai",
           |"postal_code":"00000",
           |"address":"22B Street Muraqabat Road Bafta Building Flat 102",
           |"coordinate_x":128.34566,
           |"coordinate_y":36.12334}
           |],
           |"created_at":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"${mockResult.createdBy}",
           |"updated_at":null,
           |"updated_by":null,
           |"submitted_by":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected

    }

    "return business_user_application_contact_info json in GET /business_user_applications/{id}/stage/contact_info" in {
      val mockId = UUID.randomUUID()

      val mockResult = BusinessUserApplication(
        id = 1,
        uuid = mockId,
        businessName = NameAttribute("Universal Catering Co"),
        brandName = NameAttribute("Costa Coffee DSO"),
        businessCategory = BusinessCategory("Restaurants - 5812"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Seq(ContactPerson(
          contactType = "Business Owner",
          name = "Lloyd",
          middleName = Some("Pepito"),
          surname = "Edano",
          phoneNumber = Some(Msisdn("+971544451679")),
          email = Some(Email("o.lloyd@pegb.tech")),
          idType = "National ID",
          velocityUser = Some("admin"),
          isDefault = Some(true))),
        contactAddress = Seq(ContactAddress(
          addressType = "primary",
          country = "UAE",
          city = "Dubai",
          postalCode = Some("00000"),
          address = "22B Street Muraqabat Road Bafta Building Flat 102",
          coordinates = Some(AddressCoordinates(128.34566, 36.12334)))),
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = None,
        updatedAt = None)

      (businessUserApplicationManagement.getBusinessUserApplicationById _).when(mockId, Seq("contact_info"))
        .returns(Future.successful(Right(mockResult)))

      val resp = route(app, FakeRequest(GET, s"/$endpoint/$mockId/stage/contact_info")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""
           |{
           |"id":"$mockId",
           |"status":"ongoing",
           |"contacts":[
           |{"contact_type":"Business Owner",
           |"name":"Lloyd",
           |"middle_name":"Pepito",
           |"surname":"Edano",
           |"phone_number":"+971544451679",
           |"email":"o.lloyd@pegb.tech",
           |"id_type":"National ID",
           |"is_velocity_user":true,
           |"velocity_level":"admin",
           |"is_default_contact":true}
           |],
           |"addresses":[
           |{"address_type":"primary",
           |"country":"UAE",
           |"city":"Dubai",
           |"postal_code":"00000",
           |"address":"22B Street Muraqabat Road Bafta Building Flat 102",
           |"coordinate_x":128.34566,
           |"coordinate_y":36.12334}
           |],
           |"created_at":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"${mockResult.createdBy}",
           |"updated_at":null,
           |"updated_by":null,
           |"submitted_by":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "return DocumentToRead json in POST /business_user_applications/{id}/stage/documents" in {

      val mockId = UUID.randomUUID()

      val jsonPayload =
        s"""
           |{
           |"customer_id":null,
           |"application_id":null,
           |"filename":"my-business-registration-2020-01-01.pdf",
           |"document_type":"registration-certificate"
           |}
         """.stripMargin

      val formHeaders = Headers(
        HeaderNames.CONTENT_TYPE -> FORM,
        requestIdHeaderKey → UUID.randomUUID().toString,
        requestDateHeaderKey → mockRequestDate.toString,
        requestFromHeaderKey → mockRequestFrom)
      val formKeyForFileUpload = app.injector.instanceOf[AppConfig].Document.FormKeyForFileUpload
      val formKeyForJson = app.injector.instanceOf[AppConfig].Document.FormKeyForJson

      val mockDocumentId = UUID.randomUUID()
      val expectedDocument = Document.empty.copy(
        id = mockDocumentId,
        customerId = None,
        applicationId = Some(mockId),
        documentName = Some("my-business-registration-2020-01-01.pdf"),
        status = DocumentStatuses.fromString("ongoing"),
        documentType = DocumentTypes.fromString("registration-certificate"),
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom,
        fileUploadedAt = Option(mockRequestDate.toLocalDateTimeUTC),
        fileUploadedBy = Option(mockRequestFrom))

      val expectedInput = DocumentToCreate(
        customerId = None,
        applicationId = Some(mockId),
        fileName = Some("my-business-registration-2020-01-01.pdf"),
        documentType = DocumentTypes.RegistrationCertificate,
        documentIdentifier = None,
        purpose = "business user application requirement",
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      (documentService.upsertBusinessUserDocument(_: DocumentToCreate, _: Array[Byte], _: String, _: LocalDateTime))
        .when(expectedInput, *, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC)
        .returns(Future.successful(Right(expectedDocument)))

      val fakeRequest = FakeRequest[MultipartFormData[PlayFiles.TemporaryFile]](
        method = "POST",
        uri = s"/business_user_applications/$mockId/stage/documents",
        headers = formHeaders,
        body = MultipartFormData[PlayFiles.TemporaryFile](
          dataParts = Map(formKeyForJson → Seq(jsonPayload)),
          files = Seq(FilePart[PlayFiles.TemporaryFile](
            key = formKeyForFileUpload,
            filename = "my-business-registration-2020-01-01.pdf",
            contentType = Some("application/pdf"),
            ref = PlayFiles.SingletonTemporaryFileCreator.create("my-business-registration-2020-01-01.pdf"))),
          badParts = Seq.empty))

      val controller = inject[BusinessUserApplicationController]
      val resp = controller.createBusinessUserApplicationDocument(mockId).apply(fakeRequest)

      status(resp) mustBe OK
    }

    "return business_user_application_document json in GET /business_user_applications/{id}/stage/documents" in {
      val mockId = UUID.randomUUID()

      val mockResult = BusinessUserApplication(
        id = 1,
        uuid = mockId,
        businessName = NameAttribute("Universal Catering Co"),
        brandName = NameAttribute("Costa Coffee DSO"),
        businessCategory = BusinessCategory("Restaurants - 5812"),
        stage = ApplicationStage("identity_info"),
        status = ApplicationStatus("ongoing"),
        userTier = BusinessUserTiers.Small,
        businessType = BusinessTypes.Merchant,
        registrationNumber = RegistrationNumber("213/564654EE"),
        taxNumber = TaxNumber("A213546468977M").some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        explanation = None,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Nil,
        contactAddress = Nil,
        documents = Seq(
          Document(
            id = UUID.fromString("d23f3a0c-17ff-484e-a3bc-b5d845a09498"),
            customerId = Some(UUID.fromString("70a6d737-7e74-41a8-bc23-9c60f2c9baf4")),
            applicationId = Some(mockId),
            documentName = Some("doc1-final-final agreement.pdf"),
            documentType = DocumentTypes.MerchantAgreement,
            documentIdentifier = Some("10388273829-1983812"),
            purpose = "business user application",
            status = DocumentStatuses.Pending,
            rejectionReason = None,
            checkedBy = None,
            checkedAt = None,
            createdBy = "test user",
            createdAt = LocalDateTime.now,
            fileUploadedAt = Some(LocalDateTime.now),
            fileUploadedBy = Some("test user"),
            updatedAt = None),
          Document(
            id = UUID.fromString("17dd1f08-e295-49b2-b1cb-def506eb4707"),
            customerId = Some(UUID.fromString("70a6d737-7e74-41a8-bc23-9c60f2c9baf4")),
            applicationId = Some(mockId),
            documentName = Some("my_emirates_id01_front.png"),
            documentType = DocumentTypes.PrimaryContactId,
            documentIdentifier = Some("18932749TGA-BEGE"),
            purpose = "business user application",
            status = DocumentStatuses.Pending,
            rejectionReason = None,
            checkedBy = None,
            checkedAt = None,
            createdBy = "test user",
            createdAt = LocalDateTime.now,
            fileUploadedAt = Some(LocalDateTime.now),
            fileUploadedBy = Some("test user"),
            updatedAt = None)),
        defaultCurrency = Currency.getInstance("KES"),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = None,
        updatedAt = None)

      (businessUserApplicationManagement.getBusinessUserApplicationById _).when(mockId, Seq(Stages.Docs))
        .returns(Future.successful(mockResult.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/$endpoint/$mockId/stage/documents")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""
           |{
           |"id":"$mockId",
           |"status":"ongoing",
           |"documents":[
           |{"id":"d23f3a0c-17ff-484e-a3bc-b5d845a09498",
           |"application_id":"$mockId",
           |"filename":"doc1-final-final agreement.pdf",
           |"document_type":"merchant-agreement"},
           |{"id":"17dd1f08-e295-49b2-b1cb-def506eb4707",
           |"application_id":"$mockId",
           |"filename":"my_emirates_id01_front.png",
           |"document_type":"primary-contact-id"}
           |],
           |"created_at":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"${mockResult.createdBy}",
           |"updated_at":null,
           |"updated_by":null,
           |"submitted_by":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) mustBe expectedJson
    }

    "return 200 json in PUT /business_user_applications/{id}/cancel" in {
      val mockId = UUID.randomUUID()
      val explanation = "Cancel explanation"
      val lastUpdatedAt = now.toZonedDateTimeUTC

      val jsonRequest =
        s"""
           |{
           |"explanation": "$explanation",
           |"updated_at": "$lastUpdatedAt"
           |}
         """.stripMargin

      (businessUserApplicationManagement.cancelBusinessUserApplication _)
        .when(
          mockId,
          explanation,
          mockRequestDate.toLocalDateTimeUTC,
          mockRequestFrom,
          lastUpdatedAt.toLocalDateTimeUTC.some)
        .returns(Future.successful(Right(())))

      val fakeRequest = FakeRequest(PUT, s"/business_user_applications/$mockId/cancel",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
    }

    "return 200 json in PUT /business_user_applications/{id}/reject" in {
      val mockId = UUID.randomUUID()
      val explanation = "Reject explanation"
      val lastUpdatedAt = now.toZonedDateTimeUTC

      val jsonRequest =
        s"""
           |{
           |"explanation": "$explanation",
           |"updated_at": "$lastUpdatedAt"
           |}
         """.stripMargin

      (businessUserApplicationManagement.rejectBusinessUserApplication _)
        .when(
          mockId,
          explanation,
          mockRequestDate.toLocalDateTimeUTC,
          mockRequestFrom,
          lastUpdatedAt.toLocalDateTimeUTC.some)
        .returns(Future.successful(Right(())))

      val fakeRequest = FakeRequest(PUT, s"/business_user_applications/$mockId/reject",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
    }

    "return 200 json in PUT /business_user_applications/{id}/send_for_correction" in {
      val mockId = UUID.randomUUID()
      val explanation = "send for reaction explanation"
      val lastUpdatedAt = now.toZonedDateTimeUTC

      val jsonRequest =
        s"""
           |{
           |"explanation": "$explanation",
           |"updated_at": "$lastUpdatedAt"
           |}
         """.stripMargin

      (businessUserApplicationManagement.sendForCorrectionBusinessUserApplication _)
        .when(
          mockId,
          explanation,
          mockRequestDate.toLocalDateTimeUTC,
          mockRequestFrom,
          lastUpdatedAt.toLocalDateTimeUTC.some)
        .returns(Future.successful(Right(())))

      val fakeRequest = FakeRequest(PUT, s"/business_user_applications/$mockId/send_for_correction",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
    }

    "return 200 json in PUT /business_user_applications/{id}/submit" in {
      val mockId = UUID.randomUUID()
      val lastUpdatedAt = now.toZonedDateTimeUTC

      val jsonRequest =
        s"""
           |{
           |"updated_at": "$lastUpdatedAt"
           |}
         """.stripMargin

      (businessUserApplicationManagement.submitBusinessUserApplication _)
        .when(
          mockId,
          mockRequestDate.toLocalDateTimeUTC,
          mockRequestFrom,
          lastUpdatedAt.toLocalDateTimeUTC.some)
        .returns(Future.successful(Right(())))

      val fakeRequest = FakeRequest(PUT, s"/business_user_applications/$mockId/submit",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
    }

    "return 200 json in PUT /business_user_applications/{id}/approve" in {
      val mockId = UUID.randomUUID()
      val lastUpdatedAt = now.toZonedDateTimeUTC

      val jsonRequest =
        s"""
           |{
           |"updated_at": "$lastUpdatedAt"
           |}
         """.stripMargin

      (businessUserApplicationManagement.approveBusinessUserApplication _)
        .when(
          mockId,
          mockRequestDate.toLocalDateTimeUTC,
          mockRequestFrom,
          lastUpdatedAt.toLocalDateTimeUTC.some)
        .returns(Future.successful(Right(())))

      val fakeRequest = FakeRequest(PUT, s"/business_user_applications/$mockId/approve",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
    }
  }
}
