package tech.pegb.backoffice.api.customer.controller

import java.time._
import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.{Binding, bind}
import play.api.mvc.Headers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import tech.pegb.backoffice.api.customer.dto.IndividualUserToUpdate
import tech.pegb.backoffice.domain.account.model.AccountAttributes._
import tech.pegb.backoffice.domain.application.model
import tech.pegb.backoffice.domain.application.model.ApplicationStatus
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.abstraction._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.{IndividualUser, IndividualUserType}
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}
import tech.pegb.core.{PegBTestApp, TestExecutionContext}
import tech.pegb.backoffice.mapping.api.domain.customer.Implicits._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.customer.dto.{IndividualUserToUpdate ⇒ DomainIndividualUserToUpdate}
import tech.pegb.backoffice.domain.account.dto.AccountToCreate
import tech.pegb.backoffice.domain.account.model.Account
import tech.pegb.backoffice.domain.customer.dto.IndividualUserCriteria
import tech.pegb.backoffice.api.customer.Constants
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.dto.{DocumentCriteria, DocumentToApprove, DocumentToReject}
import tech.pegb.backoffice.domain.document.model.{Document, DocumentStatuses, DocumentTypes}
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.account.Implicits._

import scala.concurrent.Future

class IndividualUserControllerSpec extends PlaySpec with PegBTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  private val customerRead = stub[CustomerRead]
  private val customerActivation = stub[CustomerActivation]
  private val customerUpdate = stub[CustomerUpdate]
  private val customerAccount = stub[CustomerAccount]
  private val documentManagement = stub[DocumentManagement]
  private val mockedWalletApplication = stub[CustomerWalletApplication]
  private val latestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[CustomerRead].to(customerRead),
      bind[CustomerActivation].to(customerActivation),
      bind[CustomerUpdate].to(customerUpdate),
      bind[CustomerAccount].to(customerAccount),
      bind[DocumentManagement].to(documentManagement),
      bind[CustomerWalletApplication].to(mockedWalletApplication),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  private val mockMsisdn = "+971544451345"
  private val mockCustomerId1 = UUID.randomUUID()
  private val individualUser = IndividualUser(
    uniqueId = "1",
    id = mockCustomerId1,
    userName = Some(LoginUsername("user")),
    password = Some("password"),
    tier = Some(CustomerTier("tier")),
    segment = Some(CustomerSegment("segment1")),
    subscription = Some(CustomerSubscription("customerSubscription1")),
    email = Some(Email("user@pegb.tech")),
    status = CustomerStatus("new"),

    msisdn = Msisdn(mockMsisdn),
    individualUserType = Some(IndividualUserType("individualUserType")),
    name = Some("Alice"),
    fullName = Some("Alice Wonderland"),
    gender = Some("F"),
    personId = None,
    documentNumber = None,
    documentModel = None,
    birthDate = Some(LocalDate.of(1992, 1, 1)),
    birthPlace = None,
    nationality = None,
    occupation = None,
    companyName = None,
    employer = None,
    createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0),
    createdBy = Option("pegbuser"),
    updatedAt = Option(LocalDateTime.of(2018, 1, 1, 0, 0, 0)),
    updatedBy = Option("pegbuser"),
    activatedAt = None)

  private val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  private val mockCustomerId2 = UUID.randomUUID()

  override protected lazy val mockRequestDate: ZonedDateTime = ZonedDateTime.now(mockClock)
  override protected lazy val mockRequestFrom = "Admin"

  private val mockRequestLocalDate: LocalDateTime = mockRequestDate.toLocalDateTimeUTC
  private val baseUrl1 = s"/individual_users/$mockCustomerId1"
  private val baseUrl2 = s"/individual_users/$mockCustomerId2"

  "IndividualUser API" should {
    val doneAt = mockRequestLocalDate
    val doneBy = mockRequestFrom //"TEST_USERNAME"
    "return individual user in getIndividualUser" in {
      val expected = Right(individualUser.copy(id = mockCustomerId2))
      (customerRead.getIndividualUser _)
        .when(mockCustomerId2).returns(Future.successful(expected))

      val resp = route(app, FakeRequest(GET, baseUrl2)
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK
      val expectedJsonResponse =
        s"""
           |{"id":"${mockCustomerId2.toString}",
           |"username":"user",
           |"tier":"tier",
           |"segment":"segment1",
           |"subscription":"customerSubscription1",
           |"email":"user@pegb.tech",
           |"status":"new",
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
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"pegbuser",
           |"updated_at":"2018-01-01T00:00:00Z",
           |"updated_by":"pegbuser",
           |"activated_at":null}""".stripMargin.replaceAll("\n", "")
      contentAsString(resp) mustBe expectedJsonResponse
    }

    "return activated individual user after activateIndividualUser" in {
      val activeStatus = conf.getOptional[String]("business-user.activated-status").get
      val expected = Right(individualUser.copy(status = CustomerStatus(activeStatus)))
      //val doneAt = ZonedDateTime.now(mockClock)

      (customerActivation.activateIndividualUser _)
        .when(mockCustomerId1, doneBy, doneAt).returns(Future.successful(expected))

      val resp = route(app, FakeRequest(PUT, baseUrl1 + "/activate")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK
      val expectedJsonResponse =
        s"""
           |{"id":"${individualUser.id.toString}",
           |"username":"user",
           |"tier":"tier",
           |"segment":"segment1",
           |"subscription":"customerSubscription1",
           |"email":"user@pegb.tech",
           |"status":"$activeStatus",
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
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"pegbuser",
           |"updated_at":"2018-01-01T00:00:00Z",
           |"updated_by":"pegbuser",
           |"activated_at":null}
           |""".stripMargin.replaceAll("\n", "")
      contentAsString(resp) mustBe expectedJsonResponse
    }
    "return deactivated individual user after deactivateIndividualUser" in {
      val expected = Right(individualUser)
      //val doneAt = ZonedDateTime.now(mockClock)

      (customerActivation.deactivateIndividualUser _)
        .when(mockCustomerId1, doneBy, doneAt).returns(Future.successful(expected))

      val resp = route(app, FakeRequest(DELETE, baseUrl1)
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK
      val expectedJsonResponse =
        s"""
           |{"id":"${individualUser.id.toString}",
           |"username":"user",
           |"tier":"tier",
           |"segment":"segment1",
           |"subscription":"customerSubscription1",
           |"email":"user@pegb.tech",
           |"status":"new",
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
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"pegbuser",
           |"updated_at":"2018-01-01T00:00:00Z",
           |"updated_by":"pegbuser",
           |"activated_at":null}
           |""".stripMargin.replaceAll("\n", "")
      contentAsString(resp) mustBe expectedJsonResponse
    }

    "return updated individual user after updateIndividualUser" in {
      val mockCustomerId = UUID.randomUUID()
      val mockIndividualUserToUpdate = IndividualUserToUpdate(msisdn = "971544451679")
      val expectedUpdatedIndividualUser = IndividualUser.getEmpty.copy(id = mockCustomerId, msisdn = Msisdn(mockIndividualUserToUpdate.msisdn), createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0))
      val mockBackofficeUserWhoUpdated = doneBy
      val timeUpdated = doneAt

      (customerUpdate.updateIndividualUser(_: UUID, _: DomainIndividualUserToUpdate, _: String, _: LocalDateTime))
        .when(mockCustomerId, mockIndividualUserToUpdate.asDomain.get, mockBackofficeUserWhoUpdated, timeUpdated)
        .returns(Future.successful(Right(expectedUpdatedIndividualUser)))

      val resp = route(app, FakeRequest(PUT, s"/individual_users/${mockCustomerId.toString}")
        .withBody(mockIndividualUserToUpdate.toJsonStr)
        .withHeaders(jsonHeaders)).get

      // val createdTime = expectedUpdatedIndividualUser.createdAt
      status(resp) mustBe OK
      val expectedJsonResponse =
        s"""
           |{"id":"${mockCustomerId.toString}",
           |"username":null,
           |"tier":null,
           |"segment":null,
           |"subscription":null,
           |"email":null,
           |"status":"some status",
           |"msisdn":"971544451679",
           |"individual_user_type":null,
           |"alias":"",
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
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"UNKNOWN",
           |"updated_at":null,
           |"updated_by":null,
           |"activated_at":null}""".stripMargin.replaceAll("\n", "")
      contentAsString(resp) mustBe expectedJsonResponse
    }

    "return an individual user's accounts in getIndividualUserAccounts" in {

      val mockCustomerId = UUID.randomUUID()
      val acct1 = UUID.randomUUID()
      val acct2 = UUID.randomUUID()
      val acct3 = UUID.randomUUID()
      val createdTime = LocalDateTime.of(2018, 1, 1, 0, 0, 0)
      val expected = Seq(
        Account.getEmpty.copy(id = acct1, customerId = mockCustomerId, currency = Currency.getInstance("USD"), createdAt = createdTime),
        Account.getEmpty.copy(id = acct2, customerId = mockCustomerId, currency = Currency.getInstance("AED"), createdAt = createdTime),
        Account.getEmpty.copy(id = acct3, customerId = mockCustomerId, currency = Currency.getInstance("KES"), createdAt = createdTime))

      val criteria = (mockCustomerId, None, None, None, None, None).asDomain

      (customerAccount.getAccountsByCriteria _)
        .when(criteria, Nil, None, None)
        .returns(Future.successful(Right(expected)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/individual_users/${mockCustomerId}/accounts")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      // val doneAtExpected = s"[${createdTime.getYear},${createdTime.getMonthValue},${createdTime.getDayOfMonth},${createdTime.getHour},${createdTime.getMinute},${createdTime.getSecond},${createdTime.getNano}]"
      val expectedJsonResponse =
        s"""{
           |"total":3,
           |"results":[
           |{
           |"id":"${acct1.toString}",
           |"customer_id":"${mockCustomerId.toString}",
           |"customer_name":null,
           |"customer_full_name":"",
           |"msisdn":"",
           |"number":"account number",
           |"name":"account name",
           |"type":"account type",
           |"is_main_account":true,
           |"currency":"USD",
           |"balance":0,
           |"blocked_balance":0,
           |"available_balance":0.00,
           |"status":"status",
           |"last_transaction_at":null,
           |"main_type":"liability",
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"",
           |"updated_by":null,
           |"updated_at":null
           |},
           |{
           |"id":"${acct2.toString}",
           |"customer_id":"${mockCustomerId.toString}",
           |"customer_name":null,
           |"customer_full_name":"",
           |"msisdn":"",
           |"number":"account number",
           |"name":"account name",
           |"type":"account type",
           |"is_main_account":true,
           |"currency":"AED",
           |"balance":0,
           |"blocked_balance":0,
           |"available_balance":0.00,
           |"status":"status",
           |"last_transaction_at":null,
           |"main_type":"liability",
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"",
           |"updated_by":null,
           |"updated_at":null
           |},
           |{
           |"id":"${acct3.toString}",
           |"customer_id":"${mockCustomerId.toString}",
           |"customer_name":null,
           |"customer_full_name":"",
           |"msisdn":"",
           |"number":"account number",
           |"name":"account name",
           |"type":"account type",
           |"is_main_account":true,
           |"currency":"KES",
           |"balance":0,
           |"blocked_balance":0,
           |"available_balance":0.00,
           |"status":"status",
           |"last_transaction_at":null,
           |"main_type":"liability",
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"",
           |"updated_by":null,
           |"updated_at":null
           |}],
           |"limit":null,
           |"offset":null
           |}
           |""".stripMargin.replaceAll("\n", "")
      contentAsString(resp) mustBe expectedJsonResponse

      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return a specific account of an individual user in getIndividualUserAccount" in {

      val mockCustomerId = UUID.randomUUID()
      val mockAccountId = UUID.randomUUID()
      val createdTime = LocalDateTime.of(2018, 1, 1, 0, 0, 0)
      val expected = Seq(
        Account.getEmpty.copy(customerId = mockCustomerId, currency = Currency.getInstance("USD"), createdAt = createdTime),
        Account.getEmpty.copy(customerId = mockCustomerId, currency = Currency.getInstance("AED"), createdAt = createdTime),
        Account.getEmpty.copy(id = mockAccountId, customerId = mockCustomerId, currency = Currency.getInstance("KES"), createdAt = createdTime))

      (customerAccount.getAccountsByCriteria _)
        .when((mockCustomerId, None, None, None, None, None).asDomain, Nil, None, None)
        .returns(Future.successful(Right(expected)))

      val resp = route(app, FakeRequest(GET, s"/individual_users/${mockCustomerId}/accounts/${mockAccountId}")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK
      val expectedJsonResponse =
        s"""{
           |"id":"${mockAccountId.toString}",
           |"customer_id":"${mockCustomerId.toString}",
           |"customer_name":null,
           |"customer_full_name":"",
           |"msisdn":"",
           |"number":"account number",
           |"name":"account name",
           |"type":"account type",
           |"is_main_account":true,
           |"currency":"KES",
           |"balance":0,
           |"blocked_balance":0,
           |"available_balance":0.00,
           |"status":"status",
           |"last_transaction_at":null,
           |"main_type":"liability",
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"",
           |"updated_by":null,
           |"updated_at":null}""".stripMargin.replaceAll("\n", "")
      contentAsString(resp) mustBe expectedJsonResponse
    }

    "activate an inactive account in activateIndividualUserAccount" in {

      val mockCustomerId = UUID.randomUUID()
      val mockAccountId = UUID.randomUUID()
      val activatedAt = doneAt //.toLocalDateTimeUTC
      val expectedActiveAccount = Account.getEmpty.copy(
        id = mockAccountId,
        customerId = mockCustomerId,
        currency = Currency.getInstance("USD"), accountStatus = AccountStatus("active"),
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0))
      (customerAccount.activateIndividualUserAccount(_: UUID, _: UUID, _: String, _: LocalDateTime))
        .when(mockCustomerId, mockAccountId, doneBy, activatedAt)
        .returns(Future.successful(Right(expectedActiveAccount)))

      val resp = route(app, FakeRequest(PUT, s"/individual_users/${mockCustomerId}/accounts/${mockAccountId}/activate")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK
      val expectedJsonResponse =
        s"""{
           |"id":"${mockAccountId.toString}",
           |"customer_id":"${mockCustomerId.toString}",
           |"customer_name":null,
           |"customer_full_name":"",
           |"msisdn":"",
           |"number":"account number",
           |"name":"account name",
           |"type":"account type",
           |"is_main_account":true,
           |"currency":"USD",
           |"balance":0,
           |"blocked_balance":0,
           |"available_balance":0.00,
           |"status":"active",
           |"last_transaction_at":null,
           |"main_type":"liability",
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"",
           |"updated_by":null,
           |"updated_at":null
           |}""".stripMargin.replaceAll("\n", "")
      contentAsString(resp) mustBe expectedJsonResponse
    }

    "activateIndividualUserAccount should fail if account is already closed" in {

      val mockCustomerId = UUID.randomUUID()
      val mockAccountId = UUID.randomUUID()
      val deactivatedAt = doneAt

      val expectedError = ServiceError.validationError(
        s"Unable to activate account $mockAccountId of customer $mockCustomerId because account status is already closed.",
        UUID.randomUUID().toOption)

      (customerAccount.activateIndividualUserAccount(_: UUID, _: UUID, _: String, _: LocalDateTime))
        .when(mockCustomerId, mockAccountId, doneBy, deactivatedAt)
        .returns(Future.successful(Left(expectedError)))

      val resp = route(app, FakeRequest(PUT, s"/individual_users/$mockCustomerId/accounts/$mockAccountId/activate")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe BAD_REQUEST

      contentAsString(resp).contains(expectedError.message) mustBe true
    }

    "deactivate an active account in deactivateIndividualUserAccount" in {

      val mockCustomerId = UUID.randomUUID()
      val mockAccountId = UUID.randomUUID()
      val activatedAt = doneAt
      val expectedDeactivetedAccount = Account.getEmpty.copy(
        id = mockAccountId,
        customerId = mockCustomerId,
        currency = Currency.getInstance("USD"), accountStatus = AccountStatus("deactivated"),
        mainType = AccountMainType("asset"),
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0))

      (customerAccount.deactivateIndividualUserAccount(_: UUID, _: UUID, _: String, _: LocalDateTime))
        .when(mockCustomerId, mockAccountId, doneBy, activatedAt)
        .returns(Future.successful(Right(expectedDeactivetedAccount)))

      val resp = route(app, FakeRequest(PUT, s"/individual_users/$mockCustomerId/accounts/$mockAccountId/deactivate")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK
      val expectedJsonResponse =
        s"""{
           |"id":"${mockAccountId.toString}",
           |"customer_id":"${mockCustomerId.toString}",
           |"customer_name":null,
           |"customer_full_name":"",
           |"msisdn":"",
           |"number":"account number",
           |"name":"account name",
           |"type":"account type",
           |"is_main_account":true,
           |"currency":"USD",
           |"balance":0,
           |"blocked_balance":0,
           |"available_balance":0.00,
           |"status":"deactivated",
           |"last_transaction_at":null,
           |"main_type":"asset",
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"",
           |"updated_by":null,
           |"updated_at":null
           |}""".stripMargin.replaceAll("\n", "")
      contentAsString(resp) mustBe expectedJsonResponse
    }

    "deactivateIndividualUserAccount should fail if account is not active" in {

      val mockCustomerId = UUID.randomUUID()
      val mockAccountId = UUID.randomUUID()
      val deactivatedAt = doneAt

      val expectedError = ServiceError.validationError(
        s"Unable to deactivate account $mockAccountId of customer $mockCustomerId because account status is closed.",
        UUID.randomUUID().toOption)

      (customerAccount.deactivateIndividualUserAccount(_: UUID, _: UUID, _: String, _: LocalDateTime))
        .when(mockCustomerId, mockAccountId, doneBy, deactivatedAt)
        .returns(Future.successful(Left(expectedError)))

      val resp = route(app, FakeRequest(PUT, s"/individual_users/$mockCustomerId/accounts/$mockAccountId/deactivate")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe BAD_REQUEST

      contentAsString(resp).contains(expectedError.message) mustBe true
    }

    "close an active account if no balance remain in closeIndividualUserAccount" in {

      val mockCustomerId = UUID.randomUUID()
      val mockAccountId = UUID.randomUUID()
      val closedAt = doneAt //.toLocalDateTimeUTC
      val expectedClosedAccount = Account.getEmpty.copy(
        id = mockAccountId,
        customerId = mockCustomerId,
        currency = Currency.getInstance("USD"), accountStatus = AccountStatus("closed"),
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0))
      (customerAccount.closeIndividualUserAccount(_: UUID, _: UUID, _: String, _: LocalDateTime))
        .when(mockCustomerId, mockAccountId, doneBy, closedAt)
        .returns(Future.successful(Right(expectedClosedAccount)))

      val resp = route(app, FakeRequest(DELETE, s"/individual_users/${mockCustomerId}/accounts/${mockAccountId}")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val expectedJsonResponse =
        s"""
           |{"id":"${mockAccountId.toString}",
           |"customer_id":"${mockCustomerId.toString}",
           |"customer_name":null,
           |"customer_full_name":"",
           |"msisdn":"",
           |"number":"account number",
           |"name":"account name",
           |"type":"account type",
           |"is_main_account":true,
           |"currency":"USD",
           |"balance":0,
           |"blocked_balance":0,
           |"available_balance":0.00,
           |"status":"closed",
           |"last_transaction_at":null,
           |"main_type":"liability",
           |"created_at":"2018-01-01T00:00:00Z",
           |"created_by":"",
           |"updated_by":null,
           |"updated_at":null}""".stripMargin.replaceAll("\n", "")
      contentAsString(resp) mustBe expectedJsonResponse
    }

    "closeIndividualUserAccount should fail if account has remaining balance" in {

      val mockCustomerId = UUID.randomUUID()
      val mockAccountId = UUID.randomUUID()
      val deactivatedAt = doneAt

      val expectedError = ServiceError.validationError(
        s"Unable to close account $mockAccountId of customer $mockCustomerId because account still has remaining balance of USD 100",
        UUID.randomUUID().toOption)

      (customerAccount.closeIndividualUserAccount(_: UUID, _: UUID, _: String, _: LocalDateTime))
        .when(mockCustomerId, mockAccountId, doneBy, deactivatedAt)
        .returns(Future.successful(Left(expectedError)))

      val resp = route(app, FakeRequest(DELETE, s"/individual_users/$mockCustomerId/accounts/$mockAccountId")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe BAD_REQUEST

      contentAsString(resp).contains(expectedError.message) mustBe true
    }

  }

  "create account if correct AccountToCreate js format" in {

    val customerId = UUID.randomUUID()
    val doneBy = mockRequestFrom //backOfficeUser.userName
    val doneAt = mockRequestLocalDate
    val domainAccountToCreate = AccountToCreate(
      customerId = customerId,
      accountNumber = None,
      accountName = None,
      accountType = AccountType("standard_wallet"),
      isMainAccount = false,
      currency = Currency.getInstance("USD"),
      initialBalance = None,
      accountStatus = None,
      mainType = AccountMainType("liability"),
      createdBy = doneBy,
      createdAt = doneAt /*LocalDateTime.of(2018, 1, 1, 0, 0, 0)*/ )

    val accountUUID = UUID.randomUUID()
    val accountResult = Account.getEmpty.copy(
      id = accountUUID,
      customerId = customerId,
      accountNumber = AccountNumber("1.0"),
      accountName = NameAttribute("+971544451986_standard_wallet"),
      accountType = AccountType("standard_wallet"),
      isMainAccount = false,
      currency = Currency.getInstance("USD"),
      balance = BigDecimal(0),
      blockedBalance = BigDecimal(0),
      dailyTotalTransactionAmount = None,
      lastDayBalance = None,
      accountStatus = AccountStatus("active"),
      lastTransactionAt = None,
      mainType = AccountMainType("liability"),
      createdAt = doneAt,
      createdBy = Some(doneBy),
      updatedBy = None,
      updatedAt = None)

    (customerAccount.openIndividualUserAccount _).when(customerId, domainAccountToCreate).returns(Future.successful(Right(accountResult)))

    val jsonRequest =
      s"""
         |{"type":"standard_wallet",
         |"currency":"USD"}
        """.stripMargin.trim.replaceAll("\n", "")

    val fakeRequest = FakeRequest(POST, s"/individual_users/${customerId.toString}/accounts")
      .withBody(jsonRequest)
      .withHeaders(jsonHeaders)

    val resp = route(app, fakeRequest).get

    status(resp) mustBe CREATED
    val expectedJsonResponse =
      s"""
         |{"id":"${accountUUID.toString}",
         |"customer_id":"$customerId",
         |"customer_name":null,
         |"customer_full_name":"",
         |"msisdn":"",
         |"number":"1.0",
         |"name":"+971544451986_standard_wallet",
         |"type":"standard_wallet",
         |"is_main_account":false,
         |"currency":"USD",
         |"balance":0,
         |"blocked_balance":0,
         |"available_balance":0.00,
         |"status":"active",
         |"last_transaction_at":null,
         |"main_type":"liability",
         |"created_at":"${doneAt.toZonedDateTimeUTC}",
         |"created_by":"$doneBy",
         |"updated_by":null,
         |"updated_at":null
         |}""".stripMargin.replaceAll("\n", "")
    contentAsString(resp) mustBe expectedJsonResponse
  }

  "create account should fail if accountToCreate json is missing something" in {

    val customerId = UUID.randomUUID()
    // val createTime = LocalDateTime.now(mockClock)
    // val doneBy = mockRequestFrom //backOfficeUser.userName

    val jsonRequest =
      s"""{"number":"001",
         |"name":"USD ACCOUNT",
         |"type":"wallet_account",
         |"is_main_account":"false",
         |"initial_balance": 10000}
        """.stripMargin

    val fakeRequest = FakeRequest(POST, s"/individual_users/${customerId.toString}/accounts")
      .withBody(jsonRequest)
      .withHeaders(jsonHeaders)

    val resp = route(app, fakeRequest).get

    status(resp) mustBe BAD_REQUEST

    val expectedResponse =
      s"""
         |{
         |"id":"${mockRequestId}",
         |"code":"MalformedRequest",
         |"msg":"Malformed request to create individual_user account. Mandatory field is missing or value of a field is of wrong type."
         |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    contentAsString(resp) mustBe expectedResponse
  }

  "get individual users by criteria should return paginated result" in {
    val expected = Right(Seq(individualUser))
    val individualUserCriteria = IndividualUserCriteria(
      status = Option(individualUser.status),
      partialMatchFields = Constants.validIndividualUsersPartialMatchFields.filterNot(_ == "disabled"))

    (customerRead.countIndividualUsersByCriteria _).when(individualUserCriteria).returns(Future.successful(Right(1)))
    (customerRead.findIndividualUsersByCriteria _)
      .when(individualUserCriteria, Nil, Some(1), None).returns(Future.successful(expected))

    val mockLatestVersion = LocalDateTime.now.toString
    (latestVersionService.getLatestVersion _).when(individualUserCriteria)
      .returns(Right(mockLatestVersion.toOption).toFuture)

    val resp = route(app, FakeRequest(GET, s"/individual_users?status=${individualUser.status.underlying}&limit=1").withHeaders(jsonHeaders)).get

    status(resp) mustBe OK
    val expectedJsonResponse =
      s"""{
         |"total":1,
         |"results":
         |[{"id":"${individualUser.id.toString}",
         |"username":"user",
         |"tier":"tier",
         |"segment":"segment1",
         |"subscription":"customerSubscription1",
         |"email":"user@pegb.tech",
         |"status":"new",
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
         |"created_at":"2018-01-01T00:00:00Z",
         |"created_by":"pegbuser",
         |"updated_at":"2018-01-01T00:00:00Z",
         |"updated_by":"pegbuser",
         |"activated_at":null}],
         |"limit":1,
         |"offset":null}""".stripMargin.replaceAll("\n", "")

    contentAsString(resp) mustBe expectedJsonResponse
    headers(resp).contains(versionHeaderKey) mustBe true
    headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
  }

  "get individual users by criteria should allow partial matching for user_id" in {

    val partialUserId = individualUser.id.toString.take(8)
    val individualUserCriteria = IndividualUserCriteria(
      userId = Option(UUIDLike(partialUserId)),
      partialMatchFields = Set("user_id"))

    (customerRead.countIndividualUsersByCriteria _).when(individualUserCriteria).returns(Future.successful(Right(1)))

    val expected = Right(Seq(individualUser))
    (customerRead.findIndividualUsersByCriteria _)
      .when(individualUserCriteria, Nil, Some(1), None).returns(Future.successful(expected))

    val mockLatestVersion = LocalDateTime.now.toString
    (latestVersionService.getLatestVersion _).when(individualUserCriteria)
      .returns(Right(mockLatestVersion.toOption).toFuture)

    val resp = route(app, FakeRequest(GET, s"/individual_users?user_id=${partialUserId}&partial_match=user_id&limit=1").withHeaders(jsonHeaders)).get

    status(resp) mustBe OK
    val expectedJsonResponse =
      s"""{
         |"total":1,
         |"results":
         |[{"id":"${individualUser.id.toString}",
         |"username":"user",
         |"tier":"tier",
         |"segment":"segment1",
         |"subscription":"customerSubscription1",
         |"email":"user@pegb.tech",
         |"status":"new",
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
         |"created_at":"2018-01-01T00:00:00Z",
         |"created_by":"pegbuser",
         |"updated_at":"2018-01-01T00:00:00Z",
         |"updated_by":"pegbuser",
         |"activated_at":null}],
         |"limit":1,
         |"offset":null}""".stripMargin.replaceAll("\n", "")
    contentAsString(resp) mustBe expectedJsonResponse
    headers(resp).contains(versionHeaderKey) mustBe true
    headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
  }

  "get individual users by criteria should return bad request when input is not valid" in {
    // val expected = Right(Seq(individualUser))

    val request = FakeRequest(GET, s"/individual_users?msisdn=DEADBEEF&limit=1")
      .withHeaders(jsonHeaders)
    val resp = route(app, request).get

    status(resp) mustBe BAD_REQUEST

    val expectedResponse =
      s"""
         |{
         |"id":"${mockRequestId}",
         |"code":"InvalidRequest",
         |"msg":"Invalid request to fetch individual users. Value of a query parameter is not in the correct format or not among the expected values."
         |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    contentAsString(resp) mustBe expectedResponse
  }

  "get individual users by criteria should return bad request when field in order_by is not valid" in {
    // val expected = Right(Seq(individualUser))

    val request = FakeRequest(GET, s"/individual_users?order_by=DEADBEEF&limit=1")
      .withHeaders(jsonHeaders)
    val resp = route(app, request).get

    status(resp) mustBe BAD_REQUEST

    val expectedResponse =
      s"""
         |{
         |"id":"${mockRequestId}",
         |"code":"InvalidRequest",
         |"msg":"Invalid request to fetch individual users. Value of order_by is not among the expected values."
         |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    contentAsString(resp) mustBe expectedResponse
  }

  "return an individual user's documents in getIndividualUserDocuments" in {

    val mockCustomerId = UUID.randomUUID()
    val docId1 = UUID.randomUUID()
    val docId2 = UUID.randomUUID()
    val docId3 = UUID.randomUUID()
    val createdTime = LocalDateTime.of(2018, 1, 1, 0, 0, 0)

    val expectedUser = Right(individualUser.copy(id = mockCustomerId))
    (customerRead.getIndividualUser _)
      .when(mockCustomerId).returns(Future.successful(expectedUser))

    val expected = Seq(
      Document.empty.copy(id = docId1, customerId = Some(mockCustomerId), createdAt = createdTime),
      Document.empty.copy(id = docId2, customerId = Some(mockCustomerId), createdAt = createdTime),
      Document.empty.copy(id = docId3, customerId = Some(mockCustomerId), createdAt = createdTime))

    val documentCriteria = DocumentCriteria(customerId = Some(UUIDLike.apply(mockCustomerId.toString)))

    (documentManagement.countDocumentsByCriteria _)
      .when(documentCriteria)
      .returns(Future.successful(Right(3)))

    (documentManagement.getDocumentsByCriteria _)
      .when(documentCriteria, *, None, None)
      .returns(Future.successful(Right(expected)))

    val mockLatestVersion = LocalDateTime.now.toString
    (latestVersionService.getLatestVersion _).when(documentCriteria)
      .returns(Right(mockLatestVersion.toOption).toFuture)

    val resp = route(app, FakeRequest(GET, s"/individual_users/${mockCustomerId}/documents")
      .withHeaders(jsonHeaders)).get

    status(resp) mustBe OK

    val expectedJsonResponse =
      s"""{
         |"total":3,
         |"results":[
         |{
         |"id":"$docId1",
         |"customer_id":"$mockCustomerId",
         |"application_id":"${expected(0).applicationId.get}",
         |"document_type":"some document type",
         |"document_identifier":null,
         |"purpose":"some purpose",
         |"created_at":"2018-01-01T00:00:00Z",
         |"created_by":"some user",
         |"status":"some document status",
         |"rejection_reason":null,
         |"checked_at":null,
         |"checked_by":null,
         |"uploaded_at":null,
         |"uploaded_by":null,
         |"updated_at":null
         |},
         |{
         |"id":"$docId2",
         |"customer_id":"$mockCustomerId",
         |"application_id":"${expected(1).applicationId.get}",
         |"document_type":"some document type",
         |"document_identifier":null,
         |"purpose":"some purpose",
         |"created_at":"2018-01-01T00:00:00Z",
         |"created_by":"some user",
         |"status":"some document status",
         |"rejection_reason":null,
         |"checked_at":null,
         |"checked_by":null,
         |"uploaded_at":null,
         |"uploaded_by":null,
         |"updated_at":null
         |},
         |{
         |"id":"$docId3",
         |"customer_id":"$mockCustomerId",
         |"application_id":"${expected(2).applicationId.get}",
         |"document_type":"some document type",
         |"document_identifier":null,
         |"purpose":"some purpose",
         |"created_at":"2018-01-01T00:00:00Z",
         |"created_by":"some user",
         |"status":"some document status",
         |"rejection_reason":null,
         |"checked_at":null,
         |"checked_by":null,
         |"uploaded_at":null,
         |"uploaded_by":null,
         |"updated_at":null
         |}
         |],
         |"limit":null,
         |"offset":null
         |}
         |""".stripMargin.replaceAll("\n", "")

    contentAsString(resp) mustBe expectedJsonResponse
    headers(resp).contains(versionHeaderKey) mustBe true
    headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
  }

  "return an individual user's documents by documentId" in {

    val mockCustomerId = UUID.randomUUID()
    val docId1 = UUID.randomUUID()
    val mockApplicationId = UUID.randomUUID()
    val createdTime = LocalDateTime.of(2018, 1, 1, 0, 0, 0)
    val expected = Document.empty.copy(
      id = docId1,
      customerId = Some(mockCustomerId),
      applicationId = mockApplicationId.toOption,
      createdAt = createdTime)

    val expectedUser = Right(individualUser.copy(id = mockCustomerId))
    (customerRead.getIndividualUser _)
      .when(mockCustomerId).returns(Future.successful(expectedUser))

    (documentManagement.getDocument _)
      .when(docId1)
      .returns(Future.successful(Right(expected)))

    val resp = route(app, FakeRequest(GET, s"/individual_users/$mockCustomerId/documents/$docId1")
      .withHeaders(jsonHeaders)).get
    status(resp) mustBe OK

    val expectedJsonResponse =
      s"""{
         |"id":"$docId1",
         |"customer_id":"$mockCustomerId",
         |"application_id":"$mockApplicationId",
         |"document_type":"some document type",
         |"document_identifier":null,
         |"purpose":"some purpose",
         |"created_at":"2018-01-01T00:00:00Z",
         |"created_by":"some user",
         |"status":"some document status",
         |"rejection_reason":null,
         |"checked_at":null,
         |"checked_by":null,
         |"uploaded_at":null,
         |"uploaded_by":null,
         |"updated_at":null
         |}""".stripMargin.replaceAll("\n", "")

    contentAsString(resp) mustBe expectedJsonResponse
  }

  "return validation error when try to get document by id which not belongs to user" in {

    val mockCustomerId = UUID.randomUUID()
    val docId1 = UUID.randomUUID()
    val createdTime = LocalDateTime.of(2018, 1, 1, 0, 0, 0)
    val expected = Document.empty.copy(id = docId1, customerId = Some(UUID.randomUUID), createdAt = createdTime)

    val expectedUser = Right(individualUser.copy(id = mockCustomerId))
    (customerRead.getIndividualUser _)
      .when(mockCustomerId).returns(Future.successful(expectedUser))

    (documentManagement.getDocument _)
      .when(docId1)
      .returns(Future.successful(Right(expected)))

    val resp = route(app, FakeRequest(GET, s"/individual_users/$mockCustomerId/documents/$docId1")
      .withHeaders(jsonHeaders)).get

    status(resp) mustBe BAD_REQUEST

    val expectedJsonResponse =
      s"""{
         |"id":"$mockRequestId",
         |"code":"InvalidRequest",
         |"msg":"Document id $docId1 does not belong to User $mockCustomerId"
         |}""".stripMargin.replaceAll("\n", "")

    contentAsString(resp) mustBe expectedJsonResponse
  }

  "return reject result when document belongs to customer" in {
    val mockCustomerId = UUID.randomUUID()
    val docId1 = UUID.randomUUID()

    val expectedUser = Right(individualUser.copy(id = mockCustomerId))
    (customerRead.getIndividualUser _)
      .when(mockCustomerId).returns(Future.successful(expectedUser))

    val mockDocumentToReject = DocumentToReject(
      id = docId1,
      rejectedBy = mockRequestFrom,
      rejectedAt = mockRequestLocalDate,
      reason = "document was fake",
      lastUpdatedAt = None)

    val getDocumentResult = Document.empty.copy(
      customerId = Some(mockCustomerId),
      documentType = DocumentTypes.fromString("selfie"),
      documentIdentifier = None,
      purpose = "for wallet application",
      status = DocumentStatuses.fromString("pending"),
      createdBy = mockRequestFrom,
      createdAt = mockRequestLocalDate,
      checkedBy = None,
      checkedAt = None,
      rejectionReason = None)

    val expectedRejectedDocument = Document.empty.copy(
      customerId = Some(mockCustomerId),
      documentType = DocumentTypes.fromString("selfie"),
      documentIdentifier = None,
      purpose = "for wallet application",
      status = DocumentStatuses.fromString("rejected"),
      createdBy = mockRequestFrom,
      createdAt = mockRequestLocalDate,
      checkedBy = Option(mockDocumentToReject.rejectedBy),
      checkedAt = Option(mockDocumentToReject.rejectedAt),
      rejectionReason = Option(mockDocumentToReject.reason))

    (documentManagement.getDocument _).when(docId1).returns(Future.successful(Right(getDocumentResult)))
    (documentManagement.rejectDocument _).when(mockDocumentToReject).returns(Future.successful(Right(expectedRejectedDocument)))

    val jsonRequest =
      """
        |{"reason":"document was fake"}
      """.stripMargin.replaceAll(System.lineSeparator(), "")

    val fakeRequest = FakeRequest(PUT, s"/individual_users/$mockCustomerId/documents/$docId1/reject", jsonHeaders, jsonRequest)

    val resp = route(app, fakeRequest).get

    val expectedResponse =
      s"""{"id":"${expectedRejectedDocument.id}",
         |"customer_id":"${expectedRejectedDocument.customerId.get}",
         |"application_id":"${expectedRejectedDocument.applicationId.get}",
         |"document_type":"${expectedRejectedDocument.documentType}",
         |"document_identifier":null,
         |"purpose":"${expectedRejectedDocument.purpose}",
         |"created_at":"${expectedRejectedDocument.createdAt.toZonedDateTimeUTC}",
         |"created_by":"${expectedRejectedDocument.createdBy}",
         |"status":"${expectedRejectedDocument.status}",
         |"rejection_reason":"${expectedRejectedDocument.rejectionReason.get}",
         |"checked_at":"${expectedRejectedDocument.checkedAt.get.toZonedDateTimeUTC}",
         |"checked_by":"${expectedRejectedDocument.checkedBy.get}",
         |"uploaded_at":null,
         |"uploaded_by":null,
         |"updated_at":null}""".stripMargin.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe OK
    contentAsString(resp) mustBe expectedResponse
  }

  "return fail on reject document when document belongs to another customer" in {
    val mockCustomerId = UUID.randomUUID()
    val otherCustomerId = UUID.randomUUID()
    val docId1 = UUID.randomUUID()

    val expectedUser = Right(individualUser.copy(id = mockCustomerId))
    (customerRead.getIndividualUser _)
      .when(mockCustomerId).returns(Future.successful(expectedUser))

    val getDocumentResult = Document.empty.copy(
      id = docId1,
      customerId = Some(otherCustomerId),
      documentType = DocumentTypes.fromString("selfie"),
      documentIdentifier = None,
      purpose = "for wallet application",
      status = DocumentStatuses.fromString("pending"),
      createdBy = mockRequestFrom,
      createdAt = mockRequestLocalDate,
      checkedBy = None,
      checkedAt = None,
      rejectionReason = None)

    (documentManagement.getDocument _).when(docId1).returns(Future.successful(Right(getDocumentResult)))

    val jsonRequest =
      """
        |{"reason":"document was fake"}
      """.stripMargin.replaceAll(System.lineSeparator(), "")

    val fakeRequest = FakeRequest(PUT, s"/individual_users/$mockCustomerId/documents/$docId1/reject", jsonHeaders, jsonRequest)

    val resp = route(app, fakeRequest).get

    status(resp) mustBe BAD_REQUEST
    val expectedJsonResponse =
      s"""{
         |"id":"$mockRequestId",
         |"code":"InvalidRequest",
         |"msg":"Document id $docId1 does not belong to User $mockCustomerId"
         |}""".stripMargin.replaceAll("\n", "")

    contentAsString(resp) mustBe expectedJsonResponse
  }

  "return Ok 200 and a DocumentToRead json (status approved, checked_by and checked_at not empty) as http response body" in {
    val docId1 = UUID.randomUUID()

    val expectedUser = Right(individualUser.copy(id = mockCustomerId2))
    (customerRead.getIndividualUser _)
      .when(mockCustomerId2).returns(Future.successful(expectedUser))

    val mockDocumentToApprove = DocumentToApprove(
      id = docId1,
      approvedBy = mockRequestFrom,
      approvedAt = mockRequestLocalDate,
      lastUpdatedAt = None)

    val getDocumentResult = Document.empty.copy(
      customerId = Some(mockCustomerId2),
      documentType = DocumentTypes.fromString("selfie"),
      documentIdentifier = None,
      purpose = "for wallet application",
      status = DocumentStatuses.fromString("pending"),
      createdBy = mockRequestFrom,
      createdAt = mockRequestLocalDate,
      checkedBy = None,
      checkedAt = None,
      rejectionReason = None)

    val expectedApprovedDocument = Document.empty.copy(
      customerId = Some(UUID.randomUUID()),
      documentType = DocumentTypes.fromString("selfie"),
      documentIdentifier = None,
      purpose = "for wallet application",
      status = DocumentStatuses.fromString("approved"),
      createdBy = mockRequestFrom,
      createdAt = mockRequestLocalDate,
      checkedBy = Option(mockDocumentToApprove.approvedBy),
      checkedAt = Option(mockDocumentToApprove.approvedAt),
      fileUploadedBy = Option("George"),
      fileUploadedAt = Option(mockRequestDate.plusDays(1).toLocalDateTimeUTC))

    (documentManagement.getDocument _).when(docId1).returns(Future.successful(Right(getDocumentResult)))
    (documentManagement.approveDocument _).when(mockDocumentToApprove).returns(Future.successful(Right(expectedApprovedDocument)))

    val jsonRequest =
      s"""{
         |"updated_at": null
         |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    val fakeRequest = FakeRequest(PUT, s"/individual_users/$mockCustomerId2/documents/$docId1/approve", jsonHeaders, jsonRequest)

    val resp = route(app, fakeRequest).get

    val expectedResponse =
      s"""{"id":"${expectedApprovedDocument.id}",
         |"customer_id":"${expectedApprovedDocument.customerId.get}",
         |"application_id":"${expectedApprovedDocument.applicationId.get}",
         |"document_type":"${expectedApprovedDocument.documentType}",
         |"document_identifier":null,
         |"purpose":"${expectedApprovedDocument.purpose}",
         |"created_at":"${expectedApprovedDocument.createdAt.toZonedDateTimeUTC}",
         |"created_by":"${expectedApprovedDocument.createdBy}",
         |"status":"${expectedApprovedDocument.status}",
         |"rejection_reason":null,
         |"checked_at":"${expectedApprovedDocument.checkedAt.get.toZonedDateTimeUTC}",
         |"checked_by":"${expectedApprovedDocument.checkedBy.get}",
         |"uploaded_at":"${mockRequestDate.plusDays(1).toLocalDateTimeUTC.toZonedDateTimeUTC}",
         |"uploaded_by":"George",
         |"updated_at":null}""".stripMargin.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe OK
    contentAsString(resp) mustBe expectedResponse
  }

  "return fail on approve document when document belongs to another customer" in {
    val mockCustomerId = UUID.randomUUID()
    val otherCustomerId = UUID.randomUUID()
    val docId1 = UUID.randomUUID()

    val expectedUser = Right(individualUser.copy(id = mockCustomerId))
    (customerRead.getIndividualUser _)
      .when(mockCustomerId).returns(Future.successful(expectedUser))

    val getDocumentResult = Document.empty.copy(
      id = docId1,
      customerId = Some(otherCustomerId),
      documentType = DocumentTypes.fromString("selfie"),
      documentIdentifier = None,
      purpose = "for wallet application",
      status = DocumentStatuses.fromString("pending"),
      createdBy = mockRequestFrom,
      createdAt = mockRequestLocalDate,
      checkedBy = None,
      checkedAt = None,
      rejectionReason = None)

    (documentManagement.getDocument _).when(docId1).returns(Future.successful(Right(getDocumentResult)))

    val jsonRequest =
      """
        |{"reason":"document was fake"}
      """.stripMargin.replaceAll(System.lineSeparator(), "")

    val fakeRequest = FakeRequest(PUT, s"/individual_users/$mockCustomerId/documents/$docId1/approve", jsonHeaders, jsonRequest)

    val resp = route(app, fakeRequest).get

    status(resp) mustBe BAD_REQUEST
    val expectedJsonResponse =
      s"""{
         |"id":"$mockRequestId",
         |"code":"InvalidRequest",
         |"msg":"Document id $docId1 does not belong to User $mockCustomerId"
         |}""".stripMargin.replaceAll("\n", "")

    contentAsString(resp) mustBe expectedJsonResponse
  }

  "get individual user wallet applications by user id" in {
    val applicationId = UUID.randomUUID()
    val userUUID = UUID.randomUUID()
    val doneAt = ZonedDateTime.now(mockClock)

    val expected = model.WalletApplication.getEmpty.copy(
      id = applicationId,
      customerId = userUUID,
      msisdn = Some(Msisdn("+1921717784288")),
      fullName = Some("Ujali Test Tyagi"),
      status = ApplicationStatus("APPROVED"),
      applicationStage = "ocr",
      checkedBy = Some("pegbuser"),
      checkedAt = Some(doneAt.toLocalDateTimeUTC),
      rejectionReason = None,
      createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
      createdBy = "pegbuser",
      updatedAt = Some(doneAt.toLocalDateTimeUTC),
      updatedBy = Some("pegbuser"))

    ((userUUID: _root_.java.util.UUID) ⇒ mockedWalletApplication.getWalletApplicationsByUserId(userUUID)).when(userUUID)
      .returns(Future.successful(Right(Set(expected))))

    val resp = route(app, FakeRequest(GET, s"/individual_users/$userUUID/wallet_applications")).get

    val expectedJson =
      s"""{
         |"total":1,
         |"results":[{"id":"$applicationId",
         |"customer_id":"$userUUID",
         |"full_name":"Ujali Test Tyagi",
         |"person_id":null,"msisdn":"+1921717784288","status":"approved",
         |"application_stage":"ocr","applied_at":"1970-01-01T00:00:03Z",
         |"checked_at":"1970-01-01T00:00:03Z","checked_by":"pegbuser",
         |"reason_if_rejected":null,
         |"total_score":null,
         |"updated_at":"1970-01-01T00:00:03Z"}],
         |"limit":null,"offset":null
         |}
         |""".stripMargin.replaceAll("\n", "")
    status(resp) mustBe OK
    contentAsString(resp) mustBe expectedJson
  }

  "get individual user wallet applications by application id and user id" in {
    val applicationId = UUID.randomUUID()
    val userUUID = UUID.randomUUID()
    val doneAt = ZonedDateTime.now(mockClock)

    val expected = model.WalletApplication.getEmpty.copy(
      id = applicationId,
      customerId = userUUID,
      msisdn = Some(Msisdn("+1921717784288")),
      fullName = Some("Ujali Test Tyagi"),
      status = ApplicationStatus("APPROVED"),
      applicationStage = "ocr",
      checkedBy = Some("pegbuser"),
      checkedAt = Some(doneAt.toLocalDateTimeUTC),
      rejectionReason = None,
      createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
      createdBy = "pegbuser",
      updatedAt = Some(doneAt.toLocalDateTimeUTC),
      updatedBy = Some("pegbuser"))

    ((userUUID: _root_.java.util.UUID, applicationUUID: _root_.java.util.UUID) ⇒ mockedWalletApplication.getWalletApplicationByApplicationIdAndUserId(userUUID, applicationUUID)).when(userUUID, applicationId)
      .returns(Future.successful(Right(expected)))

    val resp = route(app, FakeRequest(GET, s"/individual_users/$userUUID/wallet_applications/$applicationId")).get

    val expectedJson =
      s"""{
         |"id":"$applicationId",
         |"customer_id":"$userUUID",
         |"full_name":"Ujali Test Tyagi",
         |"person_id":null,"msisdn":"+1921717784288","status":"approved",
         |"application_stage":"ocr","applied_at":"1970-01-01T00:00:03Z",
         |"checked_at":"1970-01-01T00:00:03Z","checked_by":"pegbuser",
         |"reason_if_rejected":null,
         |"total_score":null,
         |"updated_at":"1970-01-01T00:00:03Z"
         |}
         |""".stripMargin.replaceAll("\n", "")
    status(resp) mustBe OK
    contentAsString(resp) mustBe expectedJson
  }

  "approve a pending wallet application and return Ok(approved application)" in {
    val pendingApplicationId = UUID.randomUUID()
    val userUUID = UUID.randomUUID()
    val doneAt = ZonedDateTime.now(mockClock)
    val doneBy = "pegbuser"

    val expected = model.WalletApplication.getEmpty.copy(
      id = pendingApplicationId,
      customerId = userUUID,
      msisdn = None,
      fullName = Some("Dima Test Linou"),
      status = ApplicationStatus("APPROVED"),
      applicationStage = "scored",
      checkedBy = Some(doneBy),
      checkedAt = Some(doneAt.toLocalDateTimeUTC),
      rejectionReason = None,
      createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
      createdBy = "pegbuser",
      updatedAt = Some(doneAt.toLocalDateTimeUTC),
      updatedBy = Some(doneBy))

    ((userUUID: _root_.java.util.UUID, applicationUUID: _root_.java.util.UUID, approvedBy: _root_.scala.Predef.String, approvedAt: _root_.java.time.LocalDateTime, lastUpdatedAt: _root_.scala.Option[_root_.java.time.LocalDateTime]) ⇒ mockedWalletApplication.approveWalletApplicationByUserId(userUUID, applicationUUID, approvedBy, approvedAt, lastUpdatedAt))
      .when(userUUID, pendingApplicationId, mockRequestFrom, mockRequestLocalDate, None)
      .returns(Future.successful(Right(expected)))

    val jsonRequest =
      s"""{
         |"updated_at": null
         |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    val resp = route(app, FakeRequest(PUT, s"/individual_users/$userUUID/wallet_applications/$pendingApplicationId/approve", jsonHeaders, jsonRequest)).get
    val expectedJson =
      s"""
         |{"id":"$pendingApplicationId",
         |"customer_id":"$userUUID",
         |"full_name":"Dima Test Linou",
         |"person_id":null,
         |"msisdn":null,
         |"status":"approved",
         |"application_stage":"scored",
         |"applied_at":"1970-01-01T00:00:03Z",
         |"checked_at":"1970-01-01T00:00:03Z",
         |"checked_by":"pegbuser",
         |"reason_if_rejected":null,
         |"total_score":null,
         |"updated_at":"1970-01-01T00:00:03Z"}
         |""".stripMargin.replaceAll("\n", "")
    status(resp) mustBe OK
    contentAsString(resp) mustBe expectedJson

  }

  "reject a pending wallet application and return Ok(rejected application)" in {
    val pendingApplicationId = UUID.randomUUID()
    val userUUID = UUID.randomUUID()
    val doneAt = ZonedDateTime.now(mockClock)
    val doneBy = "pegbuser"

    val expected = model.WalletApplication.getEmpty.copy(
      id = pendingApplicationId,
      customerId = userUUID,
      msisdn = None,
      fullName = Some("Dima Test Linou"),
      status = ApplicationStatus("REJECTED"),
      applicationStage = "scored",
      checkedBy = Some(doneBy),
      checkedAt = Some(doneAt.toLocalDateTimeUTC),
      rejectionReason = Some("insufficient document"),
      createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
      createdBy = "pegbuser",
      updatedAt = Some(doneAt.toLocalDateTimeUTC),
      updatedBy = Some(doneBy))

    val jsonRequest =
      s"""
         |{"reason": "insufficient document"}
        """.stripMargin.replaceAll("\n", "")

    ((userUUID: _root_.java.util.UUID, applicationUUID: _root_.java.util.UUID, rejectedBy: _root_.scala.Predef.String, rejectedAt: _root_.java.time.LocalDateTime, reason: _root_.scala.Predef.String, lastUpdatedAt: _root_.scala.Option[_root_.java.time.LocalDateTime]) ⇒ mockedWalletApplication.rejectWalletApplicationByUserId(userUUID, applicationUUID, rejectedBy, rejectedAt, reason, lastUpdatedAt))
      .when(userUUID, pendingApplicationId, doneBy, doneAt.toLocalDateTimeUTC, "insufficient document", None)
      .returns(Future.successful(Right(expected)))

    val resp = route(app, FakeRequest(PUT, s"/individual_users/$userUUID/wallet_applications/$pendingApplicationId/reject")
      .withBody(jsonRequest)
      .withHeaders(Headers(Seq(
        "Content-type" → "application/json",
        requestDateHeaderKey → doneAt.toString,
        requestFromHeaderKey → doneBy,
        "request-id" → UUID.randomUUID().toString): _*))).get

    val expectedJson =
      s"""
         |{"id":"$pendingApplicationId",
         |"customer_id":"$userUUID",
         |"full_name":"Dima Test Linou",
         |"person_id":null,
         |"msisdn":null,
         |"status":"rejected",
         |"application_stage":"scored",
         |"applied_at":"1970-01-01T00:00:03Z",
         |"checked_at":"1970-01-01T00:00:03Z",
         |"checked_by":"pegbuser",
         |"reason_if_rejected":"insufficient document",
         |"total_score":null,
         |"updated_at":"1970-01-01T00:00:03Z"}
         |""".stripMargin.replaceAll("\n", "")
    status(resp) mustBe OK
    contentAsString(resp) mustBe expectedJson
  }
}
