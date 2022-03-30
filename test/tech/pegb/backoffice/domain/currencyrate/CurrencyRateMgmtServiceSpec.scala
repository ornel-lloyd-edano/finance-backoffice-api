package tech.pegb.backoffice.domain.currencyrate

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.test.Injecting
import tech.pegb.backoffice.core.integration.abstraction.CurrencyExchangeCoreApiClient
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.dao.currencyexchange.abstraction.CurrencyExchangeDao
import tech.pegb.backoffice.dao.currencyexchange.dto.CurrencyExchangeCriteria
import tech.pegb.backoffice.dao.currencyexchange.entity.CurrencyExchange
import tech.pegb.backoffice.dao.model.Ordering
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.currencyrate.dto.{CurrencyRateToUpdate, CurrencyRate ⇒ RateToUpdate, ExchangeRate ⇒ ExchangeRateToUpdate}
import tech.pegb.backoffice.domain.currencyrate.implementation.CurrencyRateMgmtService
import tech.pegb.backoffice.domain.currencyrate.model.{CurrencyRate, ExchangeRate, Rate}
import tech.pegb.backoffice.domain.model.{Ordering ⇒ DomainOrdering}
import tech.pegb.backoffice.mapping.dao.domain.currency.Implicit._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class CurrencyRateMgmtServiceSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  val currencyExchangeDao = stub[CurrencyExchangeDao]
  val currencyDao = stub[CurrencyDao]
  val fxApi = stub[CurrencyExchangeCoreApiClient]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[CurrencyExchangeDao].to(currencyExchangeDao),
      bind[CurrencyDao].to(currencyDao),
      bind[CurrencyExchangeCoreApiClient].to(fxApi),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val config = inject[AppConfig]
  val currencyRateMgmtService = inject[CurrencyRateMgmtService]
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

  "CurrencyRateMgmtService getCurrencyRateList" should {
    "return list sorted by id" in {
      (currencyDao.getAll _).when()
        .returns(Set(kes, usd, eur).asRight[DaoError])

      (currencyExchangeDao.getCurrencyExchangeByCriteria _)
        .when(CurrencyExchangeCriteria(), Seq(Ordering("base_currency", Ordering.DESC)), None, None)
        .returns(Seq(kesUsd, kesEur, usdKes, usdEur, eurKes, eurUsd).asRight[DaoError])

      val expected = Seq(
        CurrencyRate(
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
              sellRate = ExchangeRate(UUID.fromString(kesEur.uuid), kesEur.rate.getReciprocal)))),
        CurrencyRate(
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
              sellRate = ExchangeRate(UUID.fromString(usdEur.uuid), usdEur.rate.getReciprocal)))),
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
              sellRate = ExchangeRate(UUID.fromString(eurUsd.uuid), eurUsd.rate.getReciprocal)))))

      val currencyRateList = currencyRateMgmtService.getCurrencyRateList(None, None)

      whenReady(currencyRateList)(result ⇒ {
        result mustBe Right(expected)
      })
    }

    "return list sorted by code asc" in {
      (currencyDao.getAll _).when()
        .returns(Set(kes, usd, eur).asRight[DaoError])

      (currencyExchangeDao.getCurrencyExchangeByCriteria _)
        .when(CurrencyExchangeCriteria(), Seq(Ordering("base_currency", Ordering.DESC)), None, None)
        .returns(Seq(kesUsd, kesEur, usdKes, usdEur, eurKes, eurUsd).asRight[DaoError])

      val expected = Seq(
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
              sellRate = ExchangeRate(UUID.fromString(eurUsd.uuid), eurUsd.rate.getReciprocal)))),
        CurrencyRate(
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
              sellRate = ExchangeRate(UUID.fromString(kesEur.uuid), kesEur.rate.getReciprocal)))),
        CurrencyRate(
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
              sellRate = ExchangeRate(UUID.fromString(usdEur.uuid), usdEur.rate.getReciprocal)))))

      val currencyRateList = currencyRateMgmtService.getCurrencyRateList(DomainOrdering("code", DomainOrdering.ASCENDING).some, None)

      whenReady(currencyRateList)(result ⇒ {
        result mustBe Right(expected)
      })
    }

    "return list sorted by code desc" in {
      (currencyDao.getAll _).when()
        .returns(Set(kes, usd, eur).asRight[DaoError])

      (currencyExchangeDao.getCurrencyExchangeByCriteria _)
        .when(CurrencyExchangeCriteria(), Seq(Ordering("base_currency", Ordering.DESC)), None, None)
        .returns(Seq(kesUsd, kesEur, usdKes, usdEur, eurKes, eurUsd).asRight[DaoError])

      val expected = Seq(
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
              sellRate = ExchangeRate(UUID.fromString(eurUsd.uuid), eurUsd.rate.getReciprocal)))),
        CurrencyRate(
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
              sellRate = ExchangeRate(UUID.fromString(kesEur.uuid), kesEur.rate.getReciprocal)))),
        CurrencyRate(
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
              sellRate = ExchangeRate(UUID.fromString(usdEur.uuid), usdEur.rate.getReciprocal))))).reverse

      val currencyRateList = currencyRateMgmtService.getCurrencyRateList(DomainOrdering("code", DomainOrdering.DESCENDING).some, None)

      whenReady(currencyRateList)(result ⇒ {
        result mustBe Right(expected)
      })
    }

    "return list sorted by id desc, show empty" in {
      (currencyDao.getAll _).when()
        .returns(Set(kes, usd, eur).asRight[DaoError])

      (currencyExchangeDao.getCurrencyExchangeByCriteria _)
        .when(CurrencyExchangeCriteria(), Seq(Ordering("base_currency", Ordering.DESC)), None, None)
        .returns(Seq(kesUsd, kesEur, usdKes, usdEur, eurKes, eurUsd).asRight[DaoError])

      val expected = Seq(
        CurrencyRate(
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
              sellRate = ExchangeRate(UUID.fromString(kesEur.uuid), kesEur.rate.getReciprocal)))),
        CurrencyRate(
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
              sellRate = ExchangeRate(UUID.fromString(usdEur.uuid), usdEur.rate.getReciprocal)))),
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
              sellRate = ExchangeRate(UUID.fromString(eurUsd.uuid), eurUsd.rate.getReciprocal))))).reverse

      val currencyRateList = currencyRateMgmtService.getCurrencyRateList(DomainOrdering("id", DomainOrdering.DESCENDING).some, true.some)

      whenReady(currencyRateList)(result ⇒ {
        result mustBe Right(expected)
      })
    }

    "return currencyRate matching the currency id" in {
      (currencyDao.getAll _).when()
        .returns(Set(kes, usd, eur).asRight[DaoError])

      (currencyExchangeDao.getCurrencyExchangeByCriteria _)
        .when(CurrencyExchangeCriteria(), Seq(Ordering("base_currency", Ordering.DESC)), None, None)
        .returns(Seq(kesUsd, kesEur, usdKes, usdEur, eurKes, eurUsd).asRight[DaoError])

      val expected =
        CurrencyRate(
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

      val currencyRateList = currencyRateMgmtService.getCurrencyRateById(kes.id)

      whenReady(currencyRateList)(result ⇒ {
        result mustBe Right(expected)
      })
    }

    "return notfound error in getCurrencyRateById when currency doesn't exist" in {
      (currencyDao.getAll _).when()
        .returns(Set(kes, usd, eur).asRight[DaoError])

      val phpId = 100

      (currencyExchangeDao.getCurrencyExchangeByCriteria _)
        .when(CurrencyExchangeCriteria(), Seq(Ordering("base_currency", Ordering.DESC)), None, None)
        .returns(Seq(kesUsd, kesEur, usdKes, usdEur, eurKes, eurUsd).asRight[DaoError])

      val currencyRateList = currencyRateMgmtService.getCurrencyRateById(phpId)

      whenReady(currencyRateList)(result ⇒ {
        result.leftMap(_.message) mustBe Left(s"CurrencyId $phpId not found")
      })
    }

    "return all currency with rates once currency rates are updated" in {

      (currencyExchangeDao.findByMultipleUuid _).when(*)
        .returns(Seq(usdKes, kesUsd).asRight[DaoError])
      (currencyDao.isCurrencyActive _).when(*)
        .returns(true.asRight[DaoError])

      (fxApi.batchUpdateFxStatus(
        _: Seq[Long],
        _: String,
        _: String,
        _: String,
        _: Option[LocalDateTime],
        _: Map[String, BigDecimal])(_: UUID)).when(*, *, *, *, *, *, *).returns(Future.successful(Right()))
      (currencyDao.getAll _).when()
        .returns(Set(kes, usd, eur).asRight[DaoError])

      (currencyExchangeDao.getCurrencyExchangeByCriteria _)
        .when(CurrencyExchangeCriteria(), Seq(Ordering("base_currency", Ordering.DESC)), None, None)
        .returns(Seq(kesUsd, kesEur, usdKes, usdEur, eurKes, eurUsd).asRight[DaoError])

      val expected = Seq(
        CurrencyRate(
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
              sellRate = ExchangeRate(UUID.fromString(kesEur.uuid), kesEur.rate.getReciprocal)))),
        CurrencyRate(
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
              sellRate = ExchangeRate(UUID.fromString(usdEur.uuid), usdEur.rate.getReciprocal)))),
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
              sellRate = ExchangeRate(UUID.fromString(eurUsd.uuid), eurUsd.rate.getReciprocal)))))

      val buyRate = ExchangeRateToUpdate(id = UUID.fromString(usdKes.uuid), rate = BigDecimal(100.00))
      val sellRate = ExchangeRateToUpdate(id = UUID.fromString(kesUsd.uuid), rate = BigDecimal(50.00))
      val currencyRate = RateToUpdate(
        code = usdKes.currencyCode,
        description = none,
        buyRate = buyRate,
        sellRate = sellRate)

      val currencyToUpdate = CurrencyRateToUpdate(
        name = "",
        description = none,
        rates = Seq(currencyRate),
        updatedAt = LocalDateTime.now(mockClock),
        updatedBy = "ujali")

      val currencyRateList = currencyRateMgmtService.updateCurrencyRateList(1, none, currencyToUpdate)

      whenReady(currencyRateList)(result ⇒ {
        result mustBe Right(expected)
      })
    }

    "return error if update fails on fxApi" in {
      val fakeRequestId = UUID.randomUUID()

      (currencyExchangeDao.findByMultipleUuid _).when(*)
        .returns(Seq(usdKes, kesUsd).asRight[DaoError])

      (fxApi.batchUpdateFxStatus(
        _: Seq[Long],
        _: String,
        _: String,
        _: String,
        _: Option[LocalDateTime],
        _: Map[String, BigDecimal])(_: UUID)).when(*, *, *, *, *, *, *).returns(Future.successful(Left(ServiceError.unknownError("unknow error while sending ", fakeRequestId.toOption))))

      val buyRate = ExchangeRateToUpdate(id = UUID.fromString(usdKes.uuid), rate = BigDecimal(100.00))
      val sellRate = ExchangeRateToUpdate(id = UUID.fromString(kesUsd.uuid), rate = BigDecimal(50.00))
      val currencyRate = RateToUpdate(
        code = usdKes.currencyCode,
        description = none,
        buyRate = buyRate,
        sellRate = sellRate)

      val currencyToUpdate = CurrencyRateToUpdate(
        name = "",
        description = none,
        rates = Seq(currencyRate),
        updatedAt = LocalDateTime.now(mockClock),
        updatedBy = "ujali")

      val currencyRateList = currencyRateMgmtService.updateCurrencyRateList(1, none, currencyToUpdate)

      whenReady(currencyRateList)(result ⇒ {

        result.isLeft
        result.left.get.message contains s"Error from wallet core api. Please check logs."
      })
    }

    "return not relative currency found error if provided uuid does not exists" in {

      (currencyExchangeDao.findByMultipleUuid _).when(*)
        .returns(Seq(usdKes, kesUsd).asRight[DaoError])
      (currencyDao.isCurrencyActive _).when(*)
        .returns(true.asRight[DaoError])

      (fxApi.batchUpdateFxStatus(
        _: Seq[Long],
        _: String,
        _: String,
        _: String,
        _: Option[LocalDateTime],
        _: Map[String, BigDecimal])(_: UUID)).when(*, *, *, *, *, *, *).returns(Future.successful(Right()))
      (currencyDao.getAll _).when()
        .returns(Set(kes, usd, eur).asRight[DaoError])

      (currencyExchangeDao.getCurrencyExchangeByCriteria _)
        .when(CurrencyExchangeCriteria(), Seq(Ordering("base_currency", Ordering.DESC)), None, None)
        .returns(Seq(kesUsd, kesEur, usdKes, usdEur, eurKes, eurUsd).asRight[DaoError])

      val buyRate = ExchangeRateToUpdate(id = UUID.fromString(usdKes.uuid), rate = BigDecimal(100.00))
      val sellRate = ExchangeRateToUpdate(id = UUID.fromString(eurUsd.uuid), rate = BigDecimal(50.00))
      val currencyRate = RateToUpdate(
        code = usdKes.currencyCode,
        description = none,
        buyRate = buyRate,
        sellRate = sellRate)

      val currencyToUpdate = CurrencyRateToUpdate(
        name = "",
        description = none,
        rates = Seq(currencyRate),
        updatedAt = LocalDateTime.now(mockClock),
        updatedBy = "ujali")

      val currencyRateList = currencyRateMgmtService.updateCurrencyRateList(1, none, currencyToUpdate)

      whenReady(currencyRateList)(result ⇒ {
        result.leftMap(_.message) mustBe Left(s"no relative id found for uuid ${sellRate.copy(rate = BigDecimal(1) / sellRate.rate)}")
      })
    }
  }
}
