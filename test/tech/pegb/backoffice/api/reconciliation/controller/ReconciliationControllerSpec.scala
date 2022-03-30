package tech.pegb.backoffice.api.reconciliation.controller

import java.time._
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.mvc.Headers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import tech.pegb.backoffice.domain.reconciliation.abstraction.Reconciliation
import tech.pegb.backoffice.domain.reconciliation.dto._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class ReconciliationControllerSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  val reconciliation = stub[Reconciliation]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[Reconciliation].to(reconciliation),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val config = inject[AppConfig]

  val reconSummary1 = InternReconSummaryDto(
    id = "0001",
    accountId = "1",
    accountNumber = "100.1",
    accountType = "distribution",
    accountMainType = "liability",
    currency = "KES",
    userUuid = "user0001",
    userFullName = "ujali".some,
    anyCustomerName = "ujali".some,
    reconDate = LocalDate.now(mockClock),
    endOfDayBalance = BigDecimal(100),
    valueChange = BigDecimal(0),
    difference = BigDecimal(0),
    transactionTotalAmount = BigDecimal(100),
    transactionTotalCount = 10,
    problematicInternReconResultsCount = 1,
    reconStatus = "FAIL",
    comments = None,
    updatedAt = None)

  val reconSummary2 = InternReconSummaryDto(
    id = "0002",
    accountId = "2",
    accountNumber = "200.1",
    accountType = "distribution",
    accountMainType = "asset",
    currency = "KES",
    userUuid = "user0002",
    userFullName = "ujali".some,
    anyCustomerName = "ujali".some,
    reconDate = LocalDate.now(mockClock),
    endOfDayBalance = BigDecimal(200),
    valueChange = BigDecimal(0),
    difference = BigDecimal(0),
    transactionTotalAmount = BigDecimal(200),
    transactionTotalCount = 20,
    problematicInternReconResultsCount = 0,
    reconStatus = "SUCCESS",
    comments = None,
    updatedAt = None)

  val reconSummary3 = InternReconSummaryDto(
    id = "0003",
    accountId = "3",
    accountNumber = "300.1",
    accountType = "distribution",
    accountMainType = "liability",
    currency = "KES",
    userUuid = "user0003",
    userFullName = "ujali".some,
    anyCustomerName = "ujali".some,
    reconDate = LocalDate.now(mockClock),
    endOfDayBalance = BigDecimal(300),
    valueChange = BigDecimal(0),
    difference = BigDecimal(0),
    transactionTotalAmount = BigDecimal(300),
    transactionTotalCount = 30,
    problematicInternReconResultsCount = 0,
    reconStatus = "SUCCESS",
    comments = None,
    updatedAt = None)

  "ReconciliationController getInternalRecon POSITIVE tests" should {

    "return list of internal recon summary without filter" in {

      (reconciliation.countInternalReconSummaries _).when(*)
        .returns(Future.successful(Right(3)))

      (reconciliation.getInternalReconSummaries _).when(*, *, *, *)
        .returns(Future.successful(Right(Seq(reconSummary1, reconSummary2, reconSummary3))))

      val resp = route(app, FakeRequest(GET, s"/internal_recons?order_by=-recon_date,id")).get

      val expected =
        s"""|{"total":3,
            |"results":[
            |{"id":"0001",
            |"account_number":"100.1",
            |"account_type":"distribution",
            |"account_main_type":"liability",
            |"currency":"KES",
            |"user_id":"user0001",
            |"user_full_name":"ujali",
            |"customer_name":"ujali",
            |"date":"1970-01-01T00:00:00Z",
            |"total_value":100,
            |"difference":0,
            |"total_txn":100,
            |"txn_count":10,
            |"incidents":1,
            |"status":"FAIL",
            |"comments":null,
            |"updated_at":null
            |},
            |{"id":"0002",
            |"account_number":"200.1",
            |"account_type":"distribution",
            |"account_main_type":"asset",
            |"currency":"KES",
            |"user_id":"user0002",
            |"user_full_name":"ujali",
            |"customer_name":"ujali",
            |"date":"1970-01-01T00:00:00Z",
            |"total_value":200,
            |"difference":0,
            |"total_txn":200,
            |"txn_count":20,
            |"incidents":0,
            |"status":"SUCCESS",
            |"comments":null,
            |"updated_at":null
            |},
            |{"id":"0003",
            |"account_number":"300.1",
            |"account_type":"distribution",
            |"account_main_type":"liability",
            |"currency":"KES",
            |"user_id":"user0003",
            |"user_full_name":"ujali",
            |"customer_name":"ujali",
            |"date":"1970-01-01T00:00:00Z",
            |"total_value":300,
            |"difference":0,
            |"total_txn":300,
            |"txn_count":30,
            |"incidents":0,
            |"status":"SUCCESS",
            |"comments":null,
            |"updated_at":null
            |}],
            |"limit":4,"offset":0}""".trim
          .stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of internal recon summary with filter" in {

      (reconciliation.countInternalReconSummaries _).when(*)
        .returns(Future.successful(Right(3)))

      (reconciliation.getInternalReconSummaries _).when(*, *, *, *)
        .returns(Future.successful(Right(Seq(reconSummary3))))

      val resp = route(app, FakeRequest(GET, s"/internal_recons?acc_type=liability&status=success")).get

      val expected =
        s"""|{"total":3,
            |"results":[
            |{"id":"0003",
            |"account_number":"300.1",
            |"account_type":"distribution",
            |"account_main_type":"liability",
            |"currency":"KES",
            |"user_id":"user0003",
            |"user_full_name":"ujali",
            |"customer_name":"ujali",
            |"date":"1970-01-01T00:00:00Z",
            |"total_value":300,
            |"difference":0,
            |"total_txn":300,
            |"txn_count":30,
            |"incidents":0,
            |"status":"SUCCESS",
            |"comments":null,
            |"updated_at":null
            |}],
            |"limit":4,"offset":0}"""
          .stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

  }

  val recon1 = InternReconResultDto(
    id = "1",
    internReconSummaryResultId = "1",
    reconDate = LocalDate.now(mockClock),
    accountId = "1",
    accountNumber = "100.01",
    currency = "PHP",
    currentTxnId = 13300L,
    currentTxnSequence = 1,
    currentTxnDirection = "credit",
    currentTxnTimestamp = LocalDateTime.now(mockClock),
    currentTxnAmount = BigDecimal(1000),
    currentTxnPreviousBalance = none,
    previousTxnId = 3300L.some,
    previousTxnSequence = 1.some,
    previousTxnDirection = "credit".some,
    previousTxnTimestamp = LocalDateTime.now(mockClock).some,
    previousTxnAmount = BigDecimal(1000).some,
    previousTxnPreviousBalance = None,
    reconStatus = "success")

  val recon2 = InternReconResultDto(
    id = "2",
    internReconSummaryResultId = "2",
    reconDate = LocalDate.now(mockClock),
    accountId = "1",
    accountNumber = "100.01",
    currency = "PHP",
    currentTxnId = 13300L,
    currentTxnSequence = 2,
    currentTxnDirection = "debit",
    currentTxnTimestamp = LocalDateTime.now(mockClock),
    currentTxnAmount = BigDecimal(500),
    previousTxnId = 3300L.some,
    previousTxnSequence = 2.some,
    previousTxnDirection = "debit".some,
    previousTxnTimestamp = LocalDateTime.now(mockClock).some,
    previousTxnAmount = BigDecimal(1000).some,
    currentTxnPreviousBalance = None,
    previousTxnPreviousBalance = None,
    reconStatus = "success")

  val recon3 = InternReconResultDto(
    id = "3",
    internReconSummaryResultId = "3",
    reconDate = LocalDate.now(mockClock),
    accountId = "2",
    accountNumber = "200.01",
    currency = "AED",
    currentTxnId = 14300L,
    currentTxnSequence = 1,
    currentTxnDirection = "credit",
    currentTxnTimestamp = LocalDateTime.now(mockClock),
    currentTxnAmount = BigDecimal(5000),
    previousTxnId = 4300L.some,
    previousTxnSequence = 1.some,
    previousTxnDirection = "credit".some,
    previousTxnTimestamp = LocalDateTime.now(mockClock).some,
    previousTxnAmount = BigDecimal(10000).some,
    currentTxnPreviousBalance = BigDecimal(3000).some,
    previousTxnPreviousBalance = None,
    reconStatus = "success")

  val recon4 = InternReconResultDto(
    id = "4",
    internReconSummaryResultId = "4",
    reconDate = LocalDate.now(mockClock),
    accountId = "2",
    accountNumber = "200.01",
    currency = "AED",
    currentTxnId = 14300L,
    currentTxnSequence = 2,
    currentTxnDirection = "debit",
    currentTxnTimestamp = LocalDateTime.now(mockClock),
    currentTxnAmount = BigDecimal(5000),
    previousTxnId = 4300L.some,
    previousTxnSequence = 2.some,
    previousTxnDirection = "debit".some,
    previousTxnTimestamp = LocalDateTime.now(mockClock).some,
    previousTxnAmount = BigDecimal(10000).some,
    currentTxnPreviousBalance = None,
    previousTxnPreviousBalance = BigDecimal(2000).some,
    reconStatus = "success")

  "ReconciliationController getInternalReconIncidents POSITIVE tests" should {

    "return list of internal recon results without filter" ignore {

      (reconciliation.countInternalReconResults _).when(*)
        .returns(Future.successful(Right(4)))

      (reconciliation.getInternalReconResults _).when(*, *, *, *)
        .returns(Future.successful(Right(Seq(recon1, recon2, recon3, recon4))))

      val resp = route(app, FakeRequest(GET, s"/internal_recons/incidents?order_by=-recon_date,incident_id")).get

      val expected =
        s"""
           |{"total":4,"results":[{"incident_id":"1","recon_id":"1","recon_date":"1970-01-01T00:00:00Z",
           |"account_number":"100.01","currency":"PHP","txn_id":"13300","txn_sequence":"1","txn_direction":"credit",
           |"txn_date":"1970-01-01T04:00:03Z","txn_amount":1000,"balance_before":null,"balance_after":null},
           |{"incident_id":"2","recon_id":"2","recon_date":"1970-01-01T00:00:00Z","account_number":"100.01",
           |"currency":"PHP","txn_id":"13300","txn_sequence":"2","txn_direction":"debit","txn_date":"1970-01-01T04:00:03Z",
           |"txn_amount":500,"balance_before":null,"balance_after":null},{"incident_id":"3","recon_id":"3",
           |"recon_date":"1970-01-01T00:00:00Z","account_number":"200.01","currency":"AED","txn_id":"14300",
           |"txn_sequence":"1","txn_direction":"credit","txn_date":"1970-01-01T04:00:03Z","txn_amount":5000,
           |"balance_before":null,"balance_after":3000},{"incident_id":"4","recon_id":"4","recon_date":"1970-01-01T00:00:00Z",
           |"account_number":"200.01","currency":"AED","txn_id":"14300","txn_sequence":"2","txn_direction":"debit",
           |"txn_date":"1970-01-01T04:00:03Z","txn_amount":5000,"balance_before":2000,"balance_after":null}],
           |"limit":4,"offset":0}""".trim.stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) contains expected
    }

    "return list of internal recon results with filter" ignore {

      (reconciliation.getInternalReconResults _).when(*, *, *, *)
        .returns(Future.successful(Right(Seq(recon1, recon2))))

      val resp = route(app, FakeRequest(GET, s"/internal_recons/incidents?account_number=100.1&start_recon_date=1970-01-01&end_recon_date=1970-01-01&order_by=-recon_date,-txn_direction")).get

      val expectedResult =
        s"""
           |{"total":2,"results":[{"incident_id":"1","recon_id":"1","recon_date":"1970-01-01T00:00:00Z",
           |"account_number":"100.01","currency":"PHP","txn_id":"13300","txn_sequence":"1","txn_direction":"credit",
           |"txn_date":${LocalDateTime.now(mockClock).toZonedDateTimeUTC.toJsonStr},"txn_amount":1000,"balance_before":null,"balance_after":null},
           |{"incident_id":"2","recon_id":"2","recon_date":"1970-01-01T00:00:00Z","account_number":"100.01",
           |"currency":"PHP","txn_id":"13300","txn_sequence":"2","txn_direction":"debit",
           |"txn_date":${LocalDateTime.now(mockClock).toZonedDateTimeUTC.toJsonStr},"txn_amount":500,"balance_before":null,"balance_after":null}],
           |"limit":4,"offset":0}
         """.trim.stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expectedResult
    }

    "return updated summary in PUT /reconciliation/internal/resolve" in {
      val id = "123"

      val reconSummary1 = InternReconSummaryDto(
        id = "123",
        accountId = "1",
        accountNumber = "100.1",
        accountType = "distribution",
        accountMainType = "liability",
        currency = "KES",
        userUuid = "user0001",
        userFullName = "ujali".some,
        anyCustomerName = "ujali".some,
        reconDate = LocalDate.now(mockClock),
        endOfDayBalance = BigDecimal(100),
        valueChange = BigDecimal(0),
        difference = BigDecimal(0),
        transactionTotalAmount = BigDecimal(100),
        transactionTotalCount = 10,
        problematicInternReconResultsCount = 1,
        reconStatus = "solved",
        comments = "manual transaction created".some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val jsonRequest =
        s"""
           |{"comments":"manual transaction created",
           |"updated_at":null
           |}
         """.stripMargin

      val expectedDto = InternReconDailySummaryResultResolve(
        comments = "manual transaction created",
        updatedBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = None)

      (reconciliation.resolveInternalReconSummary _)
        .when(id, expectedDto)
        .returns(Future.successful(Right(reconSummary1)))

      val fakeRequest = FakeRequest(PUT, s"/internal_recons/$id",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":"123",
           |"account_number":"100.1",
           |"account_type":"distribution",
           |"account_main_type":"liability",
           |"currency":"KES",
           |"user_id":"user0001",
           |"user_full_name":"ujali",
           |"customer_name":"ujali",
           |"date":"1970-01-01T00:00:00Z",
           |"total_value":100,
           |"difference":0,
           |"total_txn":100,
           |"txn_count":10,
           |"incidents":1,
           |"status":"solved",
           |"comments":"manual transaction created",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson
      status(resp) mustBe OK
    }

  }

  "ReconciliationController getInternalReconIncidents NEGATIVE tests" should {

    "return error when date_from is after date_to" in {
      val requestHeader = UUID.randomUUID().toString
      val resp = route(
        app,
        FakeRequest(GET, s"/internal_recons/incidents?account_number=100.1&start_recon_date=1970-01-05&end_recon_date=1970-01-01&order_by=-recon_date,-txn_direction")
          .withHeaders(Headers(("request-id" → requestHeader)))).get

      val expected =
        s"""|{"id":"$requestHeader","code":"InvalidRequest","msg":"start_recon_date must be before or equal to end_recon_date"}""".stripMargin.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustEqual expected
    }

    "return error when order_by is invalid" in {
      val requestHeader = UUID.randomUUID().toString
      val resp = route(
        app,
        FakeRequest(GET, s"/internal_recons/incidents?&order_by=-deadbeef")
          .withHeaders(Headers("request-id" → requestHeader))).get
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp).contains("invalid value for order_by found." +
        " Valid values: [account_number, balance_after, balance_before, created_at, currency, incident_id, recon_date," +
        " recon_id, txn_amount, txn_date, txn_direction, txn_id, txn_sequence]") mustBe true
    }

  }

}
