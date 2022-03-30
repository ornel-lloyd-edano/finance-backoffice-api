package tech.pegb.backoffice.api.transaction.controller

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import tech.pegb.backoffice.domain.model.Ordering.DESCENDING
import tech.pegb.backoffice.domain.transaction.abstraction.ManualTransactionManagement
import tech.pegb.backoffice.domain.transaction.dto.{ManualTxnCriteria, SettlementFxHistoryCriteria, SettlementRecentAccountCriteria}
import tech.pegb.backoffice.domain.transaction.model
import tech.pegb.backoffice.domain.transaction.model._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class ManualTransactionControllerSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  val manualTxnManager = stub[ManualTransactionManagement]
  val latestVersionService = stub[LatestVersionService]
  val appConf = inject[AppConfig]
  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[ManualTransactionManagement].to(manualTxnManager),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val manualTx1 = ManualTransaction(
    id = UUID.randomUUID(),
    status = "success",
    reason = "top up account for reconciliation",
    createdBy = "George Ogalo",
    createdAt = LocalDateTime.of(2019, 6, 15, 0, 0, 0),
    groupByManualTxnId = true,
    manualTxnLines = Seq(
      ManualTransactionLines(
        lineId = 1,
        accountNumber = "xxx 0001",
        currency = Currency.getInstance("KES"),
        direction = DirectionTypes.Credit,
        amount = BigDecimal(300.00),
        explanation = "0001 gets money"),
      ManualTransactionLines(
        lineId = 2,
        accountNumber = "xxx 0002",
        currency = Currency.getInstance("KES"),
        direction = DirectionTypes.Debit,
        amount = BigDecimal(300.00),
        explanation = "0002 transfers money"),
      ManualTransactionLines(
        lineId = 1,
        accountNumber = "xxx 0001",
        currency = Currency.getInstance("KES"),
        direction = DirectionTypes.Credit,
        amount = BigDecimal(10.00),
        explanation = "0001 pays fee"),
      ManualTransactionLines(
        lineId = 1,
        accountNumber = "xxx 9999",
        currency = Currency.getInstance("KES"),
        direction = DirectionTypes.Debit,
        amount = BigDecimal(10.00),
        explanation = "9999 gets fee")))

  "ManualTransactionController getManualTransactions" should {

    "return list of manual transactions without any filters" in {
      val criteria = ManualTxnCriteria()

      import tech.pegb.backoffice.domain.model._
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Future.successful(Right(Some(mockLatestVersion))))

      (manualTxnManager.countManualTransactionsByCriteria _).when(true, criteria)
        .returns(Future.successful(Right(1)))
      (manualTxnManager.getManualTransactionsByCriteria _).when(true, criteria, Seq(Ordering("created_at", Ordering.DESCENDING)), None, None)
        .returns(Future.successful(Right(Seq(manualTx1))))

      val resp = route(app, FakeRequest(GET, s"/manual_transactions?order_by=-created_at")).get

      val expected =
        s"""
           |{
           |"total":1,
           |"results":[
           |{"id":"${manualTx1.id}",
           |"manual_txn_lines":[
           |{"line_id":1,
           |"manual_txn_id":"${manualTx1.id}",
           |"account":"xxx 0001",
           |"currency":"KES",
           |"direction":"credit",
           |"amount":300.0,
           |"explanation":"0001 gets money"},
           |{"line_id":2,
           |"manual_txn_id":"${manualTx1.id}",
           |"account":"xxx 0002",
           |"currency":"KES",
           |"direction":"debit",
           |"amount":300.0,
           |"explanation":"0002 transfers money"},
           |{"line_id":1,
           |"manual_txn_id":"${manualTx1.id}",
           |"account":"xxx 0001",
           |"currency":"KES",
           |"direction":"credit",
           |"amount":10.0,
           |"explanation":"0001 pays fee"},
           |{"line_id":1,
           |"manual_txn_id":"${manualTx1.id}",
           |"account":"xxx 9999",
           |"currency":"KES",
           |"direction":"debit",
           |"amount":10.0,
           |"explanation":"9999 gets fee"}],
           |"status":"success",
           |"transaction_reason":"top up account for reconciliation",
           |"created_by":"George Ogalo",
           |"created_at":"2019-06-15T00:00:00Z"}],
           |"limit":null,
           |"offset":null
           |}
         |""".trim.stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) mustEqual expected
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe Some(mockLatestVersion)
    }

    "bad request if unknown order_by param" in {

      val resp = route(app, FakeRequest(GET, s"/manual_transactions?order_by=fried_chicken")).get

      status(resp) mustBe BAD_REQUEST
    }

  }

  "return history in GET /manual_transactions/currency_exchange_history" in {
    val criteria = SettlementFxHistoryCriteria()

    val expected = Seq(
      SettlementFxHistory(
        fxProvider = "Central Bank of Kenya: CBK",
        fromCurrencyId = 1,
        fromCurrency = "KES",
        fromIcon = "kes_icon",
        toCurrencyId = 2,
        toCurrency = "USD",
        toIcon = "usd_icon",
        fxRate = 0.09345,
        createdAt = LocalDateTime.of(2020, 1, 1, 0, 30, 0)),
      SettlementFxHistory(
        fxProvider = "Banko Sentral ng Pilipinas: BSP",
        fromCurrencyId = 1,
        fromCurrency = "KES",
        fromIcon = "kes_icon",
        toCurrencyId = 2,
        toCurrency = "PHP",
        toIcon = "php_icon",
        fxRate = 0.5033,
        createdAt = LocalDateTime.of(2019, 12, 25, 4, 45, 0)))

    (manualTxnManager.getSettlementFxHistory _)
      .when(criteria, Seq(tech.pegb.backoffice.domain.model.Ordering("created_at", DESCENDING)), None, None)
      .returns(Future.successful(Right(expected)))

    val resp = route(app, FakeRequest(GET, s"/manual_transactions/currency_exchange_history")
      .withHeaders(jsonHeaders)).get

    val expectedJson =
      s"""
         |[{
         |"fx_provider":"Central Bank of Kenya: CBK",
         |"from_currency":"KES",
         |"from_flag":"kes_icon",
         |"to_currency":"USD",
         |"to_flag":"usd_icon",
         |"fx_rate":0.09345,
         |"created_at":"2020-01-01T00:30:00Z"
         |},
         |{
         |"fx_provider":"Banko Sentral ng Pilipinas: BSP",
         |"from_currency":"KES",
         |"from_flag":"kes_icon",
         |"to_currency":"PHP",
         |"to_flag":"php_icon",
         |"fx_rate":0.5033,
         |"created_at":"2019-12-25T04:45:00Z"
         |}]""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe OK
    contentAsString(resp) mustBe expectedJson
  }

  "return recent accounts in GET /manual_transactions/frequently_used_accounts" in {
    val criteria = SettlementRecentAccountCriteria()

    val expected = Seq(
      model.SettlementRecentAccount(
        accountId = 1,
        accountUUID = UUID.randomUUID().toString,
        customerName = Some("Whizmo System"),
        accountNumber = "1201.01",
        accountName = Some("Whizmo KES"),
        balance = BigDecimal(150000.75),
        currency = "KES"),
      model.SettlementRecentAccount(
        accountId = 2,
        accountUUID = UUID.randomUUID().toString,
        customerName = Some("Whizmo System"),
        accountNumber = "1201.02",
        accountName = Some("Whizmo USD"),
        balance = BigDecimal(250000),
        currency = "USD"))

    (manualTxnManager.getSettlementRecentAccount _)
      .when(criteria, Some(appConf.SettlementConstants.fxRecentCount), None)
      .returns(Future.successful(Right(expected)))

    val resp = route(app, FakeRequest(GET, s"/manual_transactions/frequently_used_accounts")
      .withHeaders(jsonHeaders)).get

    val expectedJson =
      s"""
         |[{
         |"id":1,
         |"customer_name":"Whizmo System",
         |"account_number":"1201.01",
         |"balance":150000.75,
         |"currency":"KES"
         |},
         |{
         |"id":2,
         |"customer_name":"Whizmo System",
         |"account_number":"1201.02",
         |"balance":250000,
         |"currency":"USD"
         |}]""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe OK
    contentAsString(resp) mustBe expectedJson
  }
}
