package tech.pegb.backoffice.api.customer.controller

import java.time._
import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, Headers}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import tech.pegb.backoffice.api.customer.Constants
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.PaginatedResult
import tech.pegb.backoffice.dao.DaoError.GenericDbError
import tech.pegb.backoffice.domain.account.abstraction.AccountManagement
import tech.pegb.backoffice.domain.account.dto.AccountCriteria
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountMainType, AccountNumber, AccountStatus, AccountType}
import tech.pegb.backoffice.domain.account.model.{Account, FloatAccountAggregation}
import tech.pegb.backoffice.domain.customer.abstraction.CustomerAccount
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.settings.abstraction.SystemSettingService
import tech.pegb.backoffice.domain.settings.model.SystemSetting
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionManagement
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.api.customer.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBTestApp, TestExecutionContext}

import scala.concurrent.Future

@Ignore
class AccountControllerSpec extends PlaySpec with PegBTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {
  implicit val ec = TestExecutionContext.genericOperations

  val customerAccount: CustomerAccount = stub[CustomerAccount]
  val accountMgmt: AccountManagement = stub[AccountManagement]
  val systemSettingsService: SystemSettingService = stub[SystemSettingService]
  val txnService: TransactionManagement = stub[TransactionManagement]
  val latestVersionService: LatestVersionService = stub[LatestVersionService]

  val localDateTimeFrom: LocalDateTime = LocalDateTime.of(2019, 9, 1, 0, 0, 0)
  val localDateTimeTo: LocalDateTime = LocalDateTime.of(2019, 9, 3, 23, 59, 59)

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[CustomerAccount].to(customerAccount),
      bind[AccountManagement].to(accountMgmt),
      bind[SystemSettingService].to(systemSettingsService),
      bind[TransactionManagement].to(txnService),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

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

  "Accounts API" should {
    "return account in getAccountsById" in {
      (accountMgmt.getAccountById _)
        .when(id1).returns(Future.successful(Right(account1))).noMoreThanOnce()

      val resp = route(app, FakeRequest(GET, s"/api/accounts/${id1}")).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual Option(account1).map(_.asApi).toJsonStr

    }
    "return account in getAccountByAccountNumber" in {
      (customerAccount.getAccountByAccountNumber _)
        .when(AccountNumber("0002")).returns(Future.successful(Right(account2))).noMoreThanOnce()

      val resp = route(app, FakeRequest(GET, s"/api/accounts/account_number/0002")).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual Option(account2).map(_.asApi).toJsonStr

    }
    "return account in getAccountByAccountName" in {
      (customerAccount.getAccountByAccountName _)
        .when(NameAttribute("Alice")).returns(Future.successful(Right(account1))).noMoreThanOnce()

      val resp = route(app, FakeRequest(GET, s"/api/accounts/account_name/Alice")).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual Option(account1).map(_.asApi).toJsonStr

    }

    "activate account status" in {
      val expected = account2.copy(accountStatus = AccountStatus(Account.ACTIVE))
      (accountMgmt.activateAccount _)
        .when(account2.id, *, *, *)
        .returns(Future.successful(Right(expected))).noMoreThanOnce()

      val resp = route(
        app,
        FakeRequest(PUT, s"/api/accounts/${account2.id.toString}/activate", jsonHeaders, AnyContentAsEmpty)).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual Option(expected).map(_.asApi).toJsonStr
    }

    "close account status" in {
      val expected = account2.copy(accountStatus = AccountStatus(Account.CLOSED))
      (accountMgmt.deleteAccount _)
        .when(account2.id, *, *, *)
        .returns(Future.successful(Right(expected))).noMoreThanOnce()

      val resp = route(
        app,
        FakeRequest(PUT, s"/api/accounts/${account2.id.toString}/close", jsonHeaders, AnyContentAsEmpty)).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual Option(expected).map(_.asApi).toJsonStr
    }

    "deactivate account status" in {

      val expected = account2.copy(accountStatus = AccountStatus(Account.BLOCKED))
      (accountMgmt.blockAccount _)
        .when(account2.id, *, *, *)
        .returns(Future.successful(Right(expected))).noMoreThanOnce()

      val resp = route(
        app,
        FakeRequest(PUT, s"/api/accounts/${account2.id.toString}/deactivate", jsonHeaders, AnyContentAsEmpty)).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual Option(expected).map(_.asApi).toJsonStr
    }

    "return account in getAccounts order by account number" in {
      val emptyCriteria = AccountCriteria(None, None, None, None, None, None,
        partialMatchFields = Constants.validAccountsPartialMatchFields.filterNot(_ == "disabled"))
      (accountMgmt.countAccountsByCriteria _)
        .when(emptyCriteria).returns(Future.successful(Right(2))).noMoreThanOnce()
      (accountMgmt.getAccountsByCriteria _)
        .when(emptyCriteria, Seq(Ordering("number", Ordering.ASCENDING)), None, None).returns(Future.successful(Right(Seq(account1, account2)))).noMoreThanOnce()
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(emptyCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/api/accounts?order_by=number")).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual PaginatedResult(total = 2, results = Seq(account1, account2).map(_.asApi), limit = None, offset = None).toJsonStr

      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return validation error when order_by contains invalid column" in {
      val empty = AccountCriteria(None, None, None, None, None, None,
        partialMatchFields = Constants.validAccountsPartialMatchFields.filterNot(_ == "disabled"))
      (accountMgmt.countAccountsByCriteria _)
        .when(AccountCriteria(None, None, None, None, None)).returns(Future.successful(Right(2))).noMoreThanOnce()
      (accountMgmt.getAccountsByCriteria _)
        .when(empty, Seq(Ordering("invalid_column", Ordering.ASCENDING)), None, None)
        .returns(GenericDbError("Invalid column found").asDomainError.toLeft.toFuture).noMoreThanOnce()

      val mockRequestId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/api/accounts?order_by=invalid_column")
        .withHeaders(Headers(requestIdHeaderKey → mockRequestId.toString)))
        .get

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Invalid field found in order_by of accounts."
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST

      contentAsString(resp) mustBe expectedResponse
    }

    "create account should fail if accountToCreate json is missing something" in {

      val customerId = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"customer_id":"${customerId.toString}",
           |"number":"001",
           |"name":"USD ACCOUNT",
           |"type":"WALLET_ACCOUNT",
           |"is_main_account":"false",
           |"initial_balance": 10000}
        """.stripMargin

      val fakeRequest = FakeRequest(POST, "/api/accounts", jsonHeaders, jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"MalformedRequest",
           |"msg":"Malformed request to create account. Mandatory field is missing or value of a field is of wrong type."
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse

      status(resp) mustBe BAD_REQUEST

    }

    "return float by date range with date query parameters" in {
      val userUuidOne = UUID.randomUUID()
      val accountUuidOne = UUID.randomUUID()

      val userUuidTwo = UUID.randomUUID()
      val accountUuidTwo = UUID.randomUUID()

      val userUuidThree = UUID.randomUUID()
      val accountUuidThree = UUID.randomUUID()

      val transactionCriteria = TransactionCriteria(
        accountNumbers = Seq("pesalink", "mpesa", "test"),
        startDate = Some(localDateTimeFrom),
        endDate = Some(localDateTimeTo))

      val systemSetting = SystemSetting(
        id = 1,
        key = "float-account-numbers",
        value = "[pesalink, mpesa, test]",
        `type` = "json",
        explanation = None,
        forAndroid = false,
        forIOS = true,
        forBackoffice = true,
        createdAt = LocalDateTime.of(2019, 10, 10, 0, 0, 0),
        createdBy = "ujali",
        updatedAt = None,
        updatedBy = None)

      val floatAggregationOne = FloatAccountAggregation(
        userUuid = userUuidOne,
        userName = "ujali",
        accountUuid = accountUuidOne,
        accountNumber = AccountNumber("123345643"),
        accountType = AccountType("distribution"),
        accountMainType = AccountMainType("liability"),
        currency = Currency.getInstance("KES"),
        internalBalance = BigDecimal(500.00),
        externalBalance = None,
        inflow = BigDecimal(500.00),
        outflow = BigDecimal(300.00),
        net = BigDecimal(200.00),
        createdAt = LocalDateTime.now(),
        updatedAt = None)

      val floatAggregationTwo = FloatAccountAggregation(
        userUuid = userUuidTwo,
        userName = "david",
        accountUuid = accountUuidTwo,
        accountNumber = AccountNumber("123345644"),
        accountType = AccountType("distribution"),
        accountMainType = AccountMainType("liability"),
        currency = Currency.getInstance("KES"),
        internalBalance = BigDecimal(500.00),
        externalBalance = None,
        inflow = BigDecimal(500.00),
        outflow = BigDecimal(300.00),
        net = BigDecimal(200.00),
        createdAt = LocalDateTime.now(),
        updatedAt = None)

      val floatAggregationThree = FloatAccountAggregation(
        userUuid = userUuidThree,
        userName = "lloyd",
        accountUuid = accountUuidThree,
        accountNumber = AccountNumber("123345645"),
        accountType = AccountType("distribution"),
        accountMainType = AccountMainType("liability"),
        currency = Currency.getInstance("KES"),
        internalBalance = BigDecimal(500.00),
        externalBalance = None,
        inflow = BigDecimal(500.00),
        outflow = BigDecimal(300.00),
        net = BigDecimal(200.00),
        createdAt = LocalDateTime.now(),
        updatedAt = None)

      val expectedAggregations = Seq(floatAggregationOne, floatAggregationTwo, floatAggregationThree)
      val expectedJson =
        s"""
           |{
           |"total":3,
           |"results":[{
           |"id":"${floatAggregationOne.accountUuid}",
           |"user_id":"${floatAggregationOne.userUuid}",
           |"user_name":"ujali",
           |"account_number":"123345643",
           |"type":"distribution",
           |"main_type":"liability",
           |"currency":"Kenyan Shilling",
           |"internal_balance":500.0,
           |"external_balance":null,
           |"inflow":500.0,
           |"outflow":300.0,
           |"net":200.0,
           |"created_at":${floatAggregationOne.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${floatAggregationOne.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |},
           |{
           |"id":"${floatAggregationTwo.accountUuid}",
           |"user_id":"${floatAggregationTwo.userUuid}",
           |"user_name":"david",
           |"account_number":"123345644",
           |"type":"distribution",
           |"main_type":"liability",
           |"currency":"Kenyan Shilling",
           |"internal_balance":500.0,
           |"external_balance":null,
           |"inflow":500.0,
           |"outflow":300.0,
           |"net":200.0,
           |"created_at":${floatAggregationTwo.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${floatAggregationTwo.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |},
           |{
           |"id":"${floatAggregationThree.accountUuid}",
           |"user_id":"${floatAggregationThree.userUuid}",
           |"user_name":"lloyd",
           |"account_number":"123345645",
           |"type":"distribution",
           |"main_type":"liability",
           |"currency":"Kenyan Shilling",
           |"internal_balance":500.0,
           |"external_balance":null,
           |"inflow":500.0,
           |"outflow":300.0,
           |"net":200.0,
           |"created_at":${floatAggregationThree.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${floatAggregationThree.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |}],
           |"limit":null,
           |"offset":null}
         """.stripMargin.replaceAll("\n", "").trim

      (systemSettingsService.getSystemSettingArrayValueByKey _).when("float_account_numbers")
        .returns(Future.successful(Right(Seq("pesalink", "mpesa", "test"))))

      (accountMgmt.executeOnFlyAggregation(
        _: TransactionCriteria,
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int])).when(*, *, *, *)
        .returns(Future.successful(Right(expectedAggregations))).noMoreThanOnce()

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(*)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/api/floats?date_from=2019-09-01&date_to=2019-09-03")).get

      status(resp) mustBe OK

      contentAsString(resp) mustEqual expectedJson

    }

    "return float by date range when no query parameter is provided" in {
      val userUuidOne = UUID.randomUUID()
      val accountUuidOne = UUID.randomUUID()

      val userUuidTwo = UUID.randomUUID()
      val accountUuidTwo = UUID.randomUUID()

      val userUuidThree = UUID.randomUUID()
      val accountUuidThree = UUID.randomUUID()

      val currentDateStart = LocalDateTime.now(ZoneOffset.UTC).`with`(LocalTime.MIN) //.atTime(0,0,0)
      val currentDateEnd = LocalDateTime.now(ZoneOffset.UTC).`with`(LocalTime.MAX) //.atTime(23,59,59)

      val floatAggregationOne = FloatAccountAggregation(
        userUuid = userUuidOne,
        userName = "ujali",
        accountUuid = accountUuidOne,
        accountNumber = AccountNumber("123345643"),
        accountType = AccountType("distribution"),
        accountMainType = AccountMainType("liability"),
        currency = Currency.getInstance("KES"),
        internalBalance = BigDecimal(500.00),
        externalBalance = None,
        inflow = BigDecimal(500.00),
        outflow = BigDecimal(300.00),
        net = BigDecimal(200.00),
        createdAt = LocalDateTime.now(),
        updatedAt = None)

      val floatAggregationTwo = FloatAccountAggregation(
        userUuid = userUuidTwo,
        userName = "david",
        accountUuid = accountUuidTwo,
        accountNumber = AccountNumber("123345644"),
        accountType = AccountType("distribution"),
        accountMainType = AccountMainType("liability"),
        currency = Currency.getInstance("KES"),
        internalBalance = BigDecimal(500.00),
        externalBalance = None,
        inflow = BigDecimal(500.00),
        outflow = BigDecimal(300.00),
        net = BigDecimal(200.00),
        createdAt = LocalDateTime.now(),
        updatedAt = None)

      val floatAggregationThree = FloatAccountAggregation(
        userUuid = userUuidThree,
        userName = "lloyd",
        accountUuid = accountUuidThree,
        accountNumber = AccountNumber("123345645"),
        accountType = AccountType("distribution"),
        accountMainType = AccountMainType("liability"),
        currency = Currency.getInstance("KES"),
        internalBalance = BigDecimal(500.00),
        externalBalance = None,
        inflow = BigDecimal(500.00),
        outflow = BigDecimal(300.00),
        net = BigDecimal(200.00),
        createdAt = LocalDateTime.now(),
        updatedAt = None)

      val expectedAggregations = Seq(floatAggregationOne, floatAggregationTwo, floatAggregationThree)
      val expectedJson =
        s"""
           |{
           |"total":3,
           |"results":[{
           |"id":"${floatAggregationOne.accountUuid}",
           |"user_id":"${floatAggregationOne.userUuid}",
           |"user_name":"ujali",
           |"account_number":"123345643",
           |"type":"distribution",
           |"main_type":"liability",
           |"currency":"Kenyan Shilling",
           |"internal_balance":500.0,
           |"external_balance":null,
           |"inflow":500.0,
           |"outflow":300.0,
           |"net":200.0,
           |"created_at":${floatAggregationOne.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${floatAggregationOne.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |},
           |{
           |"id":"${floatAggregationTwo.accountUuid}",
           |"user_id":"${floatAggregationTwo.userUuid}",
           |"user_name":"david",
           |"account_number":"123345644",
           |"type":"distribution",
           |"main_type":"liability",
           |"currency":"Kenyan Shilling",
           |"internal_balance":500.0,
           |"external_balance":null,
           |"inflow":500.0,
           |"outflow":300.0,
           |"net":200.0,
           |"created_at":${floatAggregationTwo.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${floatAggregationTwo.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |},
           |{
           |"id":"${floatAggregationThree.accountUuid}",
           |"user_id":"${floatAggregationThree.userUuid}",
           |"user_name":"lloyd",
           |"account_number":"123345645",
           |"type":"distribution",
           |"main_type":"liability",
           |"currency":"Kenyan Shilling",
           |"internal_balance":500.0,
           |"external_balance":null,
           |"inflow":500.0,
           |"outflow":300.0,
           |"net":200.0,
           |"created_at":${floatAggregationThree.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${floatAggregationThree.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |}],
           |"limit":null,
           |"offset":null}
         """.stripMargin.replaceAll("\n", "").trim

      (systemSettingsService.getSystemSettingArrayValueByKey _).when("float_account_numbers")
        .returns(Future.successful(Right(Seq("pesalink", "mpesa", "test"))))

      (accountMgmt.executeOnFlyAggregation(
        _: TransactionCriteria,
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int])).when(*, *, *, *)
        .returns(Future.successful(Right(expectedAggregations))).noMoreThanOnce()

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(*)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/api/floats")).get

      status(resp) mustBe OK

      contentAsString(resp) mustEqual expectedJson

    }

    "return float by date range with offset and limit" in {
      val userUuidOne = UUID.randomUUID()
      val accountUuidOne = UUID.randomUUID()

      val userUuidTwo = UUID.randomUUID()
      val accountUuidTwo = UUID.randomUUID()

      val userUuidThree = UUID.randomUUID()
      val accountUuidThree = UUID.randomUUID()

      val currentDateStart = LocalDateTime.now(ZoneOffset.UTC).`with`(LocalTime.MIN) //.atTime(0,0,0)
      val currentDateEnd = LocalDateTime.now(ZoneOffset.UTC).`with`(LocalTime.MAX) //.atTime(23,59,59)

      val transactionCriteria = TransactionCriteria(
        accountNumbers = Seq("pesalink", "mpesa", "test"),
        startDate = Some(currentDateStart),
        endDate = Some(currentDateEnd))

      val systemSetting = SystemSetting(
        id = 1,
        key = "float-account-numbers",
        value = "[pesalink, mpesa, test]",
        `type` = "json",
        explanation = None,
        forAndroid = false,
        forIOS = true,
        forBackoffice = true,
        createdAt = LocalDateTime.of(2019, 10, 10, 0, 0, 0),
        createdBy = "ujali",
        updatedAt = None,
        updatedBy = None)

      val floatAggregationOne = FloatAccountAggregation(
        userUuid = userUuidOne,
        userName = "ujali",
        accountUuid = accountUuidOne,
        accountNumber = AccountNumber("123345643"),
        accountType = AccountType("distribution"),
        accountMainType = AccountMainType("liability"),
        currency = Currency.getInstance("KES"),
        internalBalance = BigDecimal(500.00),
        externalBalance = None,
        inflow = BigDecimal(500.00),
        outflow = BigDecimal(300.00),
        net = BigDecimal(200.00),
        createdAt = LocalDateTime.now(),
        updatedAt = None)

      val floatAggregationTwo = FloatAccountAggregation(
        userUuid = userUuidTwo,
        userName = "david",
        accountUuid = accountUuidTwo,
        accountNumber = AccountNumber("123345644"),
        accountType = AccountType("distribution"),
        accountMainType = AccountMainType("liability"),
        currency = Currency.getInstance("KES"),
        internalBalance = BigDecimal(500.00),
        externalBalance = None,
        inflow = BigDecimal(500.00),
        outflow = BigDecimal(300.00),
        net = BigDecimal(200.00),
        createdAt = LocalDateTime.now(),
        updatedAt = None)

      val floatAggregationThree = FloatAccountAggregation(
        userUuid = userUuidThree,
        userName = "lloyd",
        accountUuid = accountUuidThree,
        accountNumber = AccountNumber("123345645"),
        accountType = AccountType("distribution"),
        accountMainType = AccountMainType("liability"),
        currency = Currency.getInstance("KES"),
        internalBalance = BigDecimal(500.00),
        externalBalance = None,
        inflow = BigDecimal(500.00),
        outflow = BigDecimal(300.00),
        net = BigDecimal(200.00),
        createdAt = LocalDateTime.now(),
        updatedAt = None)

      val expectedAggregations = Seq(floatAggregationOne, floatAggregationTwo, floatAggregationThree)
      val expectedJson =
        s"""
           |{
           |"total":3,
           |"results":[{
           |"id":"${floatAggregationOne.accountUuid}",
           |"user_id":"${floatAggregationOne.userUuid}",
           |"user_name":"ujali",
           |"account_number":"123345643",
           |"type":"distribution",
           |"main_type":"liability",
           |"currency":"Kenyan Shilling",
           |"internal_balance":500.0,
           |"external_balance":null,
           |"inflow":500.0,
           |"outflow":300.0,
           |"net":200.0,
           |"created_at":${floatAggregationOne.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${floatAggregationOne.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |},
           |{
           |"id":"${floatAggregationTwo.accountUuid}",
           |"user_id":"${floatAggregationTwo.userUuid}",
           |"user_name":"david",
           |"account_number":"123345644",
           |"type":"distribution",
           |"main_type":"liability",
           |"currency":"Kenyan Shilling",
           |"internal_balance":500.0,
           |"external_balance":null,
           |"inflow":500.0,
           |"outflow":300.0,
           |"net":200.0,
           |"created_at":${floatAggregationTwo.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${floatAggregationTwo.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |},
           |{
           |"id":"${floatAggregationThree.accountUuid}",
           |"user_id":"${floatAggregationThree.userUuid}",
           |"user_name":"lloyd",
           |"account_number":"123345645",
           |"type":"distribution",
           |"main_type":"liability",
           |"currency":"Kenyan Shilling",
           |"internal_balance":500.0,
           |"external_balance":null,
           |"inflow":500.0,
           |"outflow":300.0,
           |"net":200.0,
           |"created_at":${floatAggregationThree.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${floatAggregationThree.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |}],
           |"limit":1,
           |"offset":1}
         """.stripMargin.replaceAll("\n", "").trim

      (systemSettingsService.getSystemSettingArrayValueByKey _).when("float_account_numbers")
        .returns(Future.successful(Right(Seq("pesalink", "mpesa", "test"))))

      (accountMgmt.executeOnFlyAggregation(
        _: TransactionCriteria,
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int])).when(*, *, *, *)
        .returns(Future.successful(Right(expectedAggregations))).noMoreThanOnce()

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(*)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/api/floats?offset=1&limit=1")).get

      status(resp) mustBe OK

      contentAsString(resp) mustEqual expectedJson

    }

  }
}
