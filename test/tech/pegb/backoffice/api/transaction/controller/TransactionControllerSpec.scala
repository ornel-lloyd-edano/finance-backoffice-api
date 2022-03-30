package tech.pegb.backoffice.api.transaction.controller

import java.time._
import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.mvc.Headers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import tech.pegb.backoffice.api.transaction.dto.{TxnToUpdateForCancellation, TxnToUpdateForReversal}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionManagement
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.domain.transaction.model.{Transaction, TransactionStatus}
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class TransactionControllerSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  val transactionManager = stub[TransactionManagement]
  val latestVersionService = stub[LatestVersionService]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[TransactionManagement].to(transactionManager),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val accountId1 = UUID.randomUUID()
  val accountId2 = UUID.randomUUID()
  val tx1 = Transaction(
    id = "133000",
    uniqueId = "1",
    sequence = 1,
    primaryAccountId = accountId1,
    primaryAccountName = "George Ogalo",
    primaryAccountNumber = "100.1",
    primaryAccountType = "standard",
    primaryAccountCustomerName = Some("Lloyd"),
    secondaryAccountId = accountId2,
    secondaryAccountName = "Loyd Edano",
    secondaryAccountNumber = "300.1",
    direction = "credit",
    `type` = "p2p_transactions",
    amount = BigDecimal(1000),
    currency = Currency.getInstance("AED"),
    channel = "IOS_APP",
    explanation = Some("transfer"),
    effectiveRate = None,
    costRate = None,
    status = TransactionStatus("success"),
    instrument = None,
    createdAt = LocalDateTime.now(mockClock))
  val tx2 = Transaction(
    id = "133000",
    uniqueId = "1",
    sequence = 2,
    primaryAccountId = accountId2,
    primaryAccountName = "Loyd Edano",
    primaryAccountNumber = "300.1",
    primaryAccountType = "standard",
    primaryAccountCustomerName = Some("Lloyd"),
    secondaryAccountId = accountId1,
    secondaryAccountName = "George Ogalo",
    secondaryAccountNumber = "100.1",
    direction = "debit",
    `type` = "p2p_transactions",
    amount = BigDecimal(1000),
    currency = Currency.getInstance("AED"),
    channel = "IOS_APP",
    explanation = Some("transfer"),
    effectiveRate = None,
    costRate = None,
    status = TransactionStatus("success"),
    instrument = None,
    createdAt = LocalDateTime.now(mockClock))
  val tx3 = Transaction(
    id = "133001",
    uniqueId = "1",
    sequence = 1,
    primaryAccountId = accountId1,
    primaryAccountName = "George Ogalo",
    primaryAccountNumber = "100.1",
    primaryAccountType = "standard",
    primaryAccountCustomerName = Some("Lloyd"),
    secondaryAccountId = accountId2,
    secondaryAccountName = "Loyd Edano",
    secondaryAccountNumber = "300.1",
    direction = "credit",
    `type` = "p2p_transactions",
    amount = BigDecimal(500),
    currency = Currency.getInstance("AED"),
    channel = "IOS_APP",
    explanation = Some("transfer"),
    effectiveRate = None,
    costRate = None,
    status = TransactionStatus("success"),
    instrument = None,
    createdAt = LocalDateTime.now(mockClock),
    updatedAt = LocalDateTime.of(2019, 6, 30, 0, 0).toOption)
  val tx4 = Transaction(
    id = "133001",
    uniqueId = "1",
    sequence = 2,
    primaryAccountId = accountId2,
    primaryAccountName = "Loyd Edano",
    primaryAccountNumber = "300.1",
    primaryAccountType = "standard",
    primaryAccountCustomerName = Some("Lloyd"),
    secondaryAccountId = accountId1,
    secondaryAccountName = "George Ogalo",
    secondaryAccountNumber = "100.1",
    direction = "debit",
    `type` = "p2p_transactions",
    amount = BigDecimal(1000),
    currency = Currency.getInstance("AED"),
    channel = "IOS_APP",
    explanation = Some("transfer"),
    effectiveRate = None,
    costRate = None,
    status = TransactionStatus("success"),
    instrument = None,
    createdAt = LocalDateTime.now(mockClock))

  "TransactionController" should {

    "forward to core api for cancelling transactions" in {
      val mockRequestFrom = "Daniel"
      val mockRequestDate = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)
      val mockId = "16789"

      val dto = TxnToUpdateForCancellation(
        reason = "some weird but acceptable reason",
        lastUpdatedAt = Some(ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)))

      val domainDto = dto.asDomain(mockId, mockRequestFrom, mockRequestDate)

      domainDto.txnId mustBe mockId
      domainDto.reason mustBe dto.reason
      domainDto.cancelledBy mustBe mockRequestFrom
      domainDto.cancelledAt mustBe mockRequestDate.toLocalDateTimeUTC
      domainDto.lastUpdatedAt mustBe Some(LocalDateTime.of(2019, 1, 1, 0, 0))

      val mockResult = Seq(Transaction(
        id = mockId,
        uniqueId = "1",
        sequence = 1,
        primaryAccountId = accountId1,
        primaryAccountName = "Loyd Edano",
        primaryAccountNumber = "300.1",
        primaryAccountType = "standard",
        primaryAccountCustomerName = Some("Lloyd"),
        secondaryAccountId = accountId2,
        secondaryAccountName = "George Ogalo",
        secondaryAccountNumber = "100.1",
        direction = "debit",
        `type` = "p2p_transactions",
        amount = BigDecimal(9999),
        currency = Currency.getInstance("AED"),
        channel = "IOS_APP",
        explanation = Some("transfer"),
        effectiveRate = None,
        costRate = None,
        status = TransactionStatus("cancelled"),
        instrument = None,
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0)))

      (transactionManager.cancelTransaction _).when(domainDto)
        .returns(mockResult.toRight.toFuture)

      val jsonRequest =
        s"""
           |{"reason": "${dto.reason}",
           |"updated_at":"${mockRequestDate}"
           |}
         """.stripMargin

      val resp = route(app, FakeRequest(PUT, s"/transactions/$mockId/cancel",
        Headers(
          CONTENT_TYPE → JSON,
          requestDateHeaderKey → mockRequestDate.toString,
          requestFromHeaderKey → mockRequestFrom),
        jsonRequest)).get

      status(resp) mustBe OK

      val expected =
        s"""
           |[{
           |"id":"16789",
           |"sequence":1,
           |"primary_account_id":"$accountId1",
           |"primary_account_name":"Loyd Edano",
           |"primary_account_number":"300.1",
           |"primary_account_customer_name":"Lloyd",
           |"secondary_account_id":"$accountId2",
           |"secondary_account_name":"George Ogalo",
           |"secondary_account_number":"100.1",
           |"direction":"debit",
           |"type":"p2p_transactions",
           |"amount":9999,"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"transfer",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"cancelled",
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null,
           |"previous_balance":null,
           |"reason":"${dto.reason}"}]
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }

    "forward to core api for reversing transactions" in {
      val mockRequestFrom = "Daniel"
      val mockRequestDate = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)
      val mockId = "16789"

      val dto = TxnToUpdateForReversal(
        reason = "some weird but acceptable reason",
        isFeeReversed = true.toOption,
        lastUpdatedAt = Some(ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)))

      val domainDto = dto.asDomain(mockId, mockRequestFrom, mockRequestDate)

      domainDto.txnId mustBe mockId
      domainDto.reason mustBe dto.reason.toOption
      domainDto.reversedBy mustBe mockRequestFrom
      domainDto.reversedAt mustBe mockRequestDate.toLocalDateTimeUTC

      val newTxnId = "16790"
      val mockResult = Seq(Transaction(
        id = newTxnId,
        uniqueId = "1",
        sequence = 1,
        primaryAccountId = accountId1,
        primaryAccountName = "Loyd Edano",
        primaryAccountNumber = "300.1",
        primaryAccountType = "standard",
        primaryAccountCustomerName = Some("Lloyd"),
        secondaryAccountId = accountId2,
        secondaryAccountName = "George Ogalo",
        secondaryAccountNumber = "100.1",
        direction = "debit",
        `type` = "p2p_transactions",
        amount = BigDecimal(9999),
        currency = Currency.getInstance("AED"),
        channel = "IOS_APP",
        explanation = Some("transfer"),
        effectiveRate = None,
        costRate = None,
        status = TransactionStatus("success"),
        instrument = None,
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0)))

      (transactionManager.revertTransaction _).when(domainDto)
        .returns(mockResult.toRight.toFuture)

      val jsonRequest =
        s"""
           |{
           |"reason": "${dto.reason}",
           |"is_fee_reversed":${dto.isFeeReversed.get},
           |"updated_at":"${mockRequestDate}"
           |}
         """.stripMargin

      val resp = route(app, FakeRequest(PUT, s"/transactions/$mockId/revert",
        Headers(
          CONTENT_TYPE → JSON,
          requestDateHeaderKey → mockRequestDate.toString,
          requestFromHeaderKey → mockRequestFrom),
        jsonRequest)).get

      status(resp) mustBe OK

      val expected =
        s"""
           |[{
           |"id":"$newTxnId",
           |"sequence":1,
           |"primary_account_id":"$accountId1",
           |"primary_account_name":"Loyd Edano",
           |"primary_account_number":"300.1",
           |"primary_account_customer_name":"Lloyd",
           |"secondary_account_id":"$accountId2",
           |"secondary_account_name":"George Ogalo",
           |"secondary_account_number":"100.1",
           |"direction":"debit",
           |"type":"p2p_transactions",
           |"amount":9999,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"transfer",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null,
           |"previous_balance":null,
           |"reason":"${dto.reason}"}]
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }

    "return list of transaction of user's accounts" in {
      val customerId = UUID.randomUUID()
      val accountId = UUID.randomUUID()
      val criteria = TransactionCriteria(customerId = Option(customerId.toUUIDLike), accountId = Option(accountId.toUUIDLike),
        startDate = Some(LocalDateTime.now(mockClock)), endDate = Some(LocalDateTime.now(mockClock)),
        transactionType = None, channel = None, status = None)

      (transactionManager.countTransactionsByCriteria _).when(criteria)
        .returns(Future.successful(Right(4)))

      (transactionManager.getTransactionsByCriteria _).when(criteria, "-created_at,sequence".asDomain, None, None)
        .returns(Future.successful(Right(Seq(tx1, tx2, tx3, tx4))))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/customers/$customerId/accounts/$accountId/transactions?date_from=${LocalDateTime.now(mockClock)}&date_to=${LocalDateTime.now(mockClock)}")).get
      import tech.pegb.backoffice.api.json.Implicits._
      val expected =
        s"""
           |{
           |"total":4,
           |"results":[
            |{
               |"id":"133000",
               |"sequence":1,
               |"primary_account_id":"$accountId1",
               |"primary_account_name":"George Ogalo",
               |"primary_account_number":"100.1",
               |"primary_account_customer_name":"Lloyd",
               |"secondary_account_id":"$accountId2",
               |"secondary_account_name":"Loyd Edano",
               |"secondary_account_number":"300.1",
               |"direction":"credit",
               |"type":"p2p_transactions",
               |"amount":1000,
               |"currency":"AED",
               |"exchange_currency":null,
               |"channel":"IOS_APP",
               |"explanation":"transfer",
               |"effective_rate":null,
               |"cost_rate":null,
               |"status":"success",
               |"created_at":${LocalDateTime.now(mockClock).toZonedDateTimeUTC.toJsonStr},
               |"updated_at":null,
               |"previous_balance":null,
               |"reason":null
            |},
            |{
               |"id":"133000",
               |"sequence":2,
               |"primary_account_id":"$accountId2",
               |"primary_account_name":"Loyd Edano",
               |"primary_account_number":"300.1",
               |"primary_account_customer_name":"Lloyd",
               |"secondary_account_id":"$accountId1",
               |"secondary_account_name":"George Ogalo",
               |"secondary_account_number":"100.1",
               |"direction":"debit",
               |"type":"p2p_transactions",
               |"amount":1000,
               |"currency":"AED",
               |"exchange_currency":null,
               |"channel":"IOS_APP",
               |"explanation":"transfer",
               |"effective_rate":null,
               |"cost_rate":null,
               |"status":"success",
               |"created_at":${LocalDateTime.now(mockClock).toZonedDateTimeUTC.toJsonStr},
               |"updated_at":null,
               |"previous_balance":null,
               |"reason":null
            |},
            |{
               |"id":"133001",
               |"sequence":1,
               |"primary_account_id":"$accountId1",
               |"primary_account_name":"George Ogalo",
               |"primary_account_number":"100.1",
               |"primary_account_customer_name":"Lloyd",
               |"secondary_account_id":"$accountId2",
               |"secondary_account_name":"Loyd Edano",
               |"secondary_account_number":"300.1",
               |"direction":"credit",
               |"type":"p2p_transactions",
               |"amount":500,
               |"currency":"AED",
               |"exchange_currency":null,
               |"channel":"IOS_APP",
               |"explanation":"transfer",
               |"effective_rate":null,
               |"cost_rate":null,
               |"status":"success",
               |"created_at":${LocalDateTime.now(mockClock).toZonedDateTimeUTC.toJsonStr},
               |"updated_at":${LocalDateTime.of(2019, 6, 30, 0, 0).toZonedDateTimeUTC.toJsonStr},
               |"previous_balance":null,
               |"reason":null
            |},
            |{
               |"id":"133001",
               |"sequence":2,
               |"primary_account_id":"$accountId2",
               |"primary_account_name":"Loyd Edano",
               |"primary_account_number":"300.1",
               |"primary_account_customer_name":"Lloyd",
               |"secondary_account_id":"$accountId1",
               |"secondary_account_name":"George Ogalo",
               |"secondary_account_number":"100.1",
               |"direction":"debit",
               |"type":"p2p_transactions",
               |"amount":1000,
               |"currency":"AED",
               |"exchange_currency":null,
               |"channel":"IOS_APP",
               |"explanation":"transfer",
               |"effective_rate":null,
               |"cost_rate":null,
               |"status":"success",
               |"created_at":${LocalDateTime.now(mockClock).toZonedDateTimeUTC.toJsonStr},
               |"updated_at":null,
               |"previous_balance":null,
               |"reason":null
            |}],
           |"limit":null,
           |"offset":null
           |}
         |""".stripMargin.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return error when date_from is after date_to" in {
      val customerId = UUID.randomUUID()
      val accountId = UUID.randomUUID()

      val mockClockAhead = Clock.fixed(Instant.ofEpochMilli(4600), ZoneId.systemDefault())

      val requestHeader = UUID.randomUUID().toString
      val resp = route(
        app,
        FakeRequest(GET, s"/customers/$customerId/accounts/$accountId/transactions?date_from=${LocalDateTime.now(mockClockAhead)}&date_to=${LocalDateTime.now(mockClock)}")
          .withHeaders(Headers(("request-id" → requestHeader)))).get

      val expected =
        s"""|{"id":"$requestHeader","code":"InvalidRequest","msg":"date_from must be before or equal to date_to"}""".stripMargin.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustEqual expected
    }

    "return error when limit > pagination.max-cap value" in {
      val customerId = UUID.randomUUID()
      val accountId = UUID.randomUUID()
      val PaginationCap = conf.get[Int]("pagination.max-cap")

      val biggerLimit = PaginationCap + 1

      val requestHeader = UUID.randomUUID().toString
      val resp = route(
        app,
        FakeRequest(GET, s"/customers/$customerId/accounts/$accountId/transactions?date_from=${LocalDateTime.now(mockClock)}&date_to=${LocalDateTime.now(mockClock)}&limit=${biggerLimit}")
          .withHeaders(Headers(("request-id" → requestHeader)))).get

      val expected =
        s"""|{"id":"$requestHeader","code":"InvalidRequest","msg":"Limit provided($biggerLimit) is greater than PaginationMaxCap $PaginationCap"}""".stripMargin.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustEqual expected
    }
  }

}
