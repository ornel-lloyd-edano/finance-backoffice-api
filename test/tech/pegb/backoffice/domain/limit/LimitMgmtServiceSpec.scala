package tech.pegb.backoffice.domain.limit

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.{Currency, UUID}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.test.Injecting
import tech.pegb.backoffice.core.integration.abstraction.LimitProfileCoreApiClient
import tech.pegb.backoffice.dao.Dao.UUIDEntityId
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.limit.abstraction.LimitProfileDao
import tech.pegb.backoffice.dao.limit.dto.LimitProfileToInsert
import tech.pegb.backoffice.dao.limit.entity.LimitProfile
import tech.pegb.backoffice.dao.limit.sql.LimitProfileSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerSubscription, CustomerTier}
import tech.pegb.backoffice.domain.customer.model.UserType
import tech.pegb.backoffice.domain.limit.dto.{LimitProfileCriteria, LimitProfileToCreate}
import tech.pegb.backoffice.domain.limit.implementation.LimitMgmtService
import tech.pegb.backoffice.domain.limit.model.{LimitType, TimeIntervalWrapper}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.domain.{BaseService, ServiceError}
import tech.pegb.backoffice.mapping.dao.domain.limit.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.limit.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class LimitMgmtServiceSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures with BaseService {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val limitProfileDao = stub[LimitProfileDao]
  private val currencyDao = stub[CurrencyDao]
  private val apiClient: LimitProfileCoreApiClient = stub[LimitProfileCoreApiClient]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[LimitProfileDao].to(limitProfileDao),
      bind[CurrencyDao].to(currencyDao),
      bind[LimitProfileCoreApiClient].toInstance(apiClient),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val limitMgmtService = inject[LimitMgmtService]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  private val limitProfileToCreate = LimitProfileToCreate(
    limitType = LimitType.TransactionBased,
    userType = UserType("business_user"),
    tier = CustomerTier("tier 2"),
    subscription = CustomerSubscription("standard"),
    transactionType = Some(TransactionType("top-up")),
    channel = Some(Channel("bank")),
    otherParty = Some("nbd"),
    instrument = Some("card"),
    interval = Some(TimeIntervalWrapper("daily")),
    maxIntervalAmount = Some(BigDecimal(50.00)),
    maxAmount = Some(BigDecimal(50.00)),
    minAmount = Some(BigDecimal(40.00)),
    maxCount = Some(2),
    maxBalance = None,
    currencyCode = Currency.getInstance("KES"),
    createdBy = "pegbuser",
    createdAt = LocalDateTime.now(mockClock))

  val limit1 = LimitProfile(
    id = 1,
    uuid = UUID.randomUUID(),
    limitType = LimitType.TransactionBased.underlying,
    userType = "individual_user",
    tier = "tier 1",
    subscription = "standard",
    transactionType = Some("top-up"),
    channel = Some("atm"),
    provider = Some("Mashreq"),
    instrument = Some("debit_card"),
    interval = Some("daily"),
    maxIntervalAmount = None,
    maxAmount = Some(BigDecimal(10000)),
    minAmount = None,
    maxCount = Some(50000),
    currencyCode = "USD",
    deletedAt = None,
    createdBy = "pegbuser",
    createdAt = LocalDateTime.now(mockClock),
    updatedBy = None,
    updatedAt = None)
  val limit2 = LimitProfile(
    id = 2,
    uuid = UUID.randomUUID(),
    limitType = LimitType.TransactionBased.underlying,
    userType = "individual_user",
    tier = "tier 1",
    subscription = "gold",
    transactionType = Some("p2p_domestic"),
    channel = Some("mobile_application"),
    provider = None,
    instrument = Some("debit_card"),
    interval = Some("daily"),
    maxIntervalAmount = None,
    maxAmount = Some(BigDecimal(10000)),
    minAmount = None,
    maxCount = Some(50000),
    currencyCode = "USD",
    deletedAt = Some(LocalDateTime.now(mockClock)),
    createdBy = "pegbuser",
    createdAt = LocalDateTime.now(mockClock),
    updatedBy = None,
    updatedAt = None)
  val limit3 = LimitProfile(
    id = 3,
    uuid = UUID.randomUUID(),
    limitType = LimitType.TransactionBased.underlying,
    userType = "individual_user",
    tier = "tier 2",
    subscription = "platinum",
    transactionType = Some("top-up"),
    channel = Some("atm"),
    provider = Some("Mashreq"),
    instrument = Some("debit_card"),
    interval = Some("daily"),
    maxIntervalAmount = None,
    maxAmount = Some(BigDecimal(50000)),
    minAmount = None,
    maxCount = Some(50000),
    currencyCode = "USD",
    deletedAt = None,
    createdBy = "pegbuser",
    createdAt = LocalDateTime.now(mockClock),
    updatedBy = None,
    updatedAt = None)
  val limit4 = LimitProfile(
    id = 1,
    uuid = UUID.randomUUID(),
    limitType = LimitType.TransactionBased.underlying,
    userType = "business_user",
    tier = "tier 1",
    subscription = "standard",
    transactionType = Some("withdrawal"),
    channel = Some("atm"),
    provider = Some("Mashreq"),
    instrument = Some("debit_card"),
    interval = Some("daily"),
    maxIntervalAmount = None,
    maxAmount = Some(BigDecimal(100000)),
    minAmount = Some(BigDecimal(10000)),
    maxCount = Some(50000),
    currencyCode = "USD",
    deletedAt = None,
    createdBy = "pegbuser",
    createdAt = LocalDateTime.now(mockClock),
    updatedBy = None,
    updatedAt = None)

  "LimitMgmtService countLimitProfileByCriteria" should {
    "return count based on criteria" in {
      val limitProfileCriteria = LimitProfileCriteria(
        channel = Some(Channel("atm")))

      (limitProfileDao.countLimitProfileByCriteria _).when(limitProfileCriteria.asDao)
        .returns(Right(3))

      val result = limitMgmtService.countLimitProfileByCriteria(limitProfileCriteria)

      whenReady(result) { actual ⇒
        actual mustBe Right(3)
      }
    }
  }

  "LimitMgmtService getLimitProfile" should {
    "return limit profile matching uuid" in {
      implicit val requestId: UUID = UUID.randomUUID()
      (limitProfileDao.getLimitProfile _).when(UUIDEntityId(limit1.uuid))
        .returns(Right(Some(limit1)))

      val result = limitMgmtService.getLimitProfile(limit1.uuid)

      whenReady(result) { actual ⇒
        actual mustBe Right(limit1.asDomain)
      }
    }
    "return notFoundError when profile doesnt exist" in {
      implicit val requestId = UUID.randomUUID()
      val fakeUUID = UUID.randomUUID()
      (limitProfileDao.getLimitProfile _).when(UUIDEntityId(fakeUUID))
        .returns(Right(None))

      val result = limitMgmtService.getLimitProfile(fakeUUID)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.notFoundError(s"LimitProfile with uuid $fakeUUID not found", requestId.toOption))
      }
    }
    "return notFoundError when profile has value in deletedAt" in {
      implicit val requestId = UUID.randomUUID()
      (limitProfileDao.getLimitProfile _).when(UUIDEntityId(limit2.uuid))
        .returns(Right(Some(limit2)))

      val result = limitMgmtService.getLimitProfile(limit2.uuid)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.notFoundError(s"LimitProfile with uuid ${limit2.uuid} not found", requestId.toOption))
      }
    }
  }

  "LimitMgmtService getLimitProfileByCriteria" should {
    "return list of limitProfiles satisfying query" in {
      val limitProfileCriteria = LimitProfileCriteria(
        limitType = Some(LimitType.TransactionBased))

      val ordering = Seq(Ordering("max_amount", Ordering.DESCENDING))

      (limitProfileDao.getLimitProfileByCriteria _).when(limitProfileCriteria.asDao, ordering.asDao, None, None)
        .returns(Right(Seq(limit4, limit3)))

      val result = limitMgmtService.getLimitProfileByCriteria(limitProfileCriteria, ordering, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(limit4.asDomain, limit3.asDomain))
      }
    }
  }

  "LimitMgmtService create" should {
    import tech.pegb.backoffice.dao.limit.dto.LimitProfileCriteria

    "return the successfully saved limit profile" in {
      implicit val requestId: UUID = UUID.randomUUID()
      val limitProfileCriteria = limitProfileToCreate.asDaoCriteria
      val limitProfileCriteriaWithoutCriteria = limitProfileToCreate.asDaoCriteriaWithoutInterval

      val limitProfileCriteria1 = LimitProfileCriteria(
        limitType = none,
        userType = CriteriaField(LimitProfileSqlDao.cUserType, "business_user").some,
        tier = CriteriaField(LimitProfileSqlDao.cTier, "tier 2").some,
        subscription = CriteriaField(LimitProfileSqlDao.cSubscription, "standard").some,
        channel = CriteriaField(LimitProfileSqlDao.cChannel, "bank").some,
        transactionType = CriteriaField(LimitProfileSqlDao.cTransactionType, "top-up").some,
        instrument = CriteriaField(LimitProfileSqlDao.cInstrument, "card").some,
        provider = CriteriaField("other_party", "nbd", MatchTypes.Exact).some,
        interval = CriteriaField(LimitProfileSqlDao.cInterval, "daily").some,
        isDeleted = CriteriaField(LimitProfileSqlDao.cDeletedAt, false).some)

      val insert = limitProfileToCreate.asDao(1)
      val insert1 = LimitProfileToInsert(
        limitType = "balance_based",
        userType = Some("business_user"),
        tier = Some("tier 2"),
        subscription = Some("standard"),
        transactionType = Some("top-up"),
        channel = Some("bank"),
        provider = Some("nbd"),
        instrument = Some("card"),
        maxIntervalAmount = Some(50.0),
        maxAmount = Some(50.0),
        minAmount = Some(40.0),
        maxCount = Some(2),
        interval = Some("daily"),
        currencyId = 1,
        createdBy = "pegbuser",
        createdAt = LocalDateTime.now(mockClock))

      val limitProfile = LimitProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        limitType = "balance_based",
        userType = "business_user",
        tier = "tier 2",
        subscription = "standard",
        transactionType = Some("top-up"),
        channel = Some("bank"),
        provider = Some("nbd"),
        instrument = Some("card"),
        interval = Some("daily"),
        maxIntervalAmount = Some(BigDecimal(50.0)),
        maxAmount = Some(BigDecimal(50.0)),
        minAmount = Some(BigDecimal(40.0)),
        maxCount = Some(2),
        currencyCode = "KES",
        deletedAt = None,
        createdBy = "pegbuser",
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = None,
        updatedAt = None)

      (typesDao.getTimeIntervalTypes _).when()
        .returns(Right(List(
          (1, "daily", None),
          (2, "monthly", None))))
      (typesDao.getCustomerTypes _).when()
        .returns(Right(List(
          (1, "business_user", None),
          (2, "individual_user", None))))
      (typesDao.getCustomerTiers _).when()
        .returns(Right(List(
          (1, "tier 1", None),
          (2, "tier 2", None))))
      (typesDao.getCustomerSubscriptions _).when()
        .returns(Right(List(
          (1, "standard", None),
          (2, "gold", None))))
      (typesDao.getTransactionTypes _).when()
        .returns(Right(List(
          (1, "top-up", None),
          (2, "international remittance", None))))
      (typesDao.getChannels _).when()
        .returns(Right(List(
          (1, "bank", None),
          (2, "atm", None))))
      (typesDao.getInstruments _).when()
        .returns(Right(List(
          (1, "card", None),
          (2, "atm", None))))
      (typesDao.getLimitTypes _).when()
        .returns(Right(List(
          (1, "balance_based", None),
          (2, "transaction_based", None))))

      (limitProfileDao.getLimitProfileByCriteria _).when(limitProfileCriteria, None, None, None)
        .returns(Right(Seq.empty))
      (limitProfileDao.getLimitProfileByCriteria _).when(limitProfileCriteriaWithoutCriteria, None, None, None)
        .returns(Right(Seq.empty))
      (currencyDao.getCurrenciesWithId _).when(Some(true))
        .returns(Right(List((1, "KES"), (2, "USD"), (3, "INR"))))
      (currencyDao.getAllNames _).when()
        .returns(Right(Set("KES")))
      (limitProfileDao.insertLimitProfile _).when(insert)
        .returns(Right(limitProfile))
      (apiClient.notifyLimitProfileUpdated(_: Int)(_: UUID))
        .when(limitProfile.id, *)
        .returns(Future.successful(Right(())))
        .once()

      val result = limitMgmtService.createLimitProfile(limitProfileToCreate)

      whenReady(result) { response ⇒
        response mustBe Right(limitProfile.asDomain)
      }
    }

    "return error when currency is inactive" in {
      implicit val requestId: UUID = UUID.randomUUID()
      val limitProfileCriteria = limitProfileToCreate.asDaoCriteria
      val limitProfileCriteriaWithoutCriteria = limitProfileToCreate.asDaoCriteriaWithoutInterval

      val limitProfileCriteria1 = LimitProfileCriteria(
        limitType = none,
        userType = CriteriaField(LimitProfileSqlDao.cUserType, "business_user").some,
        tier = CriteriaField(LimitProfileSqlDao.cTier, "tier 2").some,
        subscription = CriteriaField(LimitProfileSqlDao.cSubscription, "standard").some,
        channel = CriteriaField(LimitProfileSqlDao.cChannel, "bank").some,
        transactionType = CriteriaField(LimitProfileSqlDao.cTransactionType, "top-up").some,
        instrument = CriteriaField(LimitProfileSqlDao.cInstrument, "card").some,
        provider = CriteriaField("other_party", "nbd", MatchTypes.Exact).some,
        interval = CriteriaField(LimitProfileSqlDao.cInterval, "daily").some,
        isDeleted = CriteriaField(LimitProfileSqlDao.cDeletedAt, false).some)

      val insert = limitProfileToCreate.asDao(1)
      val insert1 = LimitProfileToInsert(
        limitType = "balance_based",
        userType = Some("business_user"),
        tier = Some("tier 2"),
        subscription = Some("standard"),
        transactionType = Some("top-up"),
        channel = Some("bank"),
        provider = Some("nbd"),
        instrument = Some("card"),
        maxIntervalAmount = Some(50.0),
        maxAmount = Some(50.0),
        minAmount = Some(40.0),
        maxCount = Some(2),
        interval = Some("daily"),
        currencyId = 1,
        createdBy = "pegbuser",
        createdAt = LocalDateTime.now(mockClock))

      val limitProfile = LimitProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        limitType = "balance_based",
        userType = "business_user",
        tier = "tier 2",
        subscription = "standard",
        transactionType = Some("top-up"),
        channel = Some("bank"),
        provider = Some("nbd"),
        instrument = Some("card"),
        interval = Some("daily"),
        maxIntervalAmount = Some(BigDecimal(50.0)),
        maxAmount = Some(BigDecimal(50.0)),
        minAmount = Some(BigDecimal(40.0)),
        maxCount = Some(2),
        currencyCode = "KES",
        deletedAt = None,
        createdBy = "pegbuser",
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = None,
        updatedAt = None)

      (typesDao.getTimeIntervalTypes _).when()
        .returns(Right(List(
          (1, "daily", None),
          (2, "monthly", None))))
      (typesDao.getCustomerTypes _).when()
        .returns(Right(List(
          (1, "business_user", None),
          (2, "individual_user", None))))
      (typesDao.getCustomerTiers _).when()
        .returns(Right(List(
          (1, "tier 1", None),
          (2, "tier 2", None))))
      (typesDao.getCustomerSubscriptions _).when()
        .returns(Right(List(
          (1, "standard", None),
          (2, "gold", None))))
      (typesDao.getTransactionTypes _).when()
        .returns(Right(List(
          (1, "top-up", None),
          (2, "international remittance", None))))
      (typesDao.getChannels _).when()
        .returns(Right(List(
          (1, "bank", None),
          (2, "atm", None))))
      (typesDao.getInstruments _).when()
        .returns(Right(List(
          (1, "card", None),
          (2, "atm", None))))
      (typesDao.getLimitTypes _).when()
        .returns(Right(List(
          (1, "balance_based", None),
          (2, "transaction_based", None))))

      (limitProfileDao.getLimitProfileByCriteria _).when(limitProfileCriteria, None, None, None)
        .returns(Right(Seq.empty))
      (limitProfileDao.getLimitProfileByCriteria _).when(limitProfileCriteriaWithoutCriteria, None, None, None)
        .returns(Right(Seq.empty))
      (currencyDao.getCurrenciesWithId _).when(Some(true))
        .returns(Right(List((1, "AED"), (2, "USD"), (3, "INR"))))
      (currencyDao.getAllNames _).when()
        .returns(Right(Set("KES")))
      (limitProfileDao.insertLimitProfile _).when(insert)
        .returns(Right(limitProfile))

      val result = limitMgmtService.createLimitProfile(limitProfileToCreate)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError("no active currency id found for code KES"))
      }
    }
  }
}
