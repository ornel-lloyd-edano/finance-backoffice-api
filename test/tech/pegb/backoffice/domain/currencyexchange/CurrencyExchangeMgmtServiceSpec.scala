package tech.pegb.backoffice.domain.currencyexchange

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.{Currency, UUID}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.test.Injecting
import tech.pegb.backoffice.core.integration.abstraction.CurrencyExchangeCoreApiClient
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.currencyexchange.abstraction.{CurrencyExchangeDao, SpreadsDao}
import tech.pegb.backoffice.dao.currencyexchange.dto.{CurrencyExchangeCriteria ⇒ DaoCurrencyExchangeCriteria, SpreadUpdateDto ⇒ DaoSpreadUpdateDto}
import tech.pegb.backoffice.dao.currencyexchange.entity.{CurrencyExchange, Spread ⇒ DaoSpread}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.account.abstraction.AccountManagement
import tech.pegb.backoffice.domain.currencyexchange.dto.{CurrencyExchangeCriteria, SpreadUpdateDto}
import tech.pegb.backoffice.domain.currencyexchange.implementation.CurrencyExchangeMgmtService
import tech.pegb.backoffice.domain.currencyexchange.model.CurrencyExchangeStatus
import tech.pegb.backoffice.mapping.dao.domain.currencyexchange.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.currencyexchange.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, Utils, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class CurrencyExchangeMgmtServiceSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val currencyExchangeDao = stub[CurrencyExchangeDao]
  val accountManagement = stub[AccountManagement]
  val spreadsDao = stub[SpreadsDao]
  private val fxApiClient = stub[CurrencyExchangeCoreApiClient]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[AccountManagement].to(accountManagement),
      bind[CurrencyExchangeDao].to(currencyExchangeDao),
      bind[SpreadsDao].to(spreadsDao),
      bind[CurrencyExchangeCoreApiClient].toInstance(fxApiClient),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val config = inject[AppConfig]
  val currencyExchangeMgmtService = inject[CurrencyExchangeMgmtService]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val kesId = 1
  val usdId = 2
  val euroId = 3
  val cnyId = 4
  val chfId = 5

  val cloudProviderId = 1
  val eburyProviderId = 2

  val usdEscrowUUID = UUID.randomUUID()
  val euroEscrowUUID = UUID.randomUUID()
  val cnyEscrowUUID = UUID.randomUUID()
  val chfEscrowUUID = UUID.randomUUID()
  val kesEscrowUUID = UUID.randomUUID()

  val usdBalance = BigDecimal("100.00")
  val euroBalance = BigDecimal("100.00")
  val cnyBalance = BigDecimal("100.00")
  val chfBalance = BigDecimal("100.00")

  val fxId1 = UUID.randomUUID
  val fx1 = CurrencyExchange(
    id = 1,
    uuid = fxId1.toString,
    currencyId = usdId,
    currencyCode = "USD",
    baseCurrencyId = kesId,
    baseCurrency = "KES",
    rate = BigDecimal(99.9800),
    providerId = cloudProviderId,
    provider = "Currency Cloud",
    targetCurrencyAccountId = usdId,
    targetCurrencyAccountUuid = usdEscrowUUID.toString,
    baseCurrencyAccountId = kesId,
    baseCurrencyAccountUuid = kesEscrowUUID.toString,
    balance = usdBalance,
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))
  val fxId2 = UUID.randomUUID
  val fx2 = CurrencyExchange(
    id = 2,
    uuid = fxId2.toString,
    currencyId = euroId,
    currencyCode = "EUR",
    baseCurrencyId = kesId,
    baseCurrency = "KES",
    rate = BigDecimal(112.1020),
    providerId = cloudProviderId,
    provider = "Currency Cloud",
    targetCurrencyAccountId = euroId,
    targetCurrencyAccountUuid = euroEscrowUUID.toString,
    baseCurrencyAccountId = kesId,
    baseCurrencyAccountUuid = kesEscrowUUID.toString,
    balance = euroBalance,
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))
  val fxId3 = UUID.randomUUID
  val fx3 = CurrencyExchange(
    id = 3,
    uuid = fxId3.toString,
    currencyId = cnyId,
    currencyCode = "CNY",
    baseCurrencyId = kesId,
    baseCurrency = "KES",
    rate = BigDecimal(75.9501),
    providerId = eburyProviderId,
    provider = "Ebury",
    targetCurrencyAccountId = cnyId,
    targetCurrencyAccountUuid = cnyEscrowUUID.toString,
    baseCurrencyAccountId = kesId,
    baseCurrencyAccountUuid = kesEscrowUUID.toString,
    balance = cnyBalance,
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))
  val fxId4 = UUID.randomUUID
  val fx4 = CurrencyExchange(
    id = 4,
    uuid = fxId4.toString,
    currencyId = chfId,
    currencyCode = "CHF",
    baseCurrencyId = kesId,
    baseCurrency = "KES",
    rate = BigDecimal(152.2014),
    providerId = eburyProviderId,
    provider = "Ebury",
    targetCurrencyAccountId = chfId,
    targetCurrencyAccountUuid = chfEscrowUUID.toString,
    baseCurrencyAccountId = kesId,
    baseCurrencyAccountUuid = kesEscrowUUID.toString,
    balance = chfBalance,
    status = "inactive",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))

  "CurrencyExchangeMgmtService countCurrencyExchangeByCriteria " should {
    "return count based on criteria" in {
      //val criteria = CurrencyExchangeCriteria(baseCurrency = Some(CriteriaField("base_currency", "KES")))
      val criteria = CurrencyExchangeCriteria(baseCurrency = Some(Currency.getInstance("KES")))

      (currencyExchangeDao.countTotalCurrencyExchangeByCriteria _).when(criteria.asDao)
        .returns(Right(4))

      val result = currencyExchangeMgmtService.countCurrencyExchangeByCriteria(criteria)

      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe 4
      }
    }
  }

  "CurrencyExchangeMgmtService getCurrencyExchangeByCriteria " should {
    "return list of all currency exchange when currencyExchagneCriteria is minimal" in {
      val criteria = CurrencyExchangeCriteria()
      val daoReturn = Seq(fx1, fx2, fx3, fx4)

      (currencyExchangeDao.getCurrencyExchangeByCriteria _).when(criteria.asDao, Nil, None, None)
        .returns(Right(daoReturn))

      val expected = daoReturn.flatMap(_.asDomain.toOption)

      val result = currencyExchangeMgmtService.getCurrencyExchangeByCriteria(criteria, Nil, None, None)

      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe expected
      }
    }
    "return list of currency exchanges based on filters" in {
      val criteria = CurrencyExchangeCriteria(status = Some(CurrencyExchangeStatus.Active))
      val daoReturn = Seq(fx1, fx2, fx3)

      (currencyExchangeDao.getCurrencyExchangeByCriteria _).when(criteria.asDao, Nil, None, None)
        .returns(Right(daoReturn))
      val expected = daoReturn.flatMap(_.asDomain.toOption)

      val result = currencyExchangeMgmtService.getCurrencyExchangeByCriteria(criteria, Nil, None, None)

      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe expected
      }
    }
  }

  "CurrencyExchangeMgmtService getCurrencyByUUID " should {
    "return currency exchange matching uuid" in {
      val criteria = CurrencyExchangeCriteria(id = Some(UUIDLike(fxId1.toString)))
      val daoReturn = Seq(fx1)

      (currencyExchangeDao.getCurrencyExchangeByCriteria _).when(criteria.asDao, Nil, None, None)
        .returns(Right(daoReturn))

      (currencyExchangeDao.getDailyAmount _).when(fx1.targetCurrencyAccountId, fx1.baseCurrencyAccountId)
        .returns(Right(BigDecimal("130.50").some))

      val expected = fx1.asDomain.toOption.get.copy(dailyAmount = BigDecimal("130.50").some)

      val result = currencyExchangeMgmtService.getCurrencyExchangeByUUID(fxId1)(UUID.randomUUID())

      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe expected
      }
    }
    "return notfound error when currency exchange uuid does not exist" in {
      val fakeUUID = UUID.randomUUID()
      val criteria = CurrencyExchangeCriteria(id = Some(UUIDLike(fakeUUID.toString)))
      val daoReturn = Nil

      (currencyExchangeDao.getCurrencyExchangeByCriteria _).when(criteria.asDao, Nil, None, None)
        .returns(Right(daoReturn))

      val result = currencyExchangeMgmtService.getCurrencyExchangeByUUID(fakeUUID)(UUID.randomUUID())

      val expected = s"Currency Exchange for id $fakeUUID is not found"
      whenReady(result) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe expected
      }
    }
  }

  "update valid spread and fail to update invalid" in {
    implicit val requestId: UUID = UUID.randomUUID()
    val id = UUID.randomUUID()
    val invalidFxId = UUID.randomUUID()
    val now = Utils.nowAsLocal()
    val daoSpread = DaoSpread(
      id = 180,
      uuid = id,
      currencyExchangeId = 810,
      currencyExchangeUuid = fxId1,
      transactionType = "currency_exchange",
      channel = None,
      recipientInstitution = None,
      spread = BigDecimal(0.5),
      deletedAt = None,
      createdBy = getClass.getSimpleName,
      createdAt = now.minusMinutes(1L),
      updatedBy = Some(getClass.getSimpleName),
      updatedAt = Some(now))
    val spreadDomain = daoSpread.asDomain(fx1.asDomain.toOption.get)
    val validDto = SpreadUpdateDto(
      id = daoSpread.uuid,
      currencyExchangeId = daoSpread.currencyExchangeUuid,
      spread = daoSpread.spread,
      updatedBy = getClass.getSimpleName,
      updatedAt = Utils.nowAsLocal(),
      lastUpdatedAt = None)
    val invalidDto = validDto.copy(currencyExchangeId = invalidFxId)
    val usdFxCriteria = DaoCurrencyExchangeCriteria(
      id = Some(CriteriaField("uuid", fxId1.toString, MatchTypes.Exact)))
    val invalidFxCriteria = DaoCurrencyExchangeCriteria(
      id = Some(CriteriaField("uuid", invalidFxId.toString, MatchTypes.Exact)))

    (spreadsDao.update(_: UUID, _: DaoSpreadUpdateDto)(_: UUID))
      .when(validDto.id, validDto.asDao, requestId)
      .returns(daoSpread.asRight[DaoError])
    (spreadsDao.getSpread(_: UUID))
      .when(validDto.id)
      .returns(Some(daoSpread).asRight[DaoError])
    (currencyExchangeDao.getCurrencyExchangeByCriteria _)
      .when(usdFxCriteria, *, *, *)
      .returns(Right(Seq(fx1)))
    (currencyExchangeDao.getDailyAmount _).when(fx1.targetCurrencyAccountId, fx1.baseCurrencyAccountId)
      .returns(Right(none[BigDecimal]))
    (currencyExchangeDao.getCurrencyExchangeByCriteria _)
      .when(invalidFxCriteria, *, *, *)
      .returns(Right(Seq.empty))
    (fxApiClient.notifySpreadUpdated(_: Int)(_: UUID))
      .when(*, *)
      .returns(Future.successful(Right(())))
    val result1 = currencyExchangeMgmtService.updateSpread(validDto)
    val result2 = currencyExchangeMgmtService.updateSpread(invalidDto)
    whenReady(result1)(result ⇒ {
      result mustBe Right(spreadDomain)
    })
    whenReady(result2)(result ⇒ {
      result.isLeft mustBe true
    })
  }

  "update valid spread even if core notification fails" in {
    implicit val requestId: UUID = UUID.randomUUID()
    val id = UUID.randomUUID()
    val now = Utils.nowAsLocal()
    val daoSpread = DaoSpread(
      id = 180,
      uuid = id,
      currencyExchangeId = 810,
      currencyExchangeUuid = fxId1,
      transactionType = "currency_exchange",
      channel = None,
      recipientInstitution = None,
      spread = BigDecimal(0.5),
      deletedAt = None,
      createdBy = getClass.getSimpleName,
      createdAt = now.minusMinutes(1L),
      updatedBy = Some(getClass.getSimpleName),
      updatedAt = Some(now))
    val spreadDomain = daoSpread.asDomain(fx1.asDomain.toOption.get)
    val validDto = SpreadUpdateDto(
      id = daoSpread.uuid,
      currencyExchangeId = daoSpread.currencyExchangeUuid,
      spread = daoSpread.spread,
      updatedBy = getClass.getSimpleName,
      updatedAt = Utils.nowAsLocal(),
      lastUpdatedAt = None)
    val usdFxCriteria = DaoCurrencyExchangeCriteria(
      id = Some(CriteriaField("uuid", fxId1.toString, MatchTypes.Exact)))
    (spreadsDao.update(_: UUID, _: DaoSpreadUpdateDto)(_: UUID))
      .when(validDto.id, validDto.asDao, requestId)
      .returns(daoSpread.asRight[DaoError])
    (spreadsDao.getSpread(_: UUID))
      .when(validDto.id)
      .returns(Some(daoSpread).asRight[DaoError])
    (currencyExchangeDao.getCurrencyExchangeByCriteria _)
      .when(usdFxCriteria, *, *, *)
      .returns(Right(Seq(fx1)))
    (currencyExchangeDao.getDailyAmount _).when(fx1.targetCurrencyAccountId, fx1.baseCurrencyAccountId)
      .returns(Right(none[BigDecimal]))
    (fxApiClient.notifySpreadUpdated(_: Int)(_: UUID))
      .when(*, *)
      .returns(Future.successful(Left(ServiceError.unknownError("error encountered in core notification", requestId.toOption))))
    val result1 = currencyExchangeMgmtService.updateSpread(validDto)
    whenReady(result1)(result ⇒ {
      result mustBe Right(spreadDomain)
    })
  }

  "delete existing spread and fail to remove invalid/non-existing" in {
    implicit val requestId: UUID = UUID.randomUUID()
    val id = UUID.randomUUID()
    val invalidId = UUID.randomUUID()
    val invalidFxId = UUID.randomUUID()
    val now = Utils.now()
    val nowUTC = now.toLocalDateTimeUTC
    val doneBy = getClass.getSimpleName
    val daoSpread = DaoSpread(
      id = 180,
      uuid = id,
      currencyExchangeId = 810,
      currencyExchangeUuid = fxId1,
      transactionType = "currency_exchange",
      channel = None,
      recipientInstitution = None,
      spread = BigDecimal(0.5),
      deletedAt = None,
      createdBy = doneBy,
      createdAt = nowUTC.minusMinutes(1L),
      updatedBy = Some(doneBy),
      updatedAt = Some(nowUTC))
    val spreadDomain = daoSpread.asDomain(fx1.asDomain.toOption.get)
    val usdFxCriteria = DaoCurrencyExchangeCriteria(
      id = Some(CriteriaField("uuid", fxId1.toString, MatchTypes.Exact)))
    val invalidFxCriteria = DaoCurrencyExchangeCriteria(
      id = Some(CriteriaField("uuid", invalidFxId.toString, MatchTypes.Exact)))
    (currencyExchangeDao.getCurrencyExchangeByCriteria _)
      .when(usdFxCriteria, *, *, *)
      .returns(Right(Seq(fx1)))
    (currencyExchangeDao.getDailyAmount _).when(fx1.targetCurrencyAccountId, fx1.baseCurrencyAccountId)
      .returns(Right(none[BigDecimal]))
    (currencyExchangeDao.getCurrencyExchangeByCriteria _)
      .when(invalidFxCriteria, *, *, *)
      .returns(Right(Seq.empty))
    (spreadsDao.update(_: UUID, _: DaoSpreadUpdateDto)(_: UUID))
      .when(id, DaoSpreadUpdateDto(
        spread = BigDecimal(0),
        updatedBy = doneBy,
        updatedAt = nowUTC,
        deletedAt = Some(nowUTC),
        lastUpdatedAt = None), *)
      .returns(daoSpread.asRight[DaoError])
    (spreadsDao.update(_: UUID, _: DaoSpreadUpdateDto)(_: UUID))
      .when(invalidId, DaoSpreadUpdateDto(
        spread = BigDecimal(0),
        updatedBy = doneBy,
        updatedAt = nowUTC,
        deletedAt = Some(nowUTC),
        lastUpdatedAt = None), *)
      .returns(DaoError.EntityNotFoundError("Spread was not found").asLeft[DaoSpread])

    val result1 = currencyExchangeMgmtService.deleteSpread(id, invalidFxId, now, doneBy, None)
    val result2 = currencyExchangeMgmtService.deleteSpread(invalidId, fxId1, now, doneBy, None)
    val result3 = currencyExchangeMgmtService.deleteSpread(id, fxId1, now, doneBy, None)
    whenReady(result1)(result ⇒ {
      result.isLeft mustBe true
    })
    whenReady(result2)(result ⇒ {
      result.isLeft mustBe true
    })
    whenReady(result3)(result ⇒ {
      result mustBe Right(spreadDomain)
    })
  }

}
