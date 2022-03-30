package tech.pegb.backoffice.api.currencyexchange.controller

import java.time._
import java.time.format.DateTimeFormatter
import java.util.{Currency, UUID}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.currencyexchange.Constants
import tech.pegb.backoffice.api.currencyexchange.dto.SpreadToRead
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.currencyexchange.abstraction.{CurrencyExchangeManagement, SpreadsManagement}
import tech.pegb.backoffice.domain.currencyexchange.dto.{CurrencyExchangeCriteria, SpreadCriteria, SpreadToCreate, SpreadUpdateDto}
import tech.pegb.backoffice.domain.currencyexchange.model.{CurrencyExchange, CurrencyExchangeStatus, Spread}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, Utils, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class CurrencyExchangeControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {
  implicit val ec = TestExecutionContext.genericOperations

  val currencyExchangeManagement = stub[CurrencyExchangeManagement]
  val spreadsManagement = stub[SpreadsManagement]
  val latestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[SpreadsManagement].to(spreadsManagement),
      bind[CurrencyExchangeManagement].to(currencyExchangeManagement),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  private val requestDateFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

  val currencyExchangeId1 = UUID.randomUUID()
  val currencyExchange1 = CurrencyExchange(
    id = currencyExchangeId1,
    currency = Currency.getInstance("USD"),
    baseCurrency = Currency.getInstance("KES"),
    rate = BigDecimal(99.9800),
    provider = "Currency Cloud",
    balance = BigDecimal(17526.5),
    dailyAmount = BigDecimal(300.5).some,
    status = "active",
    lastUpdated = Some(LocalDateTime.now(mockClock)))
  val currencyExchangeId2 = UUID.randomUUID()
  val currencyExchange2 = CurrencyExchange(
    id = currencyExchangeId2,
    currency = Currency.getInstance("EUR"),
    baseCurrency = Currency.getInstance("KES"),
    rate = BigDecimal(112.1020),
    provider = "Currency Cloud",
    balance = BigDecimal(10519.0),
    dailyAmount = None,
    status = "active",
    lastUpdated = Some(LocalDateTime.now(mockClock)))
  val currencyExchangeId3 = UUID.randomUUID()
  val currencyExchange3 = CurrencyExchange(
    id = currencyExchangeId3,
    currency = Currency.getInstance("CNY"),
    baseCurrency = Currency.getInstance("KES"),
    rate = BigDecimal(75.9501),
    provider = "Ebury",
    balance = BigDecimal(0.0),
    dailyAmount = None,
    status = "active",
    lastUpdated = Some(LocalDateTime.now(mockClock)))
  val currencyExchangeId4 = UUID.randomUUID()
  val currencyExchange4 = CurrencyExchange(
    id = currencyExchangeId4,
    currency = Currency.getInstance("CHF"),
    baseCurrency = Currency.getInstance("KES"),
    rate = BigDecimal(152.2014),
    provider = "Ebury",
    balance = BigDecimal(0.0),
    dailyAmount = None,
    status = "inactive",
    lastUpdated = Some(LocalDateTime.now(mockClock)))

  "CurrencyExchangeController getCurrencyExchangeByCriteria" should {

    "return list of currency exchange" in {
      val criteria = CurrencyExchangeCriteria(
        id = None,
        currencyCode = None,
        baseCurrency = None,
        provider = None,
        status = None,
        partialMatchFields = Constants.validCurrencyExchangesPartialMatchFields.filterNot(_ == "disabled"))

      val ordering = Seq(Ordering("currency_code", Ordering.ASCENDING))

      val expected =
        s"""{
           |"total":4,
           |"results":[
           |{
           |"id":"$currencyExchangeId4",
           |"sell_currency":"KES",
           |"buy_currency":"CHF",
           |"currency_description":"Swiss Franc",
           |"rate":152.2014,
           |"provider":"Ebury",
           |"balance":0.00,
           |"status":"inactive",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |},
           |{
           |"id":"$currencyExchangeId3",
           |"sell_currency":"KES",
           |"buy_currency":"CNY",
           |"currency_description":"Chinese Yuan",
           |"rate":75.9501,
           |"provider":"Ebury",
           |"balance":0.00,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |},
           |{
           |"id":"$currencyExchangeId2",
           |"sell_currency":"KES",
           |"buy_currency":"EUR",
           |"currency_description":"Euro",
           |"rate":112.102,
           |"provider":"Currency Cloud",
           |"balance":10519.00,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |},
           |{
           |"id":"$currencyExchangeId1",
           |"sell_currency":"KES",
           |"buy_currency":"USD",
           |"currency_description":"US Dollar",
           |"rate":99.98,
           |"provider":"Currency Cloud",
           |"balance":17526.50,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (currencyExchangeManagement.countCurrencyExchangeByCriteria _).when(criteria)
        .returns(Future.successful(Right(4)))
      (currencyExchangeManagement.getCurrencyExchangeByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(currencyExchange4, currencyExchange3, currencyExchange2, currencyExchange1))))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges?order_by=currency_code")).get

      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return empty list of currency exchange if HEAD method was used" in {
      val criteria = CurrencyExchangeCriteria(
        id = None,
        currencyCode = None,
        baseCurrency = None,
        provider = None,
        status = None,
        partialMatchFields = Constants.validCurrencyExchangesPartialMatchFields.filterNot(_ == "disabled"))

      val ordering = Seq(Ordering("currency_code", Ordering.ASCENDING))

      val expected =
        s"""{
           |"total":0,
           |"results":[],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (currencyExchangeManagement.countCurrencyExchangeByCriteria _).when(criteria)
        .returns(Future.successful(Right(4)))
      (currencyExchangeManagement.getCurrencyExchangeByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(currencyExchange4, currencyExchange3, currencyExchange2, currencyExchange1))))

      val resp = route(app, FakeRequest(HEAD, s"/currency_exchanges?order_by=currency_code")).get

      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return list of currency exchange satisfying the filter" in {
      val criteria = CurrencyExchangeCriteria(
        id = None,
        currencyCode = None,
        baseCurrency = None,
        provider = None,
        status = Some(CurrencyExchangeStatus.Active),
        partialMatchFields = Constants.validCurrencyExchangesPartialMatchFields.filterNot(_ == "disabled"))

      val ordering = Seq(Ordering("currency_code", Ordering.ASCENDING))

      val expected =
        s"""{
           |"total":3,
           |"results":[
           |{
           |"id":"$currencyExchangeId3",
           |"sell_currency":"KES",
           |"buy_currency":"CNY",
           |"currency_description":"Chinese Yuan",
           |"rate":75.9501,
           |"provider":"Ebury",
           |"balance":0.00,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |},
           |{
           |"id":"$currencyExchangeId2",
           |"sell_currency":"KES",
           |"buy_currency":"EUR",
           |"currency_description":"Euro",
           |"rate":112.102,
           |"provider":"Currency Cloud",
           |"balance":10519.00,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |},
           |{
           |"id":"$currencyExchangeId1",
           |"sell_currency":"KES",
           |"buy_currency":"USD",
           |"currency_description":"US Dollar",
           |"rate":99.98,
           |"provider":"Currency Cloud",
           |"balance":17526.50,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (currencyExchangeManagement.countCurrencyExchangeByCriteria _).when(criteria)
        .returns(Future.successful(Right(3)))
      (currencyExchangeManagement.getCurrencyExchangeByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(currencyExchange3, currencyExchange2, currencyExchange1))))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges?order_by=currency_code&status=active")).get

      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return list of currency exchange satisfying the filter with partialMatch" in {
      val criteria = CurrencyExchangeCriteria(
        id = None,
        currencyCode = None,
        baseCurrency = None,
        provider = Some("Cloud"),
        status = Some(CurrencyExchangeStatus.Active),
        partialMatchFields = Set("provider"))

      val ordering = Seq(Ordering("currency_code", Ordering.ASCENDING))

      val expected =
        s"""{
           |"total":2,
           |"results":[
           |{
           |"id":"$currencyExchangeId2",
           |"sell_currency":"KES",
           |"buy_currency":"EUR",
           |"currency_description":"Euro",
           |"rate":112.102,
           |"provider":"Currency Cloud",
           |"balance":10519.00,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |},
           |{
           |"id":"$currencyExchangeId1",
           |"sell_currency":"KES",
           |"buy_currency":"USD",
           |"currency_description":"US Dollar",
           |"rate":99.98,
           |"provider":"Currency Cloud",
           |"balance":17526.50,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (currencyExchangeManagement.countCurrencyExchangeByCriteria _).when(criteria)
        .returns(Future.successful(Right(2)))
      (currencyExchangeManagement.getCurrencyExchangeByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(currencyExchange2, currencyExchange1))))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges?order_by=currency_code&status=active&partial_match=provider&provider=Cloud")).get

      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return list of currency exchange satisfying the filter with order_by mapping to db columns buy_cost,rate,provider" in {
      val criteria = CurrencyExchangeCriteria(
        id = None,
        currencyCode = None,
        baseCurrency = None,
        provider = Some("Cloud"),
        status = Some(CurrencyExchangeStatus.Active),
        partialMatchFields = Set("provider"))

      val ordering = Seq(Ordering("rate", Ordering.DESCENDING), Ordering("provider_name", Ordering.ASCENDING))

      val expected =
        s"""{
           |"total":2,
           |"results":[
           |{
           |"id":"$currencyExchangeId2",
           |"sell_currency":"KES",
           |"buy_currency":"EUR",
           |"currency_description":"Euro",
           |"rate":112.102,
           |"provider":"Currency Cloud",
           |"balance":10519.00,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |},
           |{
           |"id":"$currencyExchangeId1",
           |"sell_currency":"KES",
           |"buy_currency":"USD",
           |"currency_description":"US Dollar",
           |"rate":99.98,
           |"provider":"Currency Cloud",
           |"balance":17526.50,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (currencyExchangeManagement.countCurrencyExchangeByCriteria _).when(criteria)
        .returns(Future.successful(Right(2)))
      (currencyExchangeManagement.getCurrencyExchangeByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(currencyExchange2, currencyExchange1))))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges?order_by=-rate,provider&status=active&partial_match=provider&provider=Cloud")).get

      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }
  }

  "CurrencyExchangeController getCurrencyExchange" should {

    "return currency exchange that matches uuid provided in param" in {
      val expected =
        s"""{
           |"id":"$currencyExchangeId1",
           |"sell_currency":"KES",
           |"buy_currency":"USD",
           |"currency_description":"US Dollar",
           |"rate":99.98,
           |"provider":"Currency Cloud",
           |"balance":17526.50,
           |"status":"active",
           |"updated_at":"${LocalDateTime.now(mockClock).toZonedDateTimeUTC}"
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(currencyExchangeId1, *)
        .returns(Future.successful(Right(currencyExchange1)))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges/$currencyExchangeId1")).get

      contentAsString(resp) mustBe expected

    }

    "respond 404 NotFound in /currency_exchanges/:id/spreads if currency_exchanges uuid was not found" in {
      val requestId = UUID.randomUUID()
      val fakeUUID = UUID.randomUUID()
      val refErrorId = UUID.randomUUID()
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(fakeUUID, *)
        .returns(Future.successful(Left(ServiceError.notFoundError("Currency Exchange not Found", refErrorId.toOption))))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges/$fakeUUID")
        .withHeaders(Headers("request-id" → requestId.toString))).get

      val expected =
        s"""{"id":"$requestId",
           |"code":"NotFound",
           |"msg":"Currency Exchange not Found",
           |"tracking_id":"$refErrorId"
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
      (contentAsJson(resp) \ "code").get.toString() mustBe "\"NotFound\""
    }
  }

  "CurrencyExchangeController getCurrencyExchangeSpreads" should {

    "respond 200 OK with payload PaginatedResult[SpreadToRead] json all linked to given currency exchange id in the path in /currency_exchanges/:id/spreads" in {
      val mockCurrencyExchangeId = UUID.randomUUID()

      val mockCurrencyExchange = CurrencyExchange.empty
        .copy(id = mockCurrencyExchangeId, status = "active", currency = java.util.Currency.getInstance("USD"), baseCurrency = java.util.Currency.getInstance("KES"), provider = "Currency Cloud")

      val expectedSpreads = Seq(
        Spread.empty.copy(currencyExchange = mockCurrencyExchange),
        Spread.empty.copy(currencyExchange = mockCurrencyExchange),
        Spread.empty.copy(currencyExchange = mockCurrencyExchange))

      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(CurrencyExchange.empty.copy(id = mockCurrencyExchangeId))))

      val noCriteria = SpreadCriteria(
        currencyExchangeId = Option(UUIDLike(mockCurrencyExchangeId.toString)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      (spreadsManagement.countSpreadByCriteria _).when(noCriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(noCriteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges/$mockCurrencyExchangeId/spreads")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
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
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
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
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
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

    "respond 200 OK with payload PaginatedResult[SpreadToRead] json total=0 and results=[] if HEAD method was used" in {
      val mockCurrencyExchangeId = UUID.randomUUID()

      val mockCurrencyExchange = CurrencyExchange.empty
        .copy(id = mockCurrencyExchangeId, status = "active", currency = java.util.Currency.getInstance("USD"), baseCurrency = java.util.Currency.getInstance("KES"), provider = "Currency Cloud")

      val expectedSpreads = Seq(
        Spread.empty.copy(currencyExchange = mockCurrencyExchange),
        Spread.empty.copy(currencyExchange = mockCurrencyExchange),
        Spread.empty.copy(currencyExchange = mockCurrencyExchange))

      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(CurrencyExchange.empty.copy(id = mockCurrencyExchangeId))))

      val noCriteria = SpreadCriteria(
        currencyExchangeId = Option(UUIDLike(mockCurrencyExchangeId.toString)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      //Note: even if you remove the mocking being done below, it will not result to NPE because the lazy argument evaluation in executeIfGETMethod
      (spreadsManagement.countSpreadByCriteria _).when(noCriteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(noCriteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val resp = route(app, FakeRequest(HEAD, s"/currency_exchanges/$mockCurrencyExchangeId/spreads")).get

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

    "respond 404 NotFound in /currency_exchanges/:id/spreads if currency_exchanges uuid was not found" in {
      val mockCurrencyExchangeId = UUID.randomUUID()

      val expectedError = ServiceError.notFoundError(s"Currency exchange id [$mockCurrencyExchangeId] was not found.", UUID.randomUUID().toOption)
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Left(expectedError)))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges/$mockCurrencyExchangeId/spreads")
        .withHeaders(requestIdHeaderKey → mockRequestId.toString)).get

      val expectedJson =
        s"""{"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"Currency exchange id [$mockCurrencyExchangeId] was not found.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe NOT_FOUND
      headers(resp).get("content-type") mustBe Some("application/json")
      contentAsString(resp) mustBe expectedJson
    }

    "respond with PaginatedResult[SpreadToRead] json matching given transaction_type in /currency_exchanges/:id/spreads?transaction_type=international_remittance" in {
      val mockCurrencyExchangeId = UUID.randomUUID()

      val mockCurrencyExchange = CurrencyExchange.empty
        .copy(id = mockCurrencyExchangeId, status = "active", currency = java.util.Currency.getInstance("USD"), baseCurrency = java.util.Currency.getInstance("KES"), provider = "Currency Cloud")

      val expectedSpreads = Seq(
        Spread.empty.copy(currencyExchange = mockCurrencyExchange, channel = Option(Channel("mobile_money")), transactionType = TransactionType("international_remittance")),
        Spread.empty.copy(currencyExchange = mockCurrencyExchange, channel = Option(Channel("mobile_money")), transactionType = TransactionType("international_remittance")),
        Spread.empty.copy(currencyExchange = mockCurrencyExchange, channel = Option(Channel("mobile_money")), transactionType = TransactionType("international_remittance")))

      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(CurrencyExchange.empty.copy(id = mockCurrencyExchangeId))))

      val criteria = SpreadCriteria(
        transactionType = Option(TransactionType("international_remittance")),
        currencyExchangeId = Option(UUIDLike(mockCurrencyExchangeId.toString)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      (spreadsManagement.countSpreadByCriteria _).when(criteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(criteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges/$mockCurrencyExchangeId/spreads?transaction_type=international_remittance")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(0).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"mobile_money",
           |"institution":null,
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(1).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"mobile_money",
           |"institution":null,
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
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

    "respond with PaginatedResult[SpreadToRead] json matching given channel in /currency_exchanges/:id/spreads?channel=mobile_money" in {
      val mockCurrencyExchangeId = UUID.randomUUID()

      val mockCurrencyExchange = CurrencyExchange.empty
        .copy(id = mockCurrencyExchangeId, status = "active", currency = java.util.Currency.getInstance("USD"), baseCurrency = java.util.Currency.getInstance("KES"), provider = "Currency Cloud")

      val expectedSpreads = Seq(
        Spread.empty.copy(currencyExchange = mockCurrencyExchange, transactionType = TransactionType("international_remittance"), channel = Option(Channel("mobile_money"))),
        Spread.empty.copy(currencyExchange = mockCurrencyExchange, transactionType = TransactionType("international_remittance"), channel = Option(Channel("mobile_money"))),
        Spread.empty.copy(currencyExchange = mockCurrencyExchange, transactionType = TransactionType("international_remittance"), channel = Option(Channel("mobile_money"))))

      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(CurrencyExchange.empty.copy(id = mockCurrencyExchangeId))))

      val criteria = SpreadCriteria(
        transactionType = Option(TransactionType("international_remittance")),
        currencyExchangeId = Option(UUIDLike(mockCurrencyExchangeId.toString)))
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      (spreadsManagement.countSpreadByCriteria _).when(criteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(criteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges/$mockCurrencyExchangeId/spreads?transaction_type=international_remittance")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
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
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(1).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(1).transactionType}",
           |"channel":"mobile_money",
           |"institution":null,
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(2).transactionType}",
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

    "respond with PaginatedResult[SpreadToRead] json matching given institution in /currency_exchanges/:id/spreads?institution=mpesa" in {
      val mockCurrencyExchangeId = UUID.randomUUID()

      val mockCurrencyExchange = CurrencyExchange.empty
        .copy(id = mockCurrencyExchangeId, status = "active", currency = java.util.Currency.getInstance("USD"), baseCurrency = java.util.Currency.getInstance("KES"), provider = "Currency Cloud")

      val expectedSpreads = Seq(
        Spread.empty.copy(currencyExchange = mockCurrencyExchange, channel = Option(Channel("mobile_money")), transactionType = TransactionType("international_remittance"), recipientInstitution = Option("mpesa")),
        Spread.empty.copy(currencyExchange = mockCurrencyExchange, channel = Option(Channel("mobile_money")), transactionType = TransactionType("international_remittance"), recipientInstitution = Option("mpesa")),
        Spread.empty.copy(currencyExchange = mockCurrencyExchange, channel = Option(Channel("mobile_money")), transactionType = TransactionType("international_remittance"), recipientInstitution = Option("mpesa")))

      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(CurrencyExchange.empty.copy(id = mockCurrencyExchangeId))))

      val criteria = SpreadCriteria(
        recipientInstitution = Option("mpesa"),
        currencyExchangeId = Option(UUIDLike(mockCurrencyExchangeId.toString)))
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      (spreadsManagement.countSpreadByCriteria _).when(criteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(criteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges/$mockCurrencyExchangeId/spreads?institution=mpesa")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(0).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(0).transactionType}",
           |"channel":"mobile_money",
           |"institution":"mpesa",
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(1).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(1).transactionType}",
           |"channel":"mobile_money",
           |"institution":"mpesa",
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"${expectedSpreads(2).transactionType}",
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

    "respond with PaginatedResult[SpreadToRead] json matching multiple criteria fields in /currency_exchanges/:id/spreads?channel=mobile_money&transaction_type=international_remittance&institution=mpesa" in {
      val mockCurrencyExchangeId = UUID.randomUUID()

      val mockCurrencyExchange = CurrencyExchange.empty
        .copy(id = mockCurrencyExchangeId, status = "active", currency = java.util.Currency.getInstance("USD"), baseCurrency = java.util.Currency.getInstance("KES"), provider = "Currency Cloud")

      val expectedSpreads = Seq(
        Spread.empty.copy(
          currencyExchange = mockCurrencyExchange,
          transactionType = TransactionType("international_remittance"),
          channel = Option(Channel("mobile_money")), recipientInstitution = Option("mpesa")),
        Spread.empty.copy(
          currencyExchange = mockCurrencyExchange,
          transactionType = TransactionType("international_remittance"),
          channel = Option(Channel("mobile_money")), recipientInstitution = Option("mpesa")),
        Spread.empty.copy(
          currencyExchange = mockCurrencyExchange,
          transactionType = TransactionType("international_remittance"),
          channel = Option(Channel("mobile_money")), recipientInstitution = Option("mpesa")))

      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(CurrencyExchange.empty.copy(id = mockCurrencyExchangeId))))

      val criteria = SpreadCriteria(
        recipientInstitution = Option("mpesa"),
        channel = Option(Channel("mobile_money")),
        transactionType = Option(TransactionType("international_remittance")),
        currencyExchangeId = Option(UUIDLike(mockCurrencyExchangeId.toString)))
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      (spreadsManagement.countSpreadByCriteria _).when(criteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(criteria, Seq(), None, None)
        .returns(Future.successful(Right(expectedSpreads)))

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges/$mockCurrencyExchangeId/spreads?channel=mobile_money&transaction_type=international_remittance&institution=mpesa")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(0).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(0).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(0).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"mobile_money",
           |"institution":"mpesa",
           |"spread":${expectedSpreads(0).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(1).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(1).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(1).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"mobile_money",
           |"institution":"mpesa",
           |"spread":${expectedSpreads(1).spread},
           |"updated_by":null,
           |"updated_at":null
           |},
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
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

    "respond with PaginatedResult[SpreadToRead] json limited by limit and offset params in /currency_exchanges/:id/spreads?limit=1&offset=2" in {
      val mockCurrencyExchangeId = UUID.randomUUID()

      val mockCurrencyExchange = CurrencyExchange.empty
        .copy(id = mockCurrencyExchangeId, status = "active", currency = java.util.Currency.getInstance("USD"), baseCurrency = java.util.Currency.getInstance("KES"), provider = "Currency Cloud")

      val expectedSpreads = Seq(
        Spread.empty.copy(
          currencyExchange = mockCurrencyExchange,
          transactionType = TransactionType("international_remittance"),
          channel = Option(Channel("mobile_money")), recipientInstitution = Option("mpesa")),
        Spread.empty.copy(
          currencyExchange = mockCurrencyExchange,
          transactionType = TransactionType("international_remittance"),
          channel = Option(Channel("mobile_money")), recipientInstitution = Option("mpesa")),
        Spread.empty.copy(
          currencyExchange = mockCurrencyExchange,
          transactionType = TransactionType("international_remittance"),
          channel = Option(Channel("mobile_money")), recipientInstitution = Option("mpesa")))

      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(CurrencyExchange.empty.copy(id = mockCurrencyExchangeId))))

      val criteria = SpreadCriteria(currencyExchangeId = Option(UUIDLike(mockCurrencyExchangeId.toString)))
      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      (spreadsManagement.countSpreadByCriteria _).when(criteria)
        .returns(Future.successful(Right(expectedSpreads.size)))

      (spreadsManagement.getSpreadByCriteria _).when(criteria, Seq(), Some(1), Some(2))
        .returns(Future.successful(Right(expectedSpreads.tail.tail))) //tail.tail simulates offset = 2

      val resp = route(app, FakeRequest(GET, s"/currency_exchanges/$mockCurrencyExchangeId/spreads?limit=1&offset=2")).get

      val expectedJson =
        s"""{"total":3,
           |"results":[
           |{"id":"${expectedSpreads(2).id}",
           |"currency_exchange_id":"${mockCurrencyExchangeId}",
           |"buy_currency":"${expectedSpreads(2).currencyExchange.currency.getCurrencyCode}",
           |"sell_currency":"${expectedSpreads(2).currencyExchange.baseCurrency.getCurrencyCode}",
           |"transaction_type":"international_remittance",
           |"channel":"mobile_money",
           |"institution":"mpesa",
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
  }

  "CurrencyExchangeController activateFX/deactivateFX" should {

    val validFxId = UUID.randomUUID()
    val invalidFxId = UUID.randomUUID()
    val validFx = CurrencyExchange(
      id = validFxId,
      currency = Currency.getInstance("KES"),
      baseCurrency = Currency.getInstance("AED"),
      rate = BigDecimal(1.75),
      provider = "Good friend of mine",
      balance = BigDecimal(300),
      dailyAmount = None,
      status = "inactive",
      lastUpdated = Some(LocalDateTime.now()))

    "respond with 200 OK when activate currency exchange in PUT /currency_exchanges/:id/activate" in {
      (currencyExchangeManagement.activateFX(_: UUID, _: LocalDateTime, _: String, _: Option[LocalDateTime])(_: UUID))
        .when(validFxId, mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, None, mockRequestId)
        .returns(Future.successful(Right(validFx.copy(status = "active"))))

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val req = FakeRequest(PUT, s"/currency_exchanges/$validFxId/activate", jsonHeaders, jsonRequest)
      val resp = route(app, req).get
      status(resp) mustBe OK
      contentAsString(resp) must include(""""status":"active"""")
    }

    "respond with 400 when activate non-existing currency exchange in PUT /currency_exchanges/:id/activate" in {
      (currencyExchangeManagement.activateFX(_: UUID, _: LocalDateTime, _: String, _: Option[LocalDateTime])(_: UUID))
        .when(invalidFxId, mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, None, *)
        .returns(Future.successful(Left(ServiceError.validationError(s"Currency exchange $invalidFxId was not found", mockRequestId.toOption))))

      val req = FakeRequest(PUT, s"/currency_exchanges/$invalidFxId/activate").withHeaders(jsonHeaders)
      val resp = route(app, req).get
      status(resp) mustBe BAD_REQUEST
    }

    "respond with 200 OK when deactivated currency exchange in PUT /currency_exchanges/:id/deactivate" in {
      (currencyExchangeManagement.deactivateFX(_: UUID, _: LocalDateTime, _: String, _: Option[LocalDateTime])(_: UUID))
        .when(validFxId, mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, None, *)
        .returns(Future.successful(Right(validFx.copy(status = "inactive"))))

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val req = FakeRequest(PUT, s"/currency_exchanges/$validFxId/deactivate", jsonHeaders, jsonRequest)
      val resp = route(app, req).get
      status(resp) mustBe OK
      contentAsString(resp) must include(""""status":"inactive"""")
    }

    "respond with 400 when deactivate non-existing currency exchange in PUT /currency_exchanges/:id/deactivate" in {
      (currencyExchangeManagement.deactivateFX(_: UUID, _: LocalDateTime, _: String, _: Option[LocalDateTime])(_: UUID))
        .when(invalidFxId, mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, None, *)
        .returns(Future.successful(Left(ServiceError.validationError(s"Currency exchange $invalidFxId was not found", mockRequestId.toOption))))

      val req = FakeRequest(PUT, s"/currency_exchanges/$invalidFxId/deactivate").withHeaders(jsonHeaders)
      val resp = route(app, req).get
      status(resp) mustBe BAD_REQUEST
    }

    "respond with 204 NoContent on batch activate currency exchange when CORE returns SUCCESS in PUT /currency_exchanges/activate " in {
      (currencyExchangeManagement.batchActivateFX(_: LocalDateTime, _: String)(_: UUID))
        .when(mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, mockRequestId)
        .returns(Future.successful(Right(())))

      val req = FakeRequest(PUT, s"/currency_exchanges/activate").withHeaders(jsonHeaders)
      val resp = route(app, req).get
      status(resp) mustBe NO_CONTENT
    }

    "respond with 500 on batch activate currency exchange when CORE returns ERROR in PUT /currency_exchanges/activate" in {
      (currencyExchangeManagement.batchActivateFX(_: LocalDateTime, _: String)(_: UUID))
        .when(mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, mockRequestId)
        .returns(Future.successful(Left(ServiceError.unknownError("Error encountered in core", mockRequestId.toOption))))

      val req = FakeRequest(PUT, s"/currency_exchanges/activate").withHeaders(jsonHeaders)
      val resp = route(app, req).get
      status(resp) mustBe INTERNAL_SERVER_ERROR
    }

    "respond with 204 NoContent on batch deactivate currency exchange when CORE returns SUCCESS in PUT /currency_exchanges/activate " in {
      (currencyExchangeManagement.batchDeactivateFX(_: LocalDateTime, _: String)(_: UUID))
        .when(mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, mockRequestId)
        .returns(Future.successful(Right(())))

      val req = FakeRequest(PUT, s"/currency_exchanges/deactivate").withHeaders(jsonHeaders)
      val resp = route(app, req).get
      status(resp) mustBe NO_CONTENT
    }

    "respond with 500 on batch deactivate currency exchange when CORE returns ERROR in PUT /currency_exchanges/activate" in {
      (currencyExchangeManagement.batchDeactivateFX(_: LocalDateTime, _: String)(_: UUID))
        .when(mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, mockRequestId)
        .returns(Future.successful(Left(ServiceError.unknownError("Error encountered in core", mockRequestId.toOption))))

      val req = FakeRequest(PUT, s"/currency_exchanges/deactivate").withHeaders(jsonHeaders)
      val resp = route(app, req).get
      status(resp) mustBe INTERNAL_SERVER_ERROR
    }

  }

  "CurrencyExchangeController createSpreads" should {
    "respond with CREATED when spread is successfully created" in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "international_remittance",
           |  "channel": "bank",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val fxId = UUID.randomUUID()

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

      val fakeRequest = FakeRequest(POST, s"/currency_exchanges/$fxId/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }
    "respond with BAD_REQUEST when transaction_type is invalid" in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "invalid_txnType",
           |  "channel": "bank",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val fxId = UUID.randomUUID()

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/currency_exchanges/$fxId/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
    "respond with BAD_REQUEST when transaction_type is international_remittance and channel is empty string " in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "international_remittance",
           |  "channel": "",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val fxId = UUID.randomUUID()

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/currency_exchanges/$fxId/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
    "respond with BAD_REQUEST when spread > 1 " in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "currency_exchange",
           |  "channel": null,
           |  "institution": null,
           |  "spread": 1.01
           |}""".stripMargin

      val fxId = UUID.randomUUID()

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/currency_exchanges/$fxId/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
    "respond with BAD_REQUEST when spread < 1 " in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "currency_exchange",
           |  "channel": null,
           |  "institution": null,
           |  "spread": -0.01
           |}""".stripMargin

      val fxId = UUID.randomUUID()

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/currency_exchanges/$fxId/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "respond with 200 OK when updating spread in PUT /currency_exchanges/:fxId/spreads/:id" in {
      implicit val mockRequestId: UUID = UUID.randomUUID()
      val spreadId = UUID.randomUUID()
      val fxId = UUID.randomUUID()
      val spread = BigDecimal("0.49")
      val uBy = getClass.getSimpleName
      val uAt = Utils.now().withNano(0).format(requestDateFormatter)
      val expectedSpread = Spread.empty.copy(
        id = spreadId,
        spread = spread,
        currencyExchange = CurrencyExchange.empty.copy(id = fxId, currency = Currency.getInstance("KES")))
      val dto = SpreadUpdateDto(
        id = spreadId,
        currencyExchangeId = fxId,
        spread = spread,
        updatedBy = uBy,
        updatedAt = ZonedDateTime.parse(uAt, requestDateFormatter).toLocalDateTime,
        lastUpdatedAt = None)

      (currencyExchangeManagement.updateSpread(_: SpreadUpdateDto)(_: UUID))
        .when(dto, mockRequestId)
        .returns(Future.successful(Right(expectedSpread)))

      val payload = Json.parse(
        s"""{"spread":${spread.doubleValue()}}""")
      val resp = route(app, FakeRequest(PUT, s"/currency_exchanges/$fxId/spreads/$spreadId")
        .withHeaders(
          requestDateHeaderKey → uAt,
          requestFromHeaderKey → uBy,
          requestIdHeaderKey → mockRequestId.toString)
        .withJsonBody(payload)).get

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      val updatedSpread = contentAsString(resp).as[SpreadToRead](classOf[SpreadToRead], isStrict = true).toEither
      updatedSpread.map(_.spread) mustBe Right(spread)
    }

    "respond with 200 OK when removing spread in DELETE /currency_exchanges/:fxId/spreads/:id" in {
      implicit val mockRequestId: UUID = UUID.randomUUID()
      val fakeLastUpdateAt = ZonedDateTime.now()
      val jsonRequest =
        s"""{
           |"updated_at": "$fakeLastUpdateAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val spreadId = UUID.randomUUID()
      val fxId = UUID.randomUUID()
      val spread = BigDecimal("0.49")
      val uBy = getClass.getSimpleName
      val uAt = Utils.now()
      val expectedSpread = Spread.empty.copy(
        id = spreadId,
        spread = spread,
        currencyExchange = CurrencyExchange.empty.copy(id = fxId, currency = Currency.getInstance("KES")))

      (currencyExchangeManagement.deleteSpread(_: UUID, _: UUID, _: ZonedDateTime, _: String, _: Option[LocalDateTime])(_: UUID))
        .when(spreadId, fxId, uAt, uBy, Some(fakeLastUpdateAt.toLocalDateTimeUTC), mockRequestId)
        .returns(Future.successful(Right(expectedSpread)))

      val resp = route(app, FakeRequest(DELETE, s"/currency_exchanges/$fxId/spreads/$spreadId", jsonHeaders, jsonRequest.toString)
        .withHeaders(
          requestDateHeaderKey → uAt.format(requestDateFormatter),
          requestFromHeaderKey → uBy,
          requestIdHeaderKey → mockRequestId.toString)).get

      status(resp) mustBe OK
      headers(resp).get("content-type") mustBe Some("application/json")
      val updatedSpread = contentAsString(resp).as[SpreadToRead](classOf[SpreadToRead], isStrict = true).toEither
      updatedSpread.map(_.spread) mustBe Right(spread)
    }

    "update spread fail (precondition fail)" in {

      implicit val mockRequestId: UUID = UUID.randomUUID()
      val fakeLastUpdateAt = LocalDateTime.now()

      val spreadId = UUID.randomUUID()
      val fxId = UUID.randomUUID()
      val spread = BigDecimal("0.49")
      val uBy = getClass.getSimpleName
      val uAt = Utils.now().withNano(0).format(requestDateFormatter)

      val dto = SpreadUpdateDto(
        id = spreadId,
        currencyExchangeId = fxId,
        spread = spread,
        updatedBy = uBy,
        updatedAt = ZonedDateTime.parse(uAt, requestDateFormatter).toLocalDateTime,
        lastUpdatedAt = Some(fakeLastUpdateAt))

      val error = ServiceError.staleResourceAccessError(s"Update failed. Spread ${spreadId} has been modified by another process.", mockRequestId.toOption)

      (currencyExchangeManagement.updateSpread(_: SpreadUpdateDto)(_: UUID))
        .when(dto, mockRequestId)
        .returns(Future.successful(Left(error)))

      val payload = Json.parse(
        s"""{"spread":${spread.doubleValue()},"updated_at":"${fakeLastUpdateAt}"}""")
      val resp = route(app, FakeRequest(PUT, s"/currency_exchanges/$fxId/spreads/$spreadId")
        .withHeaders(
          requestDateHeaderKey → uAt,
          requestFromHeaderKey → uBy,
          requestIdHeaderKey → mockRequestId.toString)
        .withJsonBody(payload)).get

      val errorJson =
        s"""{"id":"$mockRequestId",
           |"code":"PreconditionFailed",
           |"msg":"Update failed. Spread $spreadId has been modified by another process.",
           |"tracking_id":"${error.id}"}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe PRECONDITION_FAILED
      contentAsString(resp) mustBe errorJson
    }

    "delete spreads (precondition fail)" in {
      val fakeLastUpdateAt = ZonedDateTime.now()
      val jsonRequest =
        s"""{
           |"updated_at": "$fakeLastUpdateAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val spreadId = UUID.randomUUID()
      val fxId = UUID.randomUUID()
      val spread = BigDecimal("0.49")
      val uBy = getClass.getSimpleName
      val uAt = Utils.now()

      val error = ServiceError.staleResourceAccessError(s"Update failed. Spread ${spreadId} has been modified by another process.", mockRequestId.toOption)

      (currencyExchangeManagement.deleteSpread(_: UUID, _: UUID, _: ZonedDateTime, _: String, _: Option[LocalDateTime])(_: UUID))
        .when(spreadId, fxId, uAt, uBy, Some(fakeLastUpdateAt.toLocalDateTimeUTC), mockRequestId)
        .returns(Future.successful(Left(error)))

      val resp = route(app, FakeRequest(DELETE, s"/currency_exchanges/$fxId/spreads/$spreadId", jsonHeaders, jsonRequest.toString)
        .withHeaders(
          requestDateHeaderKey → uAt.format(requestDateFormatter),
          requestFromHeaderKey → uBy,
          requestIdHeaderKey → mockRequestId.toString)).get

      val errorJson =
        s"""{"id":"$mockRequestId",
           |"code":"PreconditionFailed",
           |"msg":"Update failed. Spread $spreadId has been modified by another process.",
           |"tracking_id":"${error.id}"}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe PRECONDITION_FAILED
      contentAsString(resp) mustBe errorJson
    }
  }

}
