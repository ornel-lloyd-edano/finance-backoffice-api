package tech.pegb.backoffice.api.currencyrate.controller

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.currencyrate.controllers.CurrencyRateController
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.dao.currencyexchange.entity.CurrencyExchange
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.currencyexchange.dto.CurrencyExchangeCriteria
import tech.pegb.backoffice.domain.currencyrate.abstraction.CurrencyRateManagement
import tech.pegb.backoffice.domain.currencyrate.dto.{CurrencyRateToUpdate, CurrencyRate ⇒ RateToUpdate, ExchangeRate ⇒ ExchangeRateToUpdate}
import tech.pegb.backoffice.domain.currencyrate.model.{CurrencyRate, ExchangeRate, Rate}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.dao.domain.currency.Implicit._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class CurrencyRateControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {
  implicit val ec = TestExecutionContext.genericOperations

  val currencyRateManagement = stub[CurrencyRateManagement]
  val latestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[CurrencyRateManagement].to(currencyRateManagement),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val kes = Currency(
    id = 1,
    name = "KES",
    description = "kenya shillings".some,
    isActive = true,
    icon = "icon_one".some,
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = now.some,
    updatedBy = none)
  val usd = Currency(
    id = 2,
    name = "USD",
    description = "US dollar".some,
    isActive = true,
    icon = "icon_one".some,
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = now.some,
    updatedBy = none)
  val eur = Currency(
    id = 3,
    name = "EUR",
    description = "euro".some,
    isActive = true,
    icon = "icon_one".some,
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = now.some,
    updatedBy = none)

  val kesUsd = CurrencyExchange(
    id = 1,
    uuid = UUID.randomUUID().toString,
    currencyId = usd.id,
    currencyCode = usd.name,
    baseCurrencyId = kes.id,
    baseCurrency = kes.name,
    rate = BigDecimal(0.009862),
    providerId = 1,
    provider = "PegB",
    targetCurrencyAccountId = usd.id,
    targetCurrencyAccountUuid = UUID.randomUUID().toString,
    baseCurrencyAccountId = kes.id,
    baseCurrencyAccountUuid = UUID.randomUUID().toString,
    balance = BigDecimal("120000.00"),
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))

  val kesEur = CurrencyExchange(
    id = 2,
    uuid = UUID.randomUUID().toString,
    currencyId = eur.id,
    currencyCode = eur.name,
    baseCurrencyId = kes.id,
    baseCurrency = kes.name,
    rate = BigDecimal(0.008843),
    providerId = 1,
    provider = "PegB",
    targetCurrencyAccountId = eur.id,
    targetCurrencyAccountUuid = UUID.randomUUID().toString,
    baseCurrencyAccountId = kes.id,
    baseCurrencyAccountUuid = UUID.randomUUID().toString,
    balance = BigDecimal("120000.00"),
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))

  val usdKes = CurrencyExchange(
    id = 3,
    uuid = UUID.randomUUID().toString,
    currencyId = kes.id,
    currencyCode = kes.name,
    baseCurrencyId = usd.id,
    baseCurrency = usd.name,
    rate = BigDecimal(101.285000),
    providerId = 1,
    provider = "PegB",
    targetCurrencyAccountId = kes.id,
    targetCurrencyAccountUuid = UUID.randomUUID().toString,
    baseCurrencyAccountId = usd.id,
    baseCurrencyAccountUuid = UUID.randomUUID().toString,
    balance = BigDecimal("150000.00"),
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))

  val usdEur = CurrencyExchange(
    id = 4,
    uuid = UUID.randomUUID().toString,
    currencyId = eur.id,
    currencyCode = eur.name,
    baseCurrencyId = usd.id,
    baseCurrency = usd.name,
    rate = BigDecimal(0.896616),
    providerId = 1,
    provider = "PegB",
    targetCurrencyAccountId = eur.id,
    targetCurrencyAccountUuid = UUID.randomUUID().toString,
    baseCurrencyAccountId = usd.id,
    baseCurrencyAccountUuid = UUID.randomUUID().toString,
    balance = BigDecimal("150000.00"),
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))

  val eurKes = CurrencyExchange(
    id = 5,
    uuid = UUID.randomUUID().toString,
    currencyId = kes.id,
    currencyCode = kes.name,
    baseCurrencyId = eur.id,
    baseCurrency = eur.name,
    rate = BigDecimal(112.969000),
    providerId = 1,
    provider = "PegB",
    targetCurrencyAccountId = kes.id,
    targetCurrencyAccountUuid = UUID.randomUUID().toString,
    baseCurrencyAccountId = eur.id,
    baseCurrencyAccountUuid = UUID.randomUUID().toString,
    balance = BigDecimal("200000.00"),
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))

  val eurUsd = CurrencyExchange(
    id = 6,
    uuid = UUID.randomUUID().toString,
    currencyId = usd.id,
    currencyCode = usd.name,
    baseCurrencyId = eur.id,
    baseCurrency = eur.name,
    rate = BigDecimal(1.115360),
    providerId = 1,
    provider = "PegB",
    targetCurrencyAccountId = usd.id,
    targetCurrencyAccountUuid = UUID.randomUUID().toString,
    baseCurrencyAccountId = eur.id,
    baseCurrencyAccountUuid = UUID.randomUUID().toString,
    balance = BigDecimal("200000.00"),
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))

  val kesRate = CurrencyRate(
    mainCurrency = kes.asDomain,
    rates = Seq(
      Rate(
        code = usd.name,
        description = usd.description,
        buyRate = ExchangeRate(UUID.fromString(usdKes.uuid), usdKes.rate),
        sellRate = ExchangeRate(UUID.fromString(kesUsd.uuid), kesUsd.rate.getReciprocal)),
      Rate(
        code = eur.name,
        description = eur.description,
        buyRate = ExchangeRate(UUID.fromString(eurKes.uuid), eurKes.rate),
        sellRate = ExchangeRate(UUID.fromString(kesEur.uuid), kesEur.rate.getReciprocal))))

  val usdRate = CurrencyRate(
    mainCurrency = usd.asDomain,
    rates = Seq(
      Rate(
        code = kes.name,
        description = kes.description,
        buyRate = ExchangeRate(UUID.fromString(kesUsd.uuid), kesUsd.rate),
        sellRate = ExchangeRate(UUID.fromString(usdKes.uuid), usdKes.rate.getReciprocal)),
      Rate(
        code = eur.name,
        description = eur.description,
        buyRate = ExchangeRate(UUID.fromString(eurUsd.uuid), eurUsd.rate),
        sellRate = ExchangeRate(UUID.fromString(usdEur.uuid), usdEur.rate.getReciprocal))))

  val euroRate =
    CurrencyRate(
      mainCurrency = eur.asDomain,
      rates = Seq(
        Rate(
          code = kes.name,
          description = kes.description,
          buyRate = ExchangeRate(UUID.fromString(kesEur.uuid), kesEur.rate),
          sellRate = ExchangeRate(UUID.fromString(eurKes.uuid), eurKes.rate.getReciprocal)),
        Rate(
          code = usd.name,
          description = usd.description,
          buyRate = ExchangeRate(UUID.fromString(usdEur.uuid), usdEur.rate),
          sellRate = ExchangeRate(UUID.fromString(eurUsd.uuid), eurUsd.rate.getReciprocal))))

  "CurrencyRateController" should {
    "return all currencyRates in GET /currency_rates" in {
      val mockLatestVersion = now.toString
      (latestVersionService.getLatestVersion _).when(CurrencyExchangeCriteria())
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (currencyRateManagement.getCurrencyRateList _).when(None, None)
        .returns(Future.successful(Seq(kesRate, usdRate, euroRate).asRight[ServiceError]))

      val expected =
        s"""{
           |"updated_at":"${now.toZonedDateTimeUTC}",
           |"results":[
           |{"main_currency":
           |{"id":1,
           |"code":"KES",
           |"description":
           |"kenya shillings"},
           |"rates":[
           |{
           |"code":"USD",
           |"description":"US dollar",
           |"buy_rate":{
           |"id":"${usdKes.uuid}",
           |"rate":101.285000
           |},
           |"sell_rate":{
           |"id":"${kesUsd.uuid}",
           |"rate":101.399310
           |}
           |},
           |{
           |"code":"EUR",
           |"description":"euro",
           |"buy_rate":{
           |"id":"${eurKes.uuid}",
           |"rate":112.969000
           |},
           |"sell_rate":{
           |"id":"${kesEur.uuid}",
           |"rate":113.083795
           |}
           |}
           |]},
           |{"main_currency":
           |{"id":2,
           |"code":"USD",
           |"description":"US dollar"},
           |"rates":[
           |{
           |"code":"KES",
           |"description":"kenya shillings",
           |"buy_rate":{
           |"id":"${kesUsd.uuid}",
           |"rate":0.009862
           |},
           |"sell_rate":{
           |"id":"${usdKes.uuid}",
           |"rate":0.009873
           |}
           |},
           |{
           |"code":"EUR",
           |"description":"euro",
           |"buy_rate":
           |{
           |"id":"${eurUsd.uuid}",
           |"rate":1.115360
           |},
           |"sell_rate":{
           |"id":"${usdEur.uuid}",
           |"rate":1.115305
           |}
           |}]},
           |{"main_currency":
           |{"id":3,
           |"code":"EUR",
           |"description":"euro"},
           |"rates":[
           |{
           |"code":"KES",
           |"description":"kenya shillings",
           |"buy_rate":{
           |"id":"${kesEur.uuid}",
           |"rate":0.008843
           |},
           |"sell_rate":{
           |"id":"${eurKes.uuid}",
           |"rate":0.008852
           |}
           |},
           |{
           |"code":"USD",
           |"description":
           |"US dollar",
           |"buy_rate":{
           |"id":"${usdEur.uuid}",
           |"rate":0.896616
           |},"sell_rate":{
           |"id":"${eurUsd.uuid}",
           |"rate":0.896572
           |}
           |}
           |]}]}""".stripMargin.replaceAll(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(GET, s"/currency_rates")).get

      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return empty results currencyRates in HEAD /currency_rates" in {
      val mockLatestVersion = now.toString
      (latestVersionService.getLatestVersion _).when(CurrencyExchangeCriteria())
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (currencyRateManagement.getCurrencyRateList _).when(None, None)
        .returns(Future.successful(Seq(kesRate, usdRate, euroRate).asRight[ServiceError]))

      val expected =
        s"""{
           |"updated_at":"${now.toZonedDateTimeUTC}",
           |"results":[]}""".stripMargin.replaceAll(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(HEAD, s"/currency_rates")).get

      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return all currencyRates in GET /currency_rates?order_by=code&show_empty=true" in {
      val mockLatestVersion = now.toString
      (latestVersionService.getLatestVersion _).when(CurrencyExchangeCriteria())
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (currencyRateManagement.getCurrencyRateList _).when(Ordering("code", Ordering.ASCENDING).some, true.some)
        .returns(Future.successful(Seq(euroRate, kesRate, usdRate).asRight[ServiceError]))

      val expected =
        s"""{
           |"updated_at":"${now.toZonedDateTimeUTC}",
           |"results":[
           |{"main_currency":
           |{"id":3,
           |"code":"EUR",
           |"description":"euro"},
           |"rates":[
           |{
           |"code":"KES",
           |"description":"kenya shillings",
           |"buy_rate":{
           |"id":"${kesEur.uuid}",
           |"rate":0.008843
           |},
           |"sell_rate":{
           |"id":"${eurKes.uuid}",
           |"rate":0.008852
           |}
           |},
           |{
           |"code":"USD",
           |"description":
           |"US dollar",
           |"buy_rate":{
           |"id":"${usdEur.uuid}",
           |"rate":0.896616
           |},"sell_rate":{
           |"id":"${eurUsd.uuid}",
           |"rate":0.896572
           |}
           |}
           |]},
           |{"main_currency":
           |{"id":1,
           |"code":"KES",
           |"description":
           |"kenya shillings"},
           |"rates":[
           |{
           |"code":"USD",
           |"description":"US dollar",
           |"buy_rate":{
           |"id":"${usdKes.uuid}",
           |"rate":101.285000
           |},
           |"sell_rate":{
           |"id":"${kesUsd.uuid}",
           |"rate":101.399310
           |}
           |},
           |{
           |"code":"EUR",
           |"description":"euro",
           |"buy_rate":{
           |"id":"${eurKes.uuid}",
           |"rate":112.969000
           |},
           |"sell_rate":{
           |"id":"${kesEur.uuid}",
           |"rate":113.083795
           |}
           |}
           |]},
           |{"main_currency":
           |{"id":2,
           |"code":"USD",
           |"description":"US dollar"},
           |"rates":[
           |{
           |"code":"KES",
           |"description":"kenya shillings",
           |"buy_rate":{
           |"id":"${kesUsd.uuid}",
           |"rate":0.009862
           |},
           |"sell_rate":{
           |"id":"${usdKes.uuid}",
           |"rate":0.009873
           |}
           |},
           |{
           |"code":"EUR",
           |"description":"euro",
           |"buy_rate":
           |{
           |"id":"${eurUsd.uuid}",
           |"rate":1.115360
           |},
           |"sell_rate":{
           |"id":"${usdEur.uuid}",
           |"rate":1.115305
           |}
           |}]}]}""".stripMargin.replaceAll(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(GET, s"/currency_rates?order_by=code&show_empty=true")).get

      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return currency_rate matching the id of currency" in {
      (currencyRateManagement.getCurrencyRateById _).when(kes.id)
        .returns(Future.successful(kesRate.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/currency_rates/${kes.id}")).get
      val expected =
        s"""
           |{"main_currency":
           |{"id":1,
           |"code":"KES",
           |"description":
           |"kenya shillings"},
           |"rates":[
           |{
           |"code":"USD",
           |"description":"US dollar",
           |"buy_rate":{
           |"id":"${usdKes.uuid}",
           |"rate":101.285000
           |},
           |"sell_rate":{
           |"id":"${kesUsd.uuid}",
           |"rate":101.399310
           |}
           |},
           |{
           |"code":"EUR",
           |"description":"euro",
           |"buy_rate":{
           |"id":"${eurKes.uuid}",
           |"rate":112.969000
           |},
           |"sell_rate":{
           |"id":"${kesEur.uuid}",
           |"rate":113.083795
           |}
           |}
           |]}""".stripMargin.replaceAll(System.lineSeparator(), "")
      contentAsString(resp) mustBe expected
    }

    "get currency_rate list invalid order by" in {
      val resp = route(app, FakeRequest(GET, s"/currency_rates?order_by=deadbeef")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe BAD_REQUEST
      (contentAsJson(resp) \ "msg").get.toString should include("invalid field for order_by found.")
    }

    "respond 404 NotFound in /currency_rates/:id if currency was not found" in {
      (currencyRateManagement.getCurrencyRateById _).when(999)
        .returns(Future.successful(Left(ServiceError.notFoundError("CurrencyId 999 not found", UUID.randomUUID().toOption))))

      val resp = route(app, FakeRequest(GET, s"/currency_rates/999")).get

      status(resp) mustBe NOT_FOUND
      (contentAsJson(resp) \ "msg").get.toString should include("CurrencyId 999 not found")
    }
  }

  "return currency_rate on update of rates " in {
    val kesUUid = UUID.randomUUID()
    val usdUuid = UUID.randomUUID()

    val buyRate = ExchangeRateToUpdate(
      id = kesUUid,
      rate = BigDecimal(50.00).setScale(2, RoundingMode.HALF_DOWN))
    val sellRate = ExchangeRateToUpdate(
      id = usdUuid,
      rate = BigDecimal(100.00).setScale(2, RoundingMode.HALF_DOWN))
    val rates = RateToUpdate(
      code = kes.name,
      description = none,
      buyRate = buyRate,
      sellRate = sellRate)
    val currencyToUpdate = CurrencyRateToUpdate(
      name = "KES",
      description = none,
      rates = Seq(rates),
      updatedAt = mockRequestDate.toLocalDateTimeUTC,
      updatedBy = "pegbuser")
    val mockLatestVersion = now.toString
    (latestVersionService.getLatestVersion _).when(CurrencyExchangeCriteria())
      .returns(Right(mockLatestVersion.toOption).toFuture)

    (currencyRateManagement.updateCurrencyRateList(
      _: Int,
      _: Option[LocalDateTime],
      _: CurrencyRateToUpdate)).when(1, None, currencyToUpdate)
      .returns(Future.successful(Seq(kesRate, usdRate, euroRate).asRight[ServiceError]))

    val jsonPayload =
      s"""
         |{
         |        "main_currency": {
         |          "id": 1,
         |          "code": "KES"
         |        },
         |        "rates": [
         |        {
         |          "code": "KES",
         |          "buy_rate": {
         |            "id": "$kesUUid",
         |            "rate": 50.00
         |          },
         |          "sell_rate": {
         |            "id": "$usdUuid",
         |            "rate": 100.00
         |          }
         |        }
         |        ]
         |      }
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    val resp = route(app, FakeRequest(PUT, s"/currency_rates/${kes.id}", jsonHeaders, jsonPayload)).get
    val expected =
      s"""{
         |"updated_at":"${now.toZonedDateTimeUTC}",
         |"results":[
         |{"main_currency":
         |{"id":1,
         |"code":"KES",
         |"description":
         |"kenya shillings"},
         |"rates":[
         |{
         |"code":"USD",
         |"description":"US dollar",
         |"buy_rate":{
         |"id":"${usdKes.uuid}",
         |"rate":101.285000
         |},
         |"sell_rate":{
         |"id":"${kesUsd.uuid}",
         |"rate":101.399310
         |}
         |},
         |{
         |"code":"EUR",
         |"description":"euro",
         |"buy_rate":{
         |"id":"${eurKes.uuid}",
         |"rate":112.969000
         |},
         |"sell_rate":{
         |"id":"${kesEur.uuid}",
         |"rate":113.083795
         |}
         |}
         |]},
         |{"main_currency":
         |{"id":2,
         |"code":"USD",
         |"description":"US dollar"},
         |"rates":[
         |{
         |"code":"KES",
         |"description":"kenya shillings",
         |"buy_rate":{
         |"id":"${kesUsd.uuid}",
         |"rate":0.009862
         |},
         |"sell_rate":{
         |"id":"${usdKes.uuid}",
         |"rate":0.009873
         |}
         |},
         |{
         |"code":"EUR",
         |"description":"euro",
         |"buy_rate":
         |{
         |"id":"${eurUsd.uuid}",
         |"rate":1.115360
         |},
         |"sell_rate":{
         |"id":"${usdEur.uuid}",
         |"rate":1.115305
         |}
         |}]},
         |{"main_currency":
         |{"id":3,
         |"code":"EUR",
         |"description":"euro"},
         |"rates":[
         |{
         |"code":"KES",
         |"description":"kenya shillings",
         |"buy_rate":{
         |"id":"${kesEur.uuid}",
         |"rate":0.008843
         |},
         |"sell_rate":{
         |"id":"${eurKes.uuid}",
         |"rate":0.008852
         |}
         |},
         |{
         |"code":"USD",
         |"description":
         |"US dollar",
         |"buy_rate":{
         |"id":"${usdEur.uuid}",
         |"rate":0.896616
         |},"sell_rate":{
         |"id":"${eurUsd.uuid}",
         |"rate":0.896572
         |}
         |}
         |]}]}""".stripMargin.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe OK
    contentAsString(resp) mustBe expected
  }

  "return badRequest for malformed json on update of rates " in {
    val kesUUid = UUID.randomUUID()
    val usdUuid = UUID.randomUUID()

    val jsonPayload =
      s"""
         |        "rates": [
         |        {
         |          "code": "KES",
         |          "buy_rate": {
         |            "id": "$kesUUid",
         |            "rate": 50.00
         |          },
         |          "sell_rate": {
         |            "id": "$usdUuid",
         |            "rate": 100.00
         |          }
         |        }
         |        ]
         |      }
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    val headers = jsonHeaders.add(strictDeserializationKey → "true")
    val resp = route(app, FakeRequest(PUT, s"/currency_rates/${kes.id}", headers, jsonPayload)).get
    val expected =
      s"""{"id":"$mockRequestId",
         |"code":"MalformedRequest",
         |"msg":"${CurrencyRateController.MalformedUpdateCurrencyRateErrorMsg}"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe BAD_REQUEST
    contentAsString(resp) mustBe expected
  }
}
