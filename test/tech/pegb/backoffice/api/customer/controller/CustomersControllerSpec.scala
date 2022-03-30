package tech.pegb.backoffice.api.customer.controller

import java.time.{LocalDate, LocalDateTime}
import java.util.{Currency, UUID}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.customer.Constants
import tech.pegb.backoffice.api.customer.controllers.impl.CustomersController
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.PaginatedResult
import tech.pegb.backoffice.domain.account.abstraction.AccountManagement
import tech.pegb.backoffice.domain.account.dto.AccountCriteria
import tech.pegb.backoffice.domain.account.model.Account
import tech.pegb.backoffice.domain.account.model.AccountAttributes._
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.abstraction.{CustomerRead, CustomerRegistration, CustomerUpdate, _}
import tech.pegb.backoffice.domain.customer.dto.GenericUserCriteria
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.GenericUser
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.IndividualUserType
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionManagement
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.domain.transaction.model.Transaction
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.account.Implicits._
import tech.pegb.backoffice.mapping.domain.api.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.api.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class CustomersControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  //TODO: remove when concrete implementation are finished
  val customerRead: CustomerRead = stub[CustomerRead]
  val customerUpdate: CustomerUpdate = stub[CustomerUpdate]
  val customerRegistration: CustomerRegistration = stub[CustomerRegistration]
  val customerAccount: CustomerAccount = stub[CustomerAccount]
  val txnMgmt = stub[TransactionManagement]
  val latestVersionService = stub[LatestVersionService]
  val accountMgmt: AccountManagement = stub[AccountManagement]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[LatestVersionService].to(latestVersionService),
      bind[CustomerRead].to(customerRead),
      bind[CustomerUpdate].to(customerUpdate),
      bind[CustomerRegistration].to(customerRegistration),
      bind[CustomerAccount].to(customerAccount),
      bind[TransactionManagement].to(txnMgmt),
      bind[AccountManagement].to(accountMgmt),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "BusinessUsers API" should {

    "getAccounts should return accounts base on criteria" in {
      val customerId = UUID.randomUUID()
      val id1 = UUID.randomUUID()
      val account1 = Account.getEmpty.copy(
        id = id1,
        customerId = customerId,
        accountNumber = AccountNumber("0001"),
        accountName = NameAttribute("Alice"),
        accountType = AccountType("COLLECTION"),
        isMainAccount = true,
        currency = Currency.getInstance("KES"),
        balance = BigDecimal(1000.0),
        blockedBalance = BigDecimal(50.0),
        dailyTotalTransactionAmount = None,
        lastDayBalance = None,
        accountStatus = AccountStatus("NEW"),
        lastTransactionAt = None,
        mainType = AccountMainType("liability"),
        createdAt = LocalDateTime.now(),
        createdBy = Some("user"),
        updatedBy = None,
        updatedAt = None)

      val id2 = UUID.randomUUID()

      val account2 = Account.getEmpty.copy(
        id = id2,
        customerId = customerId,
        accountNumber = AccountNumber("0002"),
        accountName = NameAttribute("Alice"),
        accountType = AccountType("DISTRIBUTION"),
        isMainAccount = false,
        currency = Currency.getInstance("PHP"),
        balance = BigDecimal(200000.0),
        blockedBalance = BigDecimal(0.0),
        dailyTotalTransactionAmount = None,
        lastDayBalance = None,
        accountStatus = AccountStatus("ACTIVE"),
        lastTransactionAt = None,
        mainType = AccountMainType("liability"),
        createdAt = LocalDateTime.now(),
        createdBy = Some("user"),
        updatedBy = None,
        updatedAt = None)

      val criteria1 = customerId.toAccountCriteria
      (customerAccount.getAccountsByCriteria _)
        .when(criteria1, Nil, None, None).returns(Future.successful(Right(Seq(account1, account2)))).noMoreThanOnce()

      val mockLatestVersion = LocalDateTime.now.toString

      (latestVersionService.getLatestVersion _).when(criteria1)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp1 = route(app, FakeRequest(GET, s"/individual_users/$customerId/accounts")).get

      status(resp1) mustBe OK
      contentAsString(resp1) mustEqual PaginatedResult(2, Seq(account1, account2).map(_.asApi), None, None).toJsonStr
      headers(resp1).get(versionHeaderKey) mustBe mockLatestVersion.toOption

      val criteria2 = (customerId, Some(true), None, None, None, None).asDomain
      (customerAccount.getAccountsByCriteria _)
        .when(criteria2, Nil, None, None).returns(Future.successful(Right(Seq(account1)))).noMoreThanOnce()
      (latestVersionService.getLatestVersion _).when(criteria2)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp2 = route(app, FakeRequest(GET, s"/individual_users/$customerId/accounts?primary_account=true")).get
      status(resp2) mustBe OK
      contentAsString(resp2) mustEqual PaginatedResult(1, Seq(account1.asApi), None, None).toJsonStr
      headers(resp2).get(versionHeaderKey) mustBe mockLatestVersion.toOption

      val criteria3 = (customerId, None, Some("PHP"), None, None, None).asDomain

      (customerAccount.getAccountsByCriteria _)
        .when(criteria3, Nil, None, None).returns(Future.successful(Right(Seq(account2)))).noMoreThanOnce()
      (latestVersionService.getLatestVersion _).when(criteria3)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp3 = route(app, FakeRequest(GET, s"/individual_users/$customerId/accounts?currency=PHP")).get
      status(resp3) mustBe OK
      contentAsString(resp3) mustEqual PaginatedResult(1, Seq(account2.asApi), None, None).toJsonStr
      headers(resp3).get(versionHeaderKey) mustBe mockLatestVersion.toOption

      val criteria4 = (customerId, None, None, Some("ACTIVE"), None, None).asDomain
      (customerAccount.getAccountsByCriteria _)
        .when(criteria4, Nil, None, None).returns(Future.successful(Right(Seq(account2)))).noMoreThanOnce()
      (latestVersionService.getLatestVersion _).when(criteria4)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp4 = route(app, FakeRequest(GET, s"/individual_users/$customerId/accounts?status=ACTIVE")).get
      status(resp4) mustBe OK
      contentAsString(resp4) mustEqual PaginatedResult(1, Seq(account2.asApi), None, None).toJsonStr
      headers(resp4).get(versionHeaderKey) mustBe mockLatestVersion.toOption

      val criteria5 = (customerId, None, None, None, Some("COLLECTION"), None).asDomain
      (customerAccount.getAccountsByCriteria _)
        .when(criteria5, Nil, None, None).returns(Future.successful(Right(Seq(account1)))).noMoreThanOnce()
      (latestVersionService.getLatestVersion _).when(criteria5)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp5 = route(app, FakeRequest(GET, s"/individual_users/$customerId/accounts?account_type=COLLECTION")).get
      status(resp5) mustBe OK
      contentAsString(resp5) mustEqual PaginatedResult(1, Seq(account1.asApi), None, None).toJsonStr
      headers(resp5).get(versionHeaderKey) mustBe mockLatestVersion.toOption

      val criteria6 = (customerId, Some(true), Some("KES"), Some("NEW"), Some("COLLECTION"), None).asDomain
      (customerAccount.getAccountsByCriteria _)
        .when(criteria6, Nil, None, None).returns(Future.successful(Right(Seq(account1)))).noMoreThanOnce()
      (latestVersionService.getLatestVersion _).when(criteria6)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp6 = route(app, FakeRequest(GET, s"/individual_users/$customerId/accounts?primary_account=true&currency=KES&status=NEW&account_type=COLLECTION")).get
      status(resp6) mustBe OK
      contentAsString(resp6) mustEqual PaginatedResult(1, Seq(account1.asApi), None, None).toJsonStr
      headers(resp6).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return transactions of a given customer and account" in {

      val customerId = UUID.randomUUID()
      val accountId = UUID.randomUUID()

      val mockTransactions = Seq(
        Transaction.getEmpty.copy(id = UUID.randomUUID().toString, primaryAccountId = accountId, sequence = 1),
        Transaction.getEmpty.copy(id = UUID.randomUUID().toString, primaryAccountId = accountId, sequence = 2),
        Transaction.getEmpty.copy(id = UUID.randomUUID().toString, primaryAccountId = accountId, sequence = 3),
        Transaction.getEmpty.copy(id = UUID.randomUUID().toString, primaryAccountId = accountId, sequence = 4),
        Transaction.getEmpty.copy(id = UUID.randomUUID().toString, primaryAccountId = accountId, sequence = 5))

      val mockCriteria = TransactionCriteria(customerId = Option(customerId.toUUIDLike), accountId = Option(accountId.toUUIDLike))

      (txnMgmt.countTransactionsByCriteria _)
        .when(mockCriteria).returns(Future.successful(Right(mockTransactions.size)))

      (txnMgmt.getTransactionsByCriteria _)
        .when(mockCriteria, *, *, *).returns(Future.successful(Right(mockTransactions)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(mockCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/customers/$customerId/accounts/$accountId/transactions")).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual PaginatedResult(total = mockTransactions.size, results = mockTransactions.map(_.asApi()), limit = None, offset = None).toJsonStr

      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "GET /customers should return paginated result" in {
      val mockMsisdn = "+971544451345"
      val mockCustomerId1 = UUID.randomUUID()
      val mockCustomerId2 = UUID.randomUUID()

      val u1 = GenericUser(
        dbUserId = 1,
        id = mockCustomerId1,
        userName = LoginUsername("user").some,
        password = "password".some,
        tier = CustomerTier("tier").some,
        segment = CustomerSegment("segment1").some,
        subscription = CustomerSubscription("customerSubscription1").some,
        email = Email("user@pegb.tech").some,
        status = CustomerStatus("new").some,
        customerType = CustomerType("individual_user").some,
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0),
        createdBy = "pegbuser",
        updatedAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0).some,
        updatedBy = "pegbuser".some,
        passwordUpdatedAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0).some,
        activatedAt = None,
        customerName = "Alice".some,

        msisdn = Msisdn(mockMsisdn).some,
        individualUserType = IndividualUserType("individualUserType").some,
        name = "Alice".some,
        fullName = "Alice Wonderland".some,
        gender = "F".some,
        personId = None,
        documentNumber = None,
        documentType = None,
        documentModel = None,
        birthDate = LocalDate.of(1992, 1, 1).some,
        birthPlace = None,
        nationality = None,
        occupation = None,
        companyName = None,
        employer = None,

        businessName = None,
        brandName = None,
        businessCategory = None,
        businessType = None,
        registrationNumber = None,
        taxNumber = None,
        registrationDate = None)

      val u2 = GenericUser(
        dbUserId = 2,
        id = mockCustomerId2,
        userName = LoginUsername("pesalink").some,
        password = "password".some,
        tier = CustomerTier("tier").some,
        segment = CustomerSegment("segment1").some,
        subscription = CustomerSubscription("customerSubscription1").some,
        email = Email("pesalink@pegb.tech").some,
        status = CustomerStatus("new").some,
        customerType = CustomerType("individual_user").some,
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0),
        createdBy = "pegbuser",
        updatedAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0).some,
        updatedBy = "pegbuser".some,
        passwordUpdatedAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0).some,
        activatedAt = None,
        customerName = "pesalink".some,

        msisdn = None,
        individualUserType = None,
        name = None,
        fullName = None,
        gender = None,
        personId = None,
        documentNumber = None,
        documentType = None,
        documentModel = None,
        birthDate = None,
        birthPlace = None,
        nationality = None,
        occupation = None,
        companyName = None,
        employer = None,

        businessName = None,
        brandName = None,
        businessCategory = None,
        businessType = None,
        registrationNumber = None,
        taxNumber = None,
        registrationDate = None)

      val criteria = GenericUserCriteria(partialMatchFields = CustomersController.userPartialMatchFields.filterNot(_ == "disabled"))
      val mockLatestVersion = LocalDateTime.now.toString
      val ordering = Seq(Ordering("type", Ordering.ASCENDING))

      (latestVersionService.getLatestVersion _).when(criteria).returns(Future.successful(Right(mockLatestVersion.some)))
      (customerRead.countUserByCriteria _).when(criteria)
        .returns(Future.successful(Right(2)))
      (customerRead.getUserByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(u1, u2))))

      val resp = route(app, FakeRequest(GET, s"/customers?order_by=customer_type")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""
          |{
          |"total":2,
          |"results":[
          |{
          |"id":"$mockCustomerId1",
          |"username":"user",
          |"tier":"tier",
          |"segment":"segment1",
          |"subscription":"customerSubscription1",
          |"email":"user@pegb.tech",
          |"status":"new",
          |"customer_type":"individual_user",
          |"created_at":"2018-01-01T00:00:00Z",
          |"created_by":"pegbuser",
          |"updated_at":"2018-01-01T00:00:00Z",
          |"updated_by":"pegbuser",
          |"activated_at":null,
          |"password_updated_at":null,
          |"customer_name":"Alice",
          |"msisdn":"+971544451345",
          |"individual_user_type":"individualUserType",
          |"alias":"Alice",
          |"full_name":"Alice Wonderland",
          |"gender":"F",
          |"person_id":null,
          |"document_number":null,
          |"document_type":null,
          |"document_model":null,
          |"birth_date":"1992-01-01",
          |"birth_place":null,
          |"nationality":null,
          |"occupation":null,
          |"company_name":null,
          |"employer":null,
          |"business_name":null,
          |"brand_name":null,
          |"business_type":null,
          |"business_category":null,
          |"registration_number":null,
          |"tax_number":null,
          |"registration_date":null
          |},
          |{
          |"id":"$mockCustomerId2",
          |"username":"pesalink",
          |"tier":"tier",
          |"segment":"segment1",
          |"subscription":"customerSubscription1",
          |"email":"pesalink@pegb.tech",
          |"status":"new",
          |"customer_type":"individual_user",
          |"created_at":"2018-01-01T00:00:00Z",
          |"created_by":"pegbuser",
          |"updated_at":"2018-01-01T00:00:00Z",
          |"updated_by":"pegbuser",
          |"activated_at":null,
          |"password_updated_at":null,
          |"customer_name":"pesalink",
          |"msisdn":null,
          |"individual_user_type":null,
          |"alias":null,
          |"full_name":null,
          |"gender":null,
          |"person_id":null,
          |"document_number":null,
          |"document_type":null,
          |"document_model":null,
          |"birth_date":null,
          |"birth_place":null,
          |"nationality":null,
          |"occupation":null,
          |"company_name":null,
          |"employer":null,
          |"business_name":null,
          |"brand_name":null,
          |"business_type":null,
          |"business_category":null,
          |"registration_number":null,
          |"tax_number":null,
          |"registration_date":null
          |}],
          |"limit":null,
          |"offset":null}""".stripMargin.trim.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "GET /customers should return paginated result filter by any_name" in {
      val mockMsisdn = "+971544451345"
      val mockCustomerId1 = UUID.randomUUID()
      val mockCustomerId2 = UUID.randomUUID()

      val u1 = GenericUser(
        dbUserId = 1,
        id = mockCustomerId1,
        userName = LoginUsername("user").some,
        password = "password".some,
        tier = CustomerTier("tier").some,
        segment = CustomerSegment("segment1").some,
        subscription = CustomerSubscription("customerSubscription1").some,
        email = Email("user@pegb.tech").some,
        status = CustomerStatus("new").some,
        customerType = CustomerType("individual_user").some,
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0),
        createdBy = "pegbuser",
        updatedAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0).some,
        updatedBy = "pegbuser".some,
        passwordUpdatedAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0).some,
        activatedAt = None,
        customerName = "Alice".some,

        msisdn = Msisdn(mockMsisdn).some,
        individualUserType = IndividualUserType("individualUserType").some,
        name = "Alice".some,
        fullName = "Alice Pesalink".some,
        gender = "F".some,
        personId = None,
        documentNumber = None,
        documentType = None,
        documentModel = None,
        birthDate = LocalDate.of(1992, 1, 1).some,
        birthPlace = None,
        nationality = None,
        occupation = None,
        companyName = None,
        employer = None,

        businessName = None,
        brandName = None,
        businessCategory = None,
        businessType = None,
        registrationNumber = None,
        taxNumber = None,
        registrationDate = None)

      val u2 = GenericUser(
        dbUserId = 2,
        id = mockCustomerId2,
        userName = LoginUsername("pesalink").some,
        password = "password".some,
        tier = CustomerTier("tier").some,
        segment = CustomerSegment("segment1").some,
        subscription = CustomerSubscription("customerSubscription1").some,
        email = Email("pesalink@pegb.tech").some,
        status = CustomerStatus("new").some,
        customerType = CustomerType("individual_user").some,
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0),
        createdBy = "pegbuser",
        updatedAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0).some,
        updatedBy = "pegbuser".some,
        passwordUpdatedAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0).some,
        activatedAt = None,
        customerName = "pesalink".some,

        msisdn = None,
        individualUserType = None,
        name = None,
        fullName = None,
        gender = None,
        personId = None,
        documentNumber = None,
        documentType = None,
        documentModel = None,
        birthDate = None,
        birthPlace = None,
        nationality = None,
        occupation = None,
        companyName = None,
        employer = None,

        businessName = None,
        brandName = None,
        businessCategory = None,
        businessType = None,
        registrationNumber = None,
        taxNumber = None,
        registrationDate = None)

      val criteria = GenericUserCriteria(
        anyName = NameAttribute("pesa").some,
        partialMatchFields = CustomersController.userPartialMatchFields.filterNot(_ == "disabled"))
      val mockLatestVersion = LocalDateTime.now.toString

      (latestVersionService.getLatestVersion _).when(criteria).returns(Future.successful(Right(mockLatestVersion.some)))
      (customerRead.countUserByCriteria _).when(criteria)
        .returns(Future.successful(Right(2)))
      (customerRead.getUserByCriteria _).when(criteria, Nil, None, None)
        .returns(Future.successful(Right(Seq(u1, u2))))

      val resp = route(app, FakeRequest(GET, s"/customers?any_name=pesa")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""
           |{
           |"total":2,
           |"results":[
           |{
           |"id":"$mockCustomerId1",
           |"username":"user",
           |"tier":"tier",
           |"segment":"segment1",
           |"subscription":"customerSubscription1",
           |"email":"user@pegb.tech",
           |"status":"new",
           |"customer_type":"individual_user",
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"pegbuser",
           |"updated_at":"2018-01-01T00:00:00Z",
           |"updated_by":"pegbuser",
           |"activated_at":null,
           |"password_updated_at":null,
           |"customer_name":"Alice",
           |"msisdn":"+971544451345",
           |"individual_user_type":"individualUserType",
           |"alias":"Alice",
           |"full_name":"Alice Pesalink",
           |"gender":"F",
           |"person_id":null,
           |"document_number":null,
           |"document_type":null,
           |"document_model":null,
           |"birth_date":"1992-01-01",
           |"birth_place":null,
           |"nationality":null,
           |"occupation":null,
           |"company_name":null,
           |"employer":null,
           |"business_name":null,
           |"brand_name":null,
           |"business_type":null,
           |"business_category":null,
           |"registration_number":null,
           |"tax_number":null,
           |"registration_date":null
           |},
           |{
           |"id":"$mockCustomerId2",
           |"username":"pesalink",
           |"tier":"tier",
           |"segment":"segment1",
           |"subscription":"customerSubscription1",
           |"email":"pesalink@pegb.tech",
           |"status":"new",
           |"customer_type":"individual_user",
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"pegbuser",
           |"updated_at":"2018-01-01T00:00:00Z",
           |"updated_by":"pegbuser",
           |"activated_at":null,
           |"password_updated_at":null,
           |"customer_name":"pesalink",
           |"msisdn":null,
           |"individual_user_type":null,
           |"alias":null,
           |"full_name":null,
           |"gender":null,
           |"person_id":null,
           |"document_number":null,
           |"document_type":null,
           |"document_model":null,
           |"birth_date":null,
           |"birth_place":null,
           |"nationality":null,
           |"occupation":null,
           |"company_name":null,
           |"employer":null,
           |"business_name":null,
           |"brand_name":null,
           |"business_type":null,
           |"business_category":null,
           |"registration_number":null,
           |"tax_number":null,
           |"registration_date":null
           |}],
           |"limit":null,
           |"offset":null}""".stripMargin.trim.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "GET /customers/:id return single generic user" in {
      val mockMsisdn = "+971544451345"
      val mockCustomerId1 = UUID.randomUUID()

      val u1 = GenericUser(
        dbUserId = 1,
        id = mockCustomerId1,
        userName = LoginUsername("user").some,
        password = "password".some,
        tier = CustomerTier("tier").some,
        segment = CustomerSegment("segment1").some,
        subscription = CustomerSubscription("customerSubscription1").some,
        email = Email("user@pegb.tech").some,
        status = CustomerStatus("new").some,
        customerType = CustomerType("individual_user").some,
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0),
        createdBy = "pegbuser",
        updatedAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0).some,
        updatedBy = "pegbuser".some,
        passwordUpdatedAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0).some,
        activatedAt = None,
        customerName = "Alice".some,

        msisdn = Msisdn(mockMsisdn).some,
        individualUserType = IndividualUserType("individualUserType").some,
        name = "Alice".some,
        fullName = "Alice Pesalink".some,
        gender = "F".some,
        personId = None,
        documentNumber = None,
        documentType = None,
        documentModel = None,
        birthDate = LocalDate.of(1992, 1, 1).some,
        birthPlace = None,
        nationality = None,
        occupation = None,
        companyName = None,
        employer = None,

        businessName = None,
        brandName = None,
        businessCategory = None,
        businessType = None,
        registrationNumber = None,
        taxNumber = None,
        registrationDate = None)

      val criteria = GenericUserCriteria(
        anyName = NameAttribute("pesa").some,
        partialMatchFields = CustomersController.userPartialMatchFields.filterNot(_ == "disabled"))
      val mockLatestVersion = LocalDateTime.now.toString

      (customerRead.getUser _).when(mockCustomerId1)
        .returns(Future.successful(Right(u1)))

      val resp = route(app, FakeRequest(GET, s"/customers/$mockCustomerId1")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""
           |{
           |"id":"$mockCustomerId1",
           |"username":"user",
           |"tier":"tier",
           |"segment":"segment1",
           |"subscription":"customerSubscription1",
           |"email":"user@pegb.tech",
           |"status":"new",
           |"customer_type":"individual_user",
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"pegbuser",
           |"updated_at":"2018-01-01T00:00:00Z",
           |"updated_by":"pegbuser",
           |"activated_at":null,
           |"password_updated_at":null,
           |"customer_name":"Alice",
           |"msisdn":"+971544451345",
           |"individual_user_type":"individualUserType",
           |"alias":"Alice",
           |"full_name":"Alice Pesalink",
           |"gender":"F",
           |"person_id":null,
           |"document_number":null,
           |"document_type":null,
           |"document_model":null,
           |"birth_date":"1992-01-01",
           |"birth_place":null,
           |"nationality":null,
           |"occupation":null,
           |"company_name":null,
           |"employer":null,
           |"business_name":null,
           |"brand_name":null,
           |"business_type":null,
           |"business_category":null,
           |"registration_number":null,
           |"tax_number":null,
           |"registration_date":null
           |}""".stripMargin.trim.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

  }

  "CustomerControllers proxy to AccountControllers" should {

    val customerId = UUID.randomUUID()
    val id1 = UUID.randomUUID()
    val account1 = Account.getEmpty.copy(
      id = id1,
      customerId = customerId,
      accountNumber = AccountNumber("0001"),
      accountName = NameAttribute("Alice"),
      accountType = AccountType("COLLECTION"),
      isMainAccount = true,
      currency = Currency.getInstance("KES"),
      balance = BigDecimal(1000.0),
      blockedBalance = BigDecimal(50.0),
      dailyTotalTransactionAmount = None,
      lastDayBalance = None,
      accountStatus = AccountStatus("NEW"),
      lastTransactionAt = None,
      mainType = AccountMainType("liability"),
      createdAt = LocalDateTime.now(),
      createdBy = Some("SuperUser"),
      updatedBy = None,
      updatedAt = None)

    val id2 = UUID.randomUUID()
    val account2 = Account.getEmpty.copy(
      id = id2,
      customerId = customerId,
      accountNumber = AccountNumber("0002"),
      accountName = NameAttribute("Bob"),
      accountType = AccountType("DISTRIBUTION"),
      isMainAccount = false,
      currency = Currency.getInstance("PHP"),
      balance = BigDecimal(200000.0),
      blockedBalance = BigDecimal(0.0),
      dailyTotalTransactionAmount = None,
      lastDayBalance = None,
      accountStatus = AccountStatus("ACTIVE"),
      lastTransactionAt = None,
      mainType = AccountMainType("liability"),
      createdAt = LocalDateTime.now(),
      createdBy = Some("SuperUser"),
      updatedBy = None,
      updatedAt = None)

    "return list of accounts of a customer in GET /customer/:id/accounts" in {
      val emptyCriteria = AccountCriteria(UUIDLike(customerId.toString).some, None, None, None, None, None,
        partialMatchFields = Constants.validAccountsPartialMatchFields.filterNot(_ == "disabled"))
      (accountMgmt.countAccountsByCriteria _)
        .when(emptyCriteria).returns(Future.successful(Right(2))).noMoreThanOnce()
      (accountMgmt.getAccountsByCriteria _)
        .when(emptyCriteria, Seq(Ordering("number", Ordering.ASCENDING)), None, None).returns(Future.successful(Right(Seq(account1, account2)))).noMoreThanOnce()
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(emptyCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/customers/$customerId/accounts?order_by=number")).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual PaginatedResult(total = 2, results = Seq(account1, account2).map(_.asApi), limit = None, offset = None).toJsonStr

      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption

    }

    "activate account status" in {
      val jsonRequest =
        """{"updated_at":"2019-12-11T13:32:21.420Z"}"""
      val expected = account2.copy(accountStatus = AccountStatus(Account.ACTIVE))
      (accountMgmt.activateAccount _)
        .when(account2.id, *, *, *)
        .returns(Future.successful(Right(expected))).noMoreThanOnce()

      val resp = route(
        app,
        FakeRequest(PUT, s"/customers/$customerId/accounts/${account2.id.toString}/activate", jsonHeaders, jsonRequest)).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual Option(expected).map(_.asApi).toJsonStr
    }

    "close account status" in {
      val jsonRequest =
        """{"updated_at":"2019-12-11T13:32:21.420Z"}"""

      val expected = account2.copy(accountStatus = AccountStatus(Account.CLOSED))
      (accountMgmt.deleteAccount _)
        .when(account2.id, *, *, *)
        .returns(Future.successful(Right(expected))).noMoreThanOnce()

      val resp = route(
        app,
        FakeRequest(DELETE, s"/customers/$customerId/accounts/${account2.id.toString}", jsonHeaders, jsonRequest)).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual Option(expected).map(_.asApi).toJsonStr
    }

    "deactivate account status" in {

      val jsonRequest =
        """{"updated_at":"2019-12-11T13:32:21.420Z"}"""
      val expected = account2.copy(accountStatus = AccountStatus(Account.BLOCKED))
      (accountMgmt.blockAccount _)
        .when(account2.id, *, *, *)
        .returns(Future.successful(Right(expected))).noMoreThanOnce()

      val resp = route(
        app,
        FakeRequest(PUT, s"/customers/$customerId/accounts/${account2.id.toString}/deactivate", jsonHeaders, jsonRequest)).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual Option(expected).map(_.asApi).toJsonStr
    }

  }

}
