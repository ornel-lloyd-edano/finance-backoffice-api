package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import cats.implicits._
import tech.pegb.backoffice.dao.fee.abstraction.ThirdPartyFeeProfileDao
import tech.pegb.backoffice.dao.fee.dto.{ThirdPartyFeeProfileCriteria, ThirdPartyFeeProfileRangeToInsert, ThirdPartyFeeProfileToInsert}
import tech.pegb.backoffice.dao.fee.entity.{ThirdPartyFeeProfile, ThirdPartyFeeProfileRange}
import tech.pegb.backoffice.dao.fee.sql.ThirdPartyFeeProfileSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering, OrderingSet}
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.core.PegBTestApp

class ThirdPartyFeeProfileSqlDaoSpec extends PegBTestApp {

  val config: AppConfig = inject[AppConfig]

  val mockClock: Clock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now: LocalDateTime = LocalDateTime.now(mockClock)

  override def initSql: String =
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
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(1, 'a57291af-c840-4ab1-bb4d-4baed930ed58', 'user01', 'pword', 'alice@gmail.com',  '2018-10-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-01 00:00:00', null, null);
       |
       |INSERT INTO providers
       |(id, user_id, service_id, name, transaction_type, icon, label, pg_institution_id,
       |utility_payment_type, utility_min_payment_amount, utility_max_payment_amount,
       |is_active, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('1', '1', null, 'Mashreq', 'txn type 1', 'icon 1', 'label 1', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now()),
       |('2', '1', null, 'Lloyd Bank', 'txn type 2', 'icon 2', 'label 2', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now()),
       |('3', '1', null, 'Citi Bank', 'txn type 3', 'icon 3', 'label 3', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now()),
       |('4', '1', null, 'Emirates NBD', 'txn type 4', 'icon 4', 'label 4', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now());
       |
       |
       |INSERT INTO third_party_fee_profiles
       |(id, transaction_type, provider_id, calculation_method, max_fee, min_fee,
       |fee_amount, fee_ratio, created_at, created_by, updated_at, updated_by, currency_id, is_active) VALUES
       |(1, 'p2p_domestic', '1','flat_fee', null, null,
       |'20.00', null, '$now', 'pegbuser', '$now', 'george', 1, 1),
       |(2, 'p2p_international', '2', 'flat_fee', null, null,
       |'30.00', null, '$now', 'pegbuser', '$now', 'george', 1, 1),
       |(3,  'p2p_domestic', '3', 'staircase_flat_percentage', '10.00', '5.00',
       |null, null, '$now', 'pegbuser', '$now', 'george', 2, 1),
       |(4, 'p2p_domestic',  '2', 'flat_fee', null, null,
       |'20.00', null, '$now', 'pegbuser', '$now', 'george', 3, 1),
       |(5, 'p2p_outer_space',  '1', 'flat_fee', null, null,
       |'20.00', null, '$now', 'pegbuser', '$now', 'george', 3, 0);
       |
       |INSERT INTO third_party_fee_profile_ranges
       |(id, third_party_fee_profile_id, min, max, fee_amount, fee_ratio, created_at, updated_at)
       |VALUES
       |(1, 3, 0, 1000, null, 0.10, '$now', '$now'),
       |(2, 3, 1001, 5000, null, 0.05, '$now', '$now'),
       |(3, 3, 5001, 10000, null, 0.03, '$now', '$now');
    """.stripMargin

  private val dao = inject[ThirdPartyFeeProfileDao]

  private val fp1: ThirdPartyFeeProfile = ThirdPartyFeeProfile(
    id = "1",
    transactionType = Some("p2p_domestic"),
    provider = "Mashreq",
    calculationMethod = "flat_fee",
    maxFee = None,
    minFee = None,
    feeAmount = Some(BigDecimal(20)),
    feeRatio = None,
    ranges = None,
    currencyCode = "AED",
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = Some(now),
    updatedBy = Some("george"),
    isActive = true)

  private val fp2: ThirdPartyFeeProfile = ThirdPartyFeeProfile(
    id = "2",
    transactionType = Some("p2p_international"),
    provider = "Lloyd Bank",
    calculationMethod = "flat_fee",
    maxFee = None,
    minFee = None,
    feeAmount = Some(BigDecimal(30)),
    feeRatio = None,
    ranges = None,
    currencyCode = "AED",
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = Some(now),
    updatedBy = Some("george"),
    isActive = true)

  private val fp3 = ThirdPartyFeeProfile(
    id = "3",
    transactionType = Some("p2p_domestic"),
    provider = "Citi Bank",
    calculationMethod = s"${config.StaircaseFlatPercentage}",
    maxFee = Some(BigDecimal(10)),
    minFee = Some(BigDecimal(5)),
    feeAmount = None,
    feeRatio = None,
    ranges = Some(Seq(
      ThirdPartyFeeProfileRange("1", "3", Some(BigDecimal(1000)), Some(BigDecimal(0)), None, Some(BigDecimal(0.10))),
      ThirdPartyFeeProfileRange("2", "3", Some(BigDecimal(5000)), Some(BigDecimal(1001)), None, Some(BigDecimal(0.05))),
      ThirdPartyFeeProfileRange("3", "3", Some(BigDecimal(10000)), Some(BigDecimal(5001)), None, Some(BigDecimal(0.03))))),
    currencyCode = "USD",
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = Some(now),
    updatedBy = Some("george"),
    isActive = true)

  "FeeProfileSqlDao getLimitProfileByCriteria" should {
    val cutFee1 = fp1.copy(ranges = None)
    val cutFee2 = fp2.copy(ranges = None)
    val cutFee3 = fp3.copy(ranges = None)

    "return list of profiles without filter except no active currency" in {
      val criteria = ThirdPartyFeeProfileCriteria(isActive = Some(CriteriaField(ThirdPartyFeeProfileSqlDao.cIsActive, true)))
      val orderingSet = Some(OrderingSet(Ordering("transaction_type", Ordering.ASC)))

      val actualResponse = dao.getThirdPartyFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse.map(_.toSet) mustBe Right(Set(cutFee2, cutFee1, cutFee3))
    }

    "return list of profiles filter by transaction_type" in {
      val criteria = ThirdPartyFeeProfileCriteria(
        isActive = Some(CriteriaField(ThirdPartyFeeProfileSqlDao.cIsActive, true)),
        transactionType = CriteriaField(ThirdPartyFeeProfileSqlDao.cTransactionType, "p2p_international").some)

      val actualResponse = dao.getThirdPartyFeeProfileByCriteria(criteria, None, Some(1), None)
      actualResponse mustBe Right(Seq(cutFee2))
    }

    "return list of profiles filter by provider (exact)" in {
      val criteria = ThirdPartyFeeProfileCriteria(
        isActive = Some(CriteriaField(ThirdPartyFeeProfileSqlDao.cIsActive, true)),
        provider = Some(CriteriaField("providers.name", "Mashreq")))
      val orderingSet = Some(OrderingSet(Ordering("providers.name", Ordering.DESC)))

      val actualResponse = dao.getThirdPartyFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee1))
    }

    "return list of profiles filter by other_party (partial)" in {
      val criteria = ThirdPartyFeeProfileCriteria(
        isActive = Some(CriteriaField(ThirdPartyFeeProfileSqlDao.cIsActive, true)),
        provider = Some(CriteriaField("providers.name", "Mas", MatchTypes.Partial)))
      val orderingSet = Some(OrderingSet(Ordering("providers.name", Ordering.DESC)))

      val actualResponse = dao.getThirdPartyFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee1))
    }

    "return list of profiles filter by currencyCode" in {
      val criteria = ThirdPartyFeeProfileCriteria(
        isActive = Some(CriteriaField(ThirdPartyFeeProfileSqlDao.cIsActive, true)),
        currencyCode = CriteriaField(ThirdPartyFeeProfileSqlDao.cCurrencyName, "AED").some)
      val orderingSet = Some(OrderingSet(Ordering("currency_id", Ordering.ASC)))

      val actualResponse = dao.getThirdPartyFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse.map(_.toSet) mustBe Right(Set(cutFee2, cutFee1, cutFee3))
    }
  }

  "FeeProfileSqlDao getFeeProfile" should {

    "return matching profile by ID" in {
      val actualResponse = dao.getThirdPartyFeeProfile("2")
      actualResponse mustBe Right(Some(fp2))
    }

    "return matching NONE if not found (ID)" in {
      val actualResponse = dao.getThirdPartyFeeProfile("1000")
      actualResponse mustBe Right(None)
    }

    "return matching NONE if deactivated (ID)" in {
      val actualResponse = dao.getThirdPartyFeeProfile("5")
      actualResponse mustBe Right(None)
    }

  }

  "ThirdPartyFeeProfileSqlDao getThirdPartyFeeProfileRangesByProfileId" should {
    val r1 = ThirdPartyFeeProfileRange(
      id = "1",
      thirdPartyFeeProfileId = "3",
      max = Some(BigDecimal(1000)),
      min = Some(BigDecimal(0)),
      feeAmount = None,
      feeRatio = Some(BigDecimal(0.1)))
    val r2 = ThirdPartyFeeProfileRange(
      id = "2",
      thirdPartyFeeProfileId = "3",
      max = Some(BigDecimal(5000)),
      min = Some(BigDecimal(1001)),
      feeAmount = None,
      feeRatio = Some(BigDecimal(0.05)))
    val r3 = ThirdPartyFeeProfileRange(
      id = "3",
      thirdPartyFeeProfileId = "3",
      max = Some(BigDecimal(10000)),
      min = Some(BigDecimal(5001)),
      feeAmount = None,
      feeRatio = Some(BigDecimal(0.03)))

    "return ranges of profile matching ID" in {
      val actualResponse = dao.getThirdPartyFeeProfileRangesByFeeProfileId("3")
      actualResponse mustBe Right(Seq(r1, r2, r3))
    }

    "return empty list of ranges of profile matching ID notfound" in {
      val actualResponse = dao.getThirdPartyFeeProfileRangesByFeeProfileId("10000")
      actualResponse mustBe Right(Nil)
    }
  }

  "ThirdPartyFeeProfileSqlDao create third party fee profile and range" should {
    val r1 = ThirdPartyFeeProfileRange("4", "6", Some(1000), Some(0), None, Some(0.1000))
    val r2 = ThirdPartyFeeProfileRange("5", "6", Some(5000), Some(1001), None, Some(0.0500))
    val r3 = ThirdPartyFeeProfileRange("6", "6", Some(10000), Some(5001), None, Some(0.0300))
    val tfp = ThirdPartyFeeProfile(
      id = "6",
      transactionType = Some("p2p"),
      provider = "Emirates NBD",
      calculationMethod = s"${config.StaircaseFlatPercentage}",
      maxFee = Some(BigDecimal(10)),
      minFee = Some(BigDecimal(5)),
      feeAmount = None,
      feeRatio = None,
      ranges = Some(Seq(r1, r2, r3)),
      currencyCode = "AED",
      createdAt = now,
      createdBy = "pegbuser",
      updatedAt = Some(now),
      updatedBy = Some("pegbuser"),
      isActive = true)
    val dto = ThirdPartyFeeProfileToInsert(
      transactionType = Some("p2p"),
      provider = "Emirates NBD",
      currencyId = "1",
      calculationMethod = s"${config.StaircaseFlatPercentage}",
      maxFee = Some(BigDecimal(10)),
      minFee = Some(BigDecimal(5)),
      feeAmount = None,
      feeRatio = None,
      ranges = Some(Seq(
        ThirdPartyFeeProfileRangeToInsert(Some(BigDecimal(1000)), Some(BigDecimal(0)), None, Some(BigDecimal(0.10))),
        ThirdPartyFeeProfileRangeToInsert(Some(BigDecimal(5000)), Some(BigDecimal(1001)), None, Some(BigDecimal(0.05))),
        ThirdPartyFeeProfileRangeToInsert(Some(BigDecimal(10000)), Some(BigDecimal(5001)), None, Some(BigDecimal(0.03))))),
      createdAt = now,
      createdBy = "pegbuser")

    "return created third party fee profile of profile matching ID" in {
      val actualResponse = dao.createThirdPartyFeeProfile(dto)

      actualResponse mustBe Right(tfp)
    }

  }
}

