package tech.pegb.backoffice.dao

import cats.implicits._
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import tech.pegb.backoffice.dao.DaoError.PreconditionFailed
import tech.pegb.backoffice.dao.limit.abstraction.{LimitProfileDao, LimitProfileHistoryDao}
import tech.pegb.backoffice.dao.limit.dto.{LimitProfileCriteria, LimitProfileToInsert, LimitProfileToUpdate}
import tech.pegb.backoffice.dao.limit.entity.LimitProfile
import tech.pegb.backoffice.dao.limit.sql.LimitProfileSqlDao
import tech.pegb.backoffice.mapping.domain.dao.Implicits.UUIDConverter
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering, OrderingSet}
import tech.pegb.backoffice.domain.limit.model.LimitType
import tech.pegb.backoffice.util.Utils
import tech.pegb.core.PegBTestApp

class LimitProfileDaoSpec extends PegBTestApp {
  private var id: Int = _
  private var uuid: UUID = _

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val lpUUID1 = UUID.randomUUID()
  val lpUUID2 = UUID.randomUUID()
  val lpUUID3 = UUID.randomUUID()
  val lpWithInactiveCurrencyUUID = UUID.randomUUID()
  val deletedLpUUID = UUID.randomUUID()

  override def initSql =
    s"""
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'AED', 'Dirhams', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('2', 'USD', 'US Dollar', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('3', 'EUR', 'EURO', now(), null, 0);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('4', 'CNY', 'default currency for China', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('5', 'CHF', 'default currency for Switzerland', now(), null, 1);
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(1, 'a57291af-c840-4ab1-bb4d-4baed930ed58', 'user01', 'pword', 'alice@gmail.com',  '2018-10-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-01 00:00:00', null, null);
       |
       |INSERT INTO providers
       |(id, user_id, service_id, name, transaction_type, icon, label, pg_institution_id,
       |utility_payment_type, utility_min_payment_amount, utility_max_payment_amount,
       |is_active, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('1', '1', null, 'Mashreq', 'txn type 1', 'icon 1', 'label 1', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now()),
       |('2', '1', null, 'other-party', 'txn type 2', 'icon 2', 'label 2', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now());
       |
       |INSERT INTO limit_profiles
       |(id, limit_type, user_type, tier, subscription, transaction_type, channel, provider_id, instrument, max_interval_amount, max_amount,
       |min_amount, max_count, interval, created_at, updated_at, uuid, created_by, updated_by, currency_id)
       |VALUES
       |(1, 'balance_based', 'individual_user', 'tier 1', 'standard', 'top-up', 'atm', '1', 'debit_card', null, 10000,
       | null, 50000, 'daily', '$now', '$now', '${lpUUID1}', 'pegbuser', 'pegbuser', 2),
       |(2, 'balance_based', 'business_user', 'tier 1', 'gold', 'p2p_domestic', 'mobile_application', null, 'debit_card', null, 5000,
       | null, 50000, 'daily', '$now', '$now', '${lpUUID2}', 'pegbuser', 'pegbuser',  2),
       |(3, 'transaction_based', 'individual_user', 'tier 2', 'platinum', 'top-up', 'atm', '1', 'debit_card', 6000, 50000,
       | 3000, 50000, 'monthly', '$now', '$now', '${lpUUID3}', 'pegbuser', 'pegbuser', 1),
       |(4, 'transaction_based', 'business_user', 'tier 1', 'standard', 'withdrawal', 'atm', '1', 'debit_card', null, 100000,
       | null, 50000, 'daily', '$now', '$now', '${lpWithInactiveCurrencyUUID}', 'pegbuser', 'pegbuser', 3);
       |
       |INSERT INTO limit_profiles
       |(id, limit_type, user_type, tier, subscription, transaction_type, channel, provider_id, instrument, max_interval_amount, max_amount,
       |min_amount, max_count, interval, created_at, updated_at, uuid, created_by, updated_by, currency_id, deleted_at)
       |VALUES
       |(5, 'transaction_based', 'business_user', 'tier 1', 'standard', 'withdrawal', 'atm', '1', 'debit_card', null, 100000,
       | null, 50000, 'daily', '$now', '$now', '${deletedLpUUID}', 'pegbuser', 'pegbuser', 1, '$now');
       |
    """.stripMargin

  val limitProfile1 = LimitProfile(
    id = 1,
    uuid = lpUUID1,
    limitType = "balance_based",
    userType = "individual_user",
    tier = "tier 1",
    subscription = "standard",
    transactionType = Some("top-up"),
    channel = Some("atm"),
    provider = Some("Mashreq"),
    instrument = Some("debit_card"),
    interval = Some("daily"),
    maxIntervalAmount = None,
    maxAmount = Some(10000),
    minAmount = None,
    maxCount = Some(50000),
    currencyCode = "USD",
    deletedAt = None,
    createdBy = "pegbuser",
    createdAt = now,
    updatedBy = Some("pegbuser"),
    updatedAt = Some(now))
  val limitProfile2 = LimitProfile(
    id = 2,
    uuid = lpUUID2,
    limitType = "balance_based",
    userType = "business_user",
    tier = "tier 1",
    subscription = "gold",
    transactionType = Some("p2p_domestic"),
    channel = Some("mobile_application"),
    provider = None,
    instrument = Some("debit_card"),
    interval = Some("daily"),
    maxIntervalAmount = None,
    maxAmount = Some(5000),
    minAmount = None,
    maxCount = Some(50000),
    currencyCode = "USD",
    deletedAt = None,
    createdBy = "pegbuser",
    createdAt = now,
    updatedBy = Some("pegbuser"),
    updatedAt = Some(now))
  val limitProfile3 = LimitProfile(
    id = 3,
    uuid = lpUUID3,
    limitType = "transaction_based",
    userType = "individual_user",
    tier = "tier 2",
    subscription = "platinum",
    transactionType = Some("top-up"),
    channel = Some("atm"),
    provider = Some("Mashreq"),
    instrument = Some("debit_card"),
    interval = Some("monthly"),
    maxIntervalAmount = Some(6000),
    maxAmount = Some(50000),
    minAmount = Some(3000),
    maxCount = Some(50000),
    currencyCode = "AED",
    deletedAt = None,
    createdBy = "pegbuser",
    createdAt = now,
    updatedBy = Some("pegbuser"),
    updatedAt = Some(now))

  "Limit profile countLimitProfileByCriteria" should {
    val dao = inject[LimitProfileDao]
    "return count of limit profiles which are not deleted and has active currencies" in {
      val criteria = LimitProfileCriteria(isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some)

      val actualResponse = dao.countLimitProfileByCriteria(criteria)
      actualResponse mustBe Right(3)
    }
    "return count of based on filter" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        limitType = CriteriaField(s"${LimitProfileSqlDao.cLimitType}", "balance_based").some)
      val actualResponse = dao.countLimitProfileByCriteria(criteria)
      actualResponse mustBe Right(2)
    }
  }

  "Limit profile getLimitProfileByCriteria" should {
    val dao = inject[LimitProfileDao]
    "return list of profiles without filter except no active currency" in {
      val criteria = LimitProfileCriteria(isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some)
      val orderingSet = Some(OrderingSet(Ordering("currency_code", Ordering.ASC), Ordering("max_amount", Ordering.DESC)))

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile3, limitProfile1, limitProfile2))
    }
    "return list of profiles filter by limit_type" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        limitType = CriteriaField(s"${LimitProfileSqlDao.cLimitType}", "balance_based").some)
      val orderingSet = Some(OrderingSet(Ordering("user_type", Ordering.ASC), Ordering("max_interval_amount", Ordering.ASC)))

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile2, limitProfile1))
    }
    "return list of profiles filter by user_type" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        userType = CriteriaField(s"${LimitProfileSqlDao.cUserType}", "individual_user").some)
      val orderingSet = Some(OrderingSet(Ordering("interval", Ordering.DESC), Ordering("transaction_type", Ordering.ASC)))

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile3, limitProfile1))
    }
    "return list of profiles filter by tier2" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        tier = CriteriaField(s"${LimitProfileSqlDao.cTier}", "tier 2").some)
      val orderingSet = None

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile3))
    }
    "return list of profiles filter by subscription" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        subscription = CriteriaField(s"${LimitProfileSqlDao.cSubscription}", "gold").some)
      val orderingSet = None

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile2))
    }
    "return list of profiles filter by transactionType" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        transactionType = CriteriaField(s"${LimitProfileSqlDao.cTransactionType}", "top-up").some)
      val orderingSet = Some(OrderingSet(Ordering("subscription", Ordering.DESC), Ordering("min_amount", Ordering.ASC)))

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile1, limitProfile3))
    }
    "return list of profiles filter by channel" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        channel = CriteriaField(s"${LimitProfileSqlDao.cChannel}", "mobile_application").some)
      val orderingSet = Some(OrderingSet(Ordering("subscription", Ordering.DESC), Ordering("min_amount", Ordering.ASC)))

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile2))
    }
    "return list of profiles filter by provider" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        provider = CriteriaField("provider", "Mash", MatchTypes.Partial).some)
      val orderingSet = Some(OrderingSet(Ordering("tier", Ordering.ASC), Ordering("min_amount", Ordering.ASC)))

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile1, limitProfile3))
    }
    "return list of profiles filter by instrument" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        instrument = CriteriaField(s"${LimitProfileSqlDao.cInstrument}", "debit_card").some)
      val orderingSet = Some(OrderingSet(Ordering("channel", Ordering.ASC), Ordering("limit_type", Ordering.DESC)))

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile3, limitProfile1, limitProfile2))
    }
    "return list of profiles filter by interval" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        interval = CriteriaField(s"${LimitProfileSqlDao.cInterval}", "daily").some)
      val orderingSet = Some(OrderingSet(Ordering("channel", Ordering.ASC), Ordering("limit_type", Ordering.DESC)))

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile1, limitProfile2))
    }

    "return list of profiles filter by currency_code" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        currencyCode = CriteriaField(s"${LimitProfileSqlDao.cCurrencyName}", "AED").some)
      val orderingSet = Some(OrderingSet(Ordering("channel", Ordering.ASC), Ordering("limit_type", Ordering.DESC)))

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile3))
    }

    "return list of profiles multiple filters" in {
      val criteria = LimitProfileCriteria(
        isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some,
        currencyCode = CriteriaField(s"${LimitProfileSqlDao.cCurrencyName}", "USD").some,
        provider = Option(CriteriaField("provider", "Mash", MatchTypes.Partial)))

      val orderingSet = Some(OrderingSet(Ordering("channel", Ordering.ASC), Ordering("limit_type", Ordering.DESC)))

      val actualResponse = dao.getLimitProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(limitProfile1))
    }

  }

  "Limit profile dao" should {
    val dao = inject[LimitProfileDao]
    val historyDao = inject[LimitProfileHistoryDao]

    val limitType = LimitType.TransactionBased.underlying
    val someUserType = Some("user-type")
    val someTier = Some("tier")
    val someSubscription = Some("standard")
    val someTransactionType = Some("currency-exchange")
    val someChannel = Some("channel")
    val someOtherParty = Some("other-party")
    val someInstrument = Some("instrument")
    val someMaxIntervalAmount = Some(BigDecimal(900))
    val someMaxAmount = Some(BigDecimal(700))
    val someMinAmount = Some(BigDecimal(0.1))
    val someMaxCount = Some(5)
    val someInterval = Some("day")
    val currencyId = 1
    val cBy = "unit-test"
    val cAt = LocalDateTime.of(2019, 8, 8, 0, 0, 0)

    "create limit profile" in {
      val dto = LimitProfileToInsert(
        limitType = limitType,
        userType = someUserType,
        tier = someTier,
        subscription = someSubscription,
        transactionType = someTransactionType,
        channel = someChannel,
        provider = someOtherParty,
        instrument = someInstrument,
        maxIntervalAmount = someMaxIntervalAmount,
        maxAmount = someMaxAmount,
        minAmount = someMinAmount,
        maxCount = someMaxCount,
        interval = someInterval,
        currencyId = currencyId,
        createdBy = cBy,
        createdAt = cAt)
      val resp = dao.insertLimitProfile(dto)
      resp.foreach { p ⇒
        id = p.id
        uuid = p.uuid
      }
      resp.map(_.limitType) mustBe Right(limitType)
      resp.map(_.userType) mustBe Right(someUserType.get)
      resp.map(_.tier) mustBe Right(someTier.get)
      resp.map(_.subscription) mustBe Right(someSubscription.get)
      resp.map(_.transactionType) mustBe Right(someTransactionType)
      resp.map(_.channel) mustBe Right(someChannel)
      resp.map(_.provider) mustBe Right(someOtherParty)
      resp.map(_.instrument) mustBe Right(someInstrument)
      resp.map(_.maxIntervalAmount) mustBe Right(someMaxIntervalAmount)
      resp.map(_.maxAmount) mustBe Right(someMaxAmount)
      resp.map(_.minAmount) mustBe Right(someMinAmount)
      resp.map(_.maxCount) mustBe Right(someMaxCount)
      resp.map(_.createdBy) mustBe Right(cBy)
      resp.map(_.createdAt) mustBe Right(cAt)
      resp.map(_.updatedBy) mustBe Right(Some(cBy))
      resp.map(_.updatedAt) mustBe Right(Some(cAt))
    }

    "update existing limit profile" in {
      val someUpdatedMaxAmount = Some(BigDecimal(80))
      val uBy = "update-unit-test"
      val uAt = now
      val dto = LimitProfileToUpdate(
        maxAmount = someUpdatedMaxAmount,
        updatedBy = uBy,
        updatedAt = uAt,
        lastUpdatedAt = Some(cAt))
      val resp = dao.updateLimitProfile(uuid.asEntityId, dto)
      resp.map(_.flatMap(_.maxAmount)) mustBe Right(someUpdatedMaxAmount)
      resp.map(_.flatMap(_.updatedBy)) mustBe Right(Some(uBy))
      resp.map(_.flatMap(_.updatedAt)) mustBe Right(Some(uAt))
      val historyResp = historyDao.fetchHistoryForProfile(id)
      historyResp.map(_.lastOption.flatMap(_.oldMaxAmount)) mustBe Right(someMaxAmount)
      historyResp.map(_.lastOption.flatMap(_.newMaxAmount)) mustBe Right(someUpdatedMaxAmount)
    }

    "fail update for stale data" in {
      val someUpdatedMaxAmount = Some(BigDecimal(100))
      val uBy = "update-unit-test"
      val uAt = now
      val dto = LimitProfileToUpdate(
        maxAmount = someUpdatedMaxAmount,
        updatedBy = uBy,
        updatedAt = uAt,
        lastUpdatedAt = Some(cAt))
      val resp = dao.updateLimitProfile(uuid.asEntityId, dto)
      resp mustBe Left(PreconditionFailed(s"Update failed. Limit profile $uuid has been modified by another process."))
    }

    "fail delete for stale data" in {
      val uBy = "delete-unit-test"
      val uAt = Utils.nowAsLocal()
      val someDeletedAt = Some(uAt)
      val dto = LimitProfileToUpdate(
        deletedAt = someDeletedAt,
        updatedBy = uBy,
        updatedAt = uAt,
        lastUpdatedAt = Some(uAt))
      val entityId = uuid.asEntityId
      val resp = dao.updateLimitProfile(entityId, dto)
      resp mustBe Left(PreconditionFailed(s"Update failed. Limit profile $uuid has been modified by another process."))
    }

    "delete existing limit profile" in {
      val uBy = "delete-unit-test"
      val uAt = LocalDateTime.of(2019, 8, 8, 0, 0, 0)
      val someDeletedAt = Some(uAt)
      val dto = LimitProfileToUpdate(
        deletedAt = someDeletedAt,
        updatedBy = uBy,
        updatedAt = uAt,
        lastUpdatedAt = Option(now))
      val entityId = uuid.asEntityId
      val resp = dao.updateLimitProfile(entityId, dto)
      resp.map(_.flatMap(_.updatedBy)) mustBe Right(Some(uBy))
      resp.map(_.flatMap(_.updatedAt)) mustBe Right(Some(uAt))
      val historyResp = historyDao.fetchHistoryForProfile(id)
      historyResp.map(_.lastOption.map(_.updatedBy)) mustBe Right(Some(uBy))
      val findResp = dao.getLimitProfile(entityId)
      findResp mustBe Right(Option.empty[LimitProfile])
    }

  }

}
