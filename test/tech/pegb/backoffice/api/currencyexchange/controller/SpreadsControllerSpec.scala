package tech.pegb.backoffice.api.currencyexchange.controller

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.mvc.Headers
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, route, status, _}
import tech.pegb.backoffice.api.currencyexchange.Constants
import tech.pegb.backoffice.api.currencyexchange.controllers.SpreadsController
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.currencyexchange.abstraction.{CurrencyExchangeManagement, SpreadsManagement}
import tech.pegb.backoffice.domain.currencyexchange.dto.{SpreadCriteria, SpreadToCreate}
import tech.pegb.backoffice.domain.currencyexchange.model.{CurrencyExchange, Spread}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class SpreadsControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {
  implicit val ec = TestExecutionContext.genericOperations
  private val spreadsManagement = stub[SpreadsManagement]
  private val currencyExchMgmt = stub[CurrencyExchangeManagement]
  val latestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[SpreadsManagement].to(spreadsManagement),
      bind[CurrencyExchangeManagement].to(currencyExchMgmt),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  import TestHelper._

  "SpreadsController" should {
    "respond with 200 OK with payload SpreadToRead json in /spreads/:id if uuid was found" in {

      val mockId = UUID.randomUUID()

      val expectedSpread = Spread.empty.copy(id = mockId)

      (spreadsManagement.getSpread _).when(mockId)
        .returns(Future.successful(Right(expectedSpread)))

      val resp = route(app, FakeRequest(GET, s"/spreads/$mockId")).get

      val expectedJson =
        s"""
           |{"id":"$mockId",
           |"currency_exchange_id":"${expectedSpread.currencyExchange.id}",
           |"buy_currency":"${expectedSpread.currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpread.currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpread.transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpread.spread},
           |"updated_by":null,
           |"updated_at":null
           |}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
    }

    "respond 200 OK with payload SpreadToRead json in /currency_exchanges/:fxId/spreads/:id if uuid was found" in {

      val mockId = UUID.randomUUID()
      val spreadId = UUID.randomUUID()

      val expectedSpread = Spread.empty.copy(id = mockId)

      (spreadsManagement.getSpread _).when(spreadId)
        .returns(Future.successful(Right(expectedSpread)))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges/$mockId/spreads/$spreadId")).get

      val expectedJson =
        s"""
           |{"id":"$mockId",
           |"currency_exchange_id":"${expectedSpread.currencyExchange.id}",
           |"buy_currency":"${expectedSpread.currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpread.currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpread.transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpread.spread},
           |"updated_by":null,
           |"updated_at":null
           |}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
    }

    "respond 404 NotFound in /spreads/:id if uuid was not found" in {
      val mockId = UUID.randomUUID()

      val expectedError = ServiceError.notFoundError(s"Currency spread id [$mockId] was not found", UUID.randomUUID().toOption)

      (spreadsManagement.getSpread _).when(mockId)
        .returns(Future.successful(Left(expectedError)))

      val resp = route(app, FakeRequest(GET, s"/spreads/$mockId")
        .withHeaders(Headers("request-id" → mockRequestId.toString))).get

      val expectedJson =
        s"""
           |{"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"${expectedError.message}",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe NOT_FOUND
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
    }

    "respond with 200 OK with payload PaginatedResult[SpreadToRead] json in /spreads" in {

      val expectedSpreads = Seq(
        Spread.empty.copy(currencyExchange = CurrencyExchange.empty.copy(currency = "KES")),
        Spread.empty.copy(currencyExchange = CurrencyExchange.empty.copy(currency = "TZS")),
        Spread.empty.copy(currencyExchange = CurrencyExchange.empty.copy(currency = "ZAR")))

      val noCriteria = SpreadCriteria(partialMatchFields = Constants.validSpreadsPartialMatchFields.filterNot(_ == "disabled"))

      (spreadsManagement.countSpreadByCriteria _).when(noCriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(noCriteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/spreads")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${expectedSpreads(0).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(0).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${expectedSpreads(1).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(1).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(1).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${expectedSpreads(2).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(2).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(2).spread},
           |"updated_by":null,
           |"updated_at":null
           |}
           |],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "respond with 200 OK with payload PaginatedResult[SpreadToRead] json matching given partial uuid in /spreads?id=ce3b-441e" in {
      val expectedSpreads = Seq(
        Spread.empty.copy(
          id = UUID.fromString("4d7ca68c-ce3b-441e-ba9f-2fc64bf43dbf"), //ce3b-441e partial match
          currencyExchange = CurrencyExchange.empty.copy(currency = "KES")),
        Spread.empty.copy(
          id = UUID.fromString("26ffce3b-441e-4604-b0dd-5dea8dbae0a8"), //ce3b-441e partial match
          currencyExchange = CurrencyExchange.empty.copy(currency = "TZS")),
        Spread.empty.copy(
          id = UUID.fromString("9691ce48-8db2-4770-ce3b-441e4bf43dbf"), //ce3b-441e partial match
          currencyExchange = CurrencyExchange.empty.copy(currency = "ZAR")))

      val partialMatchIdcriteria = SpreadCriteria(
        id = Option(UUIDLike("ce3b-441e")),
        partialMatchFields = Constants.validSpreadsPartialMatchFields.filterNot(_ == "disabled"))

      (spreadsManagement.countSpreadByCriteria _).when(partialMatchIdcriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(partialMatchIdcriteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(partialMatchIdcriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/spreads?id=ce3b-441e")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${expectedSpreads(0).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(0).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${expectedSpreads(1).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(1).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(1).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${expectedSpreads(2).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(2).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(2).spread},
           |"updated_by":null,
           |"updated_at":null
           |}
           |],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "respond with 200 OK with payload PaginatedResult[SpreadToRead] json matching given partial currency_exchange_id in /spreads?currency_exchange_id=a0f8-5ceb18" in {
      val expectedSpreads = Seq(
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(
            id = UUID.fromString("4d7ca68c-ce3b-441e-a0f8-5ceb18f43dbf"), //a0f8-5ceb18 partial match
            currency = "KES")),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(
            id = UUID.fromString("26ffce3b-441e-4604-a0f8-5ceb18bae0a8"), //a0f8-5ceb18 partial match
            currency = "TZS")),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(
            id = UUID.fromString("9691ce48-8db2-4770-a0f8-5ceb18f43dbf"), //a0f8-5ceb18 partial match
            currency = "ZAR")))

      val partialMatchIdcriteria = SpreadCriteria(
        currencyExchangeId = Option(UUIDLike("a0f8-5ceb18")),
        partialMatchFields = Constants.validSpreadsPartialMatchFields.filterNot(_ == "disabled"))

      (spreadsManagement.countSpreadByCriteria _).when(partialMatchIdcriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(partialMatchIdcriteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(partialMatchIdcriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/spreads?currency_exchange_id=a0f8-5ceb18")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${expectedSpreads(0).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(0).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${expectedSpreads(1).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(1).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(1).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${expectedSpreads(2).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(2).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(2).spread},
           |"updated_by":null,
           |"updated_at":null
           |}
           |],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "respond with 200 OK with payload PaginatedResult[SpreadToRead] json matching given currency in /spreads?currency=AED" in {
      val expectedSpreads = Seq(
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "AED", baseCurrency = "USD")),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "AED", baseCurrency = "USD")),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "AED", baseCurrency = "USD")))

      val currencyCriteria = SpreadCriteria(
        currencyCode = Option("AED"),
        partialMatchFields = Constants.validSpreadsPartialMatchFields.filterNot(_ == "disabled"))

      (spreadsManagement.countSpreadByCriteria _).when(currencyCriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(currencyCriteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(currencyCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/spreads?currency=AED")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${expectedSpreads(0).currencyExchange.id}",
           |"buy_currency":"AED",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${expectedSpreads(1).currencyExchange.id}",
           |"buy_currency":"AED",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(1).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${expectedSpreads(2).currencyExchange.id}",
           |"buy_currency":"AED",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(2).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(2).spread},
           |"updated_by":null,
           |"updated_at":null
           |}
           |],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "respond with 200 OK with payload PaginatedResult[SpreadToRead] json matching given transaction_type in /spreads?transaction_type=international_remittance" in {
      val InterNatlRemitTxnType = TransactionType("international_remittance")

      val expectedSpreads = Seq(
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "AED"),
          transactionType = InterNatlRemitTxnType, channel = Option(Channel("some channel"))),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "USD"),
          transactionType = InterNatlRemitTxnType, channel = Option(Channel("some channel"))),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "KES"),
          transactionType = InterNatlRemitTxnType, channel = Option(Channel("some channel"))))

      val currencyCriteria = SpreadCriteria(
        transactionType = Option(InterNatlRemitTxnType),
        partialMatchFields = Constants.validSpreadsPartialMatchFields.filterNot(_ == "disabled"))

      (spreadsManagement.countSpreadByCriteria _).when(currencyCriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(currencyCriteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(currencyCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/spreads?transaction_type=international_remittance")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${expectedSpreads(0).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(0).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"some channel",
           |"institution":null,
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${expectedSpreads(1).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(1).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"some channel",
           |"institution":null,
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${expectedSpreads(2).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"some channel",
           |"institution":null,
           |"spread":${expectedSpreads(2).spread},
           |"updated_by":null,
           |"updated_at":null
           |}
           |],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "respond with 200 OK with payload PaginatedResult[SpreadToRead] json matching given channel in /spreads?channel=mobile_money" in {
      val MobileMoneyChannel = Channel("mobile_money")

      val expectedSpreads = Seq(
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "AED"),
          channel = Option(MobileMoneyChannel), transactionType = TransactionType("international_remittance")),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "USD"),
          channel = Option(MobileMoneyChannel), transactionType = TransactionType("international_remittance")),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "KES"),
          channel = Option(MobileMoneyChannel), transactionType = TransactionType("international_remittance")))

      val currencyCriteria = SpreadCriteria(
        channel = Option(MobileMoneyChannel),
        partialMatchFields = Constants.validSpreadsPartialMatchFields.filterNot(_ == "disabled"))

      (spreadsManagement.countSpreadByCriteria _).when(currencyCriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(currencyCriteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(currencyCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/spreads?channel=mobile_money")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${expectedSpreads(0).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(0).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":"mobile_money",
           |"institution":null,
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${expectedSpreads(1).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(1).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":"mobile_money",
           |"institution":null,
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${expectedSpreads(2).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":"mobile_money",
           |"institution":null,
           |"spread":${expectedSpreads(2).spread},
           |"updated_by":null,
           |"updated_at":null
           |}
           |],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "respond with 200 OK with payload PaginatedResult[SpreadToRead] json matching given institution in /spreads?institution=mpesa" in {
      val MpesaInstitution = "mpesa"

      val expectedSpreads = Seq(
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "AED"), channel = Option(Channel("some channel")),
          recipientInstitution = Option(MpesaInstitution), transactionType = TransactionType("international_remittance")),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "USD"), channel = Option(Channel("some channel")),
          recipientInstitution = Option(MpesaInstitution), transactionType = TransactionType("international_remittance")),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = "KES"), channel = Option(Channel("some channel")),
          recipientInstitution = Option(MpesaInstitution), transactionType = TransactionType("international_remittance")))

      val currencyCriteria = SpreadCriteria(
        recipientInstitution = Option(MpesaInstitution),
        partialMatchFields = Constants.validSpreadsPartialMatchFields.filterNot(_ == "disabled"))

      (spreadsManagement.countSpreadByCriteria _).when(currencyCriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(currencyCriteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(currencyCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/spreads?institution=mpesa")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${expectedSpreads(0).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(0).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":"some channel",
           |"institution":"mpesa",
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${expectedSpreads(1).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(1).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":"some channel",
           |"institution":"mpesa",
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${expectedSpreads(2).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":"some channel",
           |"institution":"mpesa",
           |"spread":${expectedSpreads(2).spread},
           |"updated_by":null,
           |"updated_at":null
           |}
           |],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "respond with 200 OK with payload PaginatedResult[SpreadToRead] json matching multiple criteria fields in /spreads?channel=mobile_money&transaction_type=international_remittance&currency=USD&institution=mpesa" in {
      val MobileMoneyChannel = Channel("mobile_money")
      val CurrencyExTxnType = TransactionType("international_remittance")
      val USDCurrency = "USD"
      val MpesaInstitution = "mpesa"

      val expectedSpreads = Seq(
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = USDCurrency, baseCurrency = "KES"),
          transactionType = CurrencyExTxnType, channel = Option(MobileMoneyChannel), recipientInstitution = Option(MpesaInstitution)),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = USDCurrency, baseCurrency = "INR"),
          transactionType = CurrencyExTxnType, channel = Option(MobileMoneyChannel), recipientInstitution = Option(MpesaInstitution)),
        Spread.empty.copy(
          currencyExchange = CurrencyExchange.empty.copy(currency = USDCurrency, baseCurrency = "AED"),
          transactionType = CurrencyExTxnType, channel = Option(MobileMoneyChannel), recipientInstitution = Option(MpesaInstitution)))

      val currencyCriteria = SpreadCriteria(
        currencyCode = Option(USDCurrency),
        transactionType = Option(CurrencyExTxnType),
        channel = Option(MobileMoneyChannel),
        recipientInstitution = Option(MpesaInstitution),
        partialMatchFields = Constants.validSpreadsPartialMatchFields.filterNot(_ == "disabled"))

      (spreadsManagement.countSpreadByCriteria _).when(currencyCriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(currencyCriteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(currencyCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/spreads?channel=mobile_money&transaction_type=international_remittance&currency=USD&institution=mpesa")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${expectedSpreads(0).currencyExchange.id}",
           |"buy_currency":"USD",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"mobile_money",
           |"institution":"mpesa",
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${expectedSpreads(1).currencyExchange.id}",
           |"buy_currency":"USD",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"mobile_money",
           |"institution":"mpesa",
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${expectedSpreads(2).currencyExchange.id}",
           |"buy_currency":"USD",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"mobile_money",
           |"institution":"mpesa",
           |"spread":${expectedSpreads(2).spread},
           |"updated_by":null,
           |"updated_at":null
           |}
           |],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "respond with 200 OK with payload PaginatedResult[SpreadToRead] json limited by limit and offset params in /spreads?limit=1&offset=2" in {
      val expectedSpreads = Seq(
        Spread.empty.copy(currencyExchange = CurrencyExchange.empty.copy(currency = "KES")),
        Spread.empty.copy(currencyExchange = CurrencyExchange.empty.copy(currency = "TZS")),
        Spread.empty.copy(currencyExchange = CurrencyExchange.empty.copy(currency = "ZAR")))

      val noCriteria = SpreadCriteria(partialMatchFields = Constants.validSpreadsPartialMatchFields.filterNot(_ == "disabled"))

      (spreadsManagement.countSpreadByCriteria _).when(noCriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(noCriteria, Seq(), Option(1), Option(2))
        .returns(Future.successful(Right(expectedSpreads.tail.tail)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/spreads?limit=1&offset=2")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${expectedSpreads(2).currencyExchange.id}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(2).transactionType}",
           |"channel":null,
           |"institution":null,
           |"spread":${expectedSpreads(2).spread},
           |"updated_by":null,
           |"updated_at":null
           |}
           |],
           |"limit":1,
           |"offset":2}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "respond with 200 OK with payload PaginatedResult[SpreadToRead] json total=0 and results= [] if HEAD method was used" in {
      val expectedSpreads = Seq(
        Spread.empty.copy(currencyExchange = CurrencyExchange.empty.copy(currency = "KES")),
        Spread.empty.copy(currencyExchange = CurrencyExchange.empty.copy(currency = "TZS")),
        Spread.empty.copy(currencyExchange = CurrencyExchange.empty.copy(currency = "ZAR")))

      val noCriteria = SpreadCriteria(partialMatchFields = Constants.validSpreadsPartialMatchFields.filterNot(_ == "disabled"))

      //Note: even if you remove the mocking being done below, it will not result to NPE because the lazy argument evaluation in executeIfGETMethod
      (spreadsManagement.countSpreadByCriteria _).when(noCriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(noCriteria, Seq(), Option(1), Option(2))
        .returns(Future.successful(Right(expectedSpreads.tail.tail)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(HEAD, s"/spreads")).get

      val expectedJson =
        s"""{"total":0,
           |"results":[],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "respond with 400 BadRequest with payload ApiError if order_by is given not allowed values in /spreads?order_by=account_number" in {
      val mockRequestId = UUID.randomUUID()

      val resp = route(app, FakeRequest(GET, s"/spreads?order_by=account_number")
        .withHeaders(requestIdHeaderKey → mockRequestId.toString)).get

      val expectedJson =
        s"""
           |{"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"invalid field for order_by found. Valid fields: ${Constants.validSpreadsOrderByFields.defaultMkString}"}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe BAD_REQUEST
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
    }

    "respond with 400 BadRequest with payload ApiError if partial_match is given not allowed values in /spreads?channel=mobil&partial_match=channel" in {
      val mockRequestId = UUID.randomUUID()

      val resp = route(app, FakeRequest(GET, s"/spreads?channel=mobil&partial_match=channel")
        .withHeaders(requestIdHeaderKey → mockRequestId.toString)).get

      val expectedJson =
        s"""
           |{"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"invalid field for partial matching found. Valid fields: [currency_exchange_id, disabled, id]"}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe BAD_REQUEST
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
    }

  }

  "SpreadsControllerSpec createSpreads" should {
    "respond with CREATED when spread is successfully created" in {
      val fxId = UUID.randomUUID()
      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$fxId",
           |  "transaction_type": "international_remittance",
           |  "channel": "bank",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val domainDto = SpreadToCreate(
        currencyExchangeId = fxId,
        transactionType = TransactionType("international_remittance"),
        channel = Some(Channel("bank")),
        institution = Some("Mashreq"),
        spread = BigDecimal(0.01),
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom)

      val mockCurrencyExchange = CurrencyExchange.empty
        .copy(id = fxId, status = "active", currency = java.util.Currency.getInstance("USD"), baseCurrency = java.util.Currency.getInstance("KES"), provider = "Currency Cloud")

      val expectedSpread = Spread.empty.copy(
        currencyExchange = mockCurrencyExchange,
        transactionType = TransactionType("international_remittance"),
        channel = Option(Channel("bank")),
        recipientInstitution = Option("Mashreq"),
        spread = BigDecimal(0.01))

      (spreadsManagement.createSpread(_: SpreadToCreate)(_: UUID)).when(domainDto, *)
        .returns(Future.successful(Right(expectedSpread)))

      val expectedJson =
        s"""{"id":"${expectedSpread.id}",
           |"currency_exchange_id":"$fxId",
           |"buy_currency":"USD",
           |"sell_currency":"KES",
           |"transaction_type":"international_remittance",
           |"channel":"bank",
           |"institution":"Mashreq",
           |"spread":0.01,
           |"updated_by":null,
           |"updated_at":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }
    "respond with BAD_REQUEST when transaction_type is invalid" in {
      val fxId = UUID.randomUUID()
      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$fxId",
           |  "transaction_type": "invalid_txnType",
           |  "channel": "bank",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"${SpreadsController.InvalidCreateSpreadErrorMsg}"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
    "respond with BAD_REQUEST when transaction_type is international_remittance and channel is empty string " in {
      val fxId = UUID.randomUUID()
      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$fxId",
           |  "transaction_type": "international_remittance",
           |  "channel": "",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"${SpreadsController.InvalidCreateSpreadErrorMsg}"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
    "respond with BAD_REQUEST when spread > 1 " in {
      val fxId = UUID.randomUUID()
      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$fxId",
           |  "transaction_type": "currency_exchange",
           |  "channel": null,
           |  "institution": null,
           |  "spread": 1.01
           |}""".stripMargin

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"${SpreadsController.InvalidCreateSpreadErrorMsg}"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "respond with BAD_REQUEST when spread < 1 " in {
      val fxId = UUID.randomUUID()

      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$fxId",
           |  "transaction_type": "currency_exchange",
           |  "channel": null,
           |  "institution": null,
           |  "spread": -0.01
           |}""".stripMargin

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"${SpreadsController.InvalidCreateSpreadErrorMsg}"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

  }

}
