package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import tech.pegb.backoffice.dao.Dao.{IntEntityId, UUIDEntityId}
import tech.pegb.backoffice.dao.DaoError.PreconditionFailed
import tech.pegb.backoffice.dao.fee.abstraction.FeeProfileDao
import tech.pegb.backoffice.dao.fee.dto.{FeeProfileCriteria, FeeProfileRangeToInsert, FeeProfileToUpdate}
import tech.pegb.backoffice.dao.fee.entity.{FeeProfile, FeeProfileRange}
import tech.pegb.backoffice.dao.fee.sql.FeeProfileSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering, OrderingSet}
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.util.{AppConfig, Utils}
import tech.pegb.core.PegBTestApp

class FeeProfileSqlDaoSpec extends PegBTestApp {

  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val fpUuid1 = UUID.randomUUID()
  val fpUuid2 = UUID.randomUUID()
  val fpUuid3 = UUID.randomUUID()
  val fpUuidNoCurrency = UUID.randomUUID()
  val fpUuidDeleted = UUID.randomUUID()

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
       |
       |INSERT INTO fee_profiles
       |(id, uuid, fee_type, user_type, tier, subscription_type, transaction_type, channel, provider_id, instrument, calculation_method, max_fee, min_fee,
       |fee_amount, fee_ratio, fee_method, tax_included, created_at, created_by, updated_at, updated_by, currency_id) VALUES
       |(1, '$fpUuid1', 'transaction_based', 'individual_user', 'basic', 'standard', 'p2p_domestic', 'mobile_application', null, 'visa_debit', 'flat_fee', null, null,
       |'20.00', null, 'add', 'tax_included', '$now', 'pegbuser', '$now', 'george', 1),
       |(2, '$fpUuid2', 'subscription_based', 'individual_user', 'basic', 'platinum', 'p2p_international', 'atm', '1', 'visa_debit', 'flat_fee', null, null,
       |'30.00', null, 'deduct', 'no_tax', '$now', 'pegbuser', '$now', 'george', 1),
       |(3, '$fpUuid3', 'transaction_based', 'business_user', 'small', 'gold', 'p2p_domestic', 'mobile_application', null, 'visa_debit', 'staircase_flat_percentage', '10.00', '5.00',
       |null, null, 'add', 'tax_not_included', '$now', 'pegbuser', '$now', 'george', 2),
       |(4, '$fpUuidNoCurrency', 'transaction_based', 'individual_user', 'basic', 'standard', 'p2p_domestic', 'mobile_application', null, 'visa_debit', 'flat_fee', null, null,
       |'20.00', null, 'add', '1', '$now', 'pegbuser', '$now', 'george', 3);
       |
       |INSERT INTO fee_profiles
       |(id, uuid, fee_type, user_type, tier, subscription_type, transaction_type, channel, provider_id, instrument, calculation_method, max_fee, min_fee,
       |fee_amount, fee_ratio, fee_method, tax_included, created_at, created_by, updated_at, updated_by, currency_id, deleted_at) VALUES
       |(5, '$fpUuidDeleted', 'transaction_based', 'individual_user', 'basic', 'standard', 'p2p_domestic', 'mobile_application', null, 'visa_debit', 'flat_fee', null, null,
       |'20.00', null, 'add', 'tax_included', '$now', 'pegbuser', '$now', 'george', 1, '$now');
       |
       |INSERT INTO fee_profile_ranges
       |(id, fee_profile_id, min, max, fee_amount, fee_ratio, created_at, updated_at)
       |VALUES
       |(1, 3, 0, 1000, null, 0.10, '$now', '$now'),
       |(2, 3, 1001, 5000, null, 0.05, '$now', '$now'),
       |(3, 3, 5001, 10000, null, 0.03, '$now', '$now');
    """.stripMargin

  private val dao = inject[FeeProfileDao]

  "FeeProfileSqlDao countFeeProfileByCriteria" should {
    "return count of fee profiles which has active currencies" in {
      val criteria = FeeProfileCriteria(isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some)

      val actualResponse = dao.countFeeProfileByCriteria(criteria)
      actualResponse mustBe Right(3)
    }
    "return count of based on filter" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        feeType = CriteriaField(FeeProfileSqlDao.cFeeType, "transaction_based").some)
      val actualResponse = dao.countFeeProfileByCriteria(criteria)
      actualResponse mustBe Right(2)
    }
  }

  val fp1 = FeeProfile(
    id = 1,
    uuid = fpUuid1,
    feeType = "transaction_based",
    userType = "individual_user",
    tier = "basic",
    subscription = "standard",
    transactionType = "p2p_domestic",
    channel = Some("mobile_application"),
    provider = None,
    instrument = Some("visa_debit"),
    calculationMethod = "flat_fee",
    maxFee = None,
    minFee = None,
    feeAmount = Some(BigDecimal(20)),
    feeRatio = None,
    feeMethod = "add",
    taxIncluded = "tax_included",
    ranges = None,
    currencyCode = "AED",
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = Some(now),
    updatedBy = Some("george"),
    deletedAt = None)

  val fp2 = FeeProfile(
    id = 2,
    uuid = fpUuid2,
    feeType = "subscription_based",
    userType = "individual_user",
    tier = "basic",
    subscription = "platinum",
    transactionType = "p2p_international",
    channel = Some("atm"),
    provider = Some("Mashreq"),
    instrument = Some("visa_debit"),
    calculationMethod = "flat_fee",
    maxFee = None,
    minFee = None,
    feeAmount = Some(BigDecimal(30)),
    feeRatio = None,
    feeMethod = "deduct",
    taxIncluded = "no_tax",
    ranges = None,
    currencyCode = "AED",
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = Some(now),
    updatedBy = Some("george"),
    deletedAt = None)

  val fp3 = FeeProfile(
    id = 3,
    uuid = fpUuid3,
    feeType = "transaction_based",
    userType = "business_user",
    tier = "small",
    subscription = "gold",
    transactionType = "p2p_domestic",
    channel = Some("mobile_application"),
    provider = None,
    instrument = Some("visa_debit"),
    calculationMethod = s"${config.StaircaseFlatPercentage}",
    maxFee = Some(BigDecimal(10)),
    minFee = Some(BigDecimal(5)),
    feeAmount = None,
    feeRatio = None,
    feeMethod = "add",
    taxIncluded = "tax_not_included",
    ranges = Some(Seq(
      FeeProfileRange(1, Some(3), Some(BigDecimal(1000)), Some(BigDecimal(0)), None, Some(BigDecimal(0.10))),
      FeeProfileRange(2, Some(3), Some(BigDecimal(5000)), Some(BigDecimal(1001)), None, Some(BigDecimal(0.05))),
      FeeProfileRange(3, Some(3), Some(BigDecimal(10000)), Some(BigDecimal(5001)), None, Some(BigDecimal(0.03))))),
    currencyCode = "USD",
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = Some(now),
    updatedBy = Some("george"),
    deletedAt = None)

  "FeeProfileSqlDao getLimitProfileByCriteria" should {
    val cutFee1 = fp1.copy(ranges = None)
    val cutFee2 = fp2.copy(ranges = None)
    val cutFee3 = fp3.copy(ranges = None)

    "return list of profiles without filter except no active currency" in {
      val criteria = FeeProfileCriteria(isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some)
      val orderingSet = Some(OrderingSet(Ordering("fee_type", Ordering.ASC), Ordering("user_type", Ordering.DESC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee2, cutFee1, cutFee3))
    }

    "return list of profiles filter by fee_type" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        feeType = CriteriaField(FeeProfileSqlDao.cFeeType, "transaction_based").some)
      val orderingSet = Some(OrderingSet(Ordering("subscription_type", Ordering.ASC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee3, cutFee1))
    }

    "return list of profiles filter by user_type" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        userType = CriteriaField(FeeProfileSqlDao.cUserType, "individual_user").some)
      val orderingSet = Some(OrderingSet(Ordering("transaction_type", Ordering.DESC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee2, cutFee1))
    }

    "return list of profiles filter by tier" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        tier = CriteriaField(FeeProfileSqlDao.cTier, "small").some)
      val orderingSet = Some(OrderingSet(Ordering("transaction_type", Ordering.DESC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee3))
    }

    "return list of profiles filter by transaction_type" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        transactionType = CriteriaField(FeeProfileSqlDao.cTransactionType, "p2p_international").some)

      val actualResponse = dao.getFeeProfileByCriteria(criteria, None, Some(1), None)
      actualResponse mustBe Right(Seq(cutFee2))
    }

    "return list of profiles filter by channel" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        channel = CriteriaField(FeeProfileSqlDao.cChannel, "atm").some)
      val orderingSet = Some(OrderingSet(Ordering("other_party", Ordering.DESC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee2))
    }

    "return list of profiles filter by other_party (exact)" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        provider = CriteriaField(Provider.cName, "Mashreq").some)
      val orderingSet = Some(OrderingSet(Ordering("other_party", Ordering.DESC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee2))
    }

    "return list of profiles filter by other_party (partial)" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        provider = Some(CriteriaField(Provider.cName, "Mas", MatchTypes.Partial)))
      val orderingSet = Some(OrderingSet(Ordering("other_party", Ordering.DESC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee2))
    }

    "return list of profiles filter by instrument" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        instrument = CriteriaField(FeeProfileSqlDao.cInstrument, "visa_debit").some)
      val orderingSet = Some(OrderingSet(Ordering("calculation_method", Ordering.DESC), Ordering("fee_amount", Ordering.ASC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee3, cutFee1, cutFee2))
    }

    "return list of profiles filter by calculationMethod" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        calculationMethod = CriteriaField(FeeProfileSqlDao.cCalculationMethod, s"${config.StaircaseFlatPercentage}").some)
      val orderingSet = Some(OrderingSet(Ordering("max_fee", Ordering.DESC), Ordering("min_fee", Ordering.ASC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee3))
    }

    "return list of profiles filter by feeMethod" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        feeMethod = CriteriaField(FeeProfileSqlDao.cFeeMethod, "add").some)
      val orderingSet = Some(OrderingSet(Ordering("currency_code", Ordering.DESC), Ordering("min_fee", Ordering.ASC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee3, cutFee1))
    }

    "return list of profiles filter by taxIncluded" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        taxIncluded = CriteriaField(FeeProfileSqlDao.cTaxIncluded, "tax_not_included").some)
      val orderingSet = Some(OrderingSet(Ordering("fee_method", Ordering.DESC), Ordering("min_fee", Ordering.ASC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee3))
    }

    "return list of profiles filter by no_tax" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        taxIncluded = CriteriaField(FeeProfileSqlDao.cTaxIncluded, "no_tax").some)
      val orderingSet = Some(OrderingSet(Ordering("fee_method", Ordering.DESC), Ordering("min_fee", Ordering.ASC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee2))
    }

    "return list of profiles filter by currencyCode" in {
      val criteria = FeeProfileCriteria(
        isDeleted = CriteriaField(FeeProfileSqlDao.cDeletedAt, false).some,
        currencyCode = CriteriaField(FeeProfileSqlDao.cCurrencyName, "AED").some)
      val orderingSet = Some(OrderingSet(Ordering("fee_type", Ordering.ASC), Ordering("min_fee", Ordering.ASC)))

      val actualResponse = dao.getFeeProfileByCriteria(criteria, orderingSet, None, None)
      actualResponse mustBe Right(Seq(cutFee2, cutFee1))
    }
  }

  "FeeProfileSqlDao getFeeProfile" should {
    "return matching profile by UUID" in {
      val actualResponse = dao.getFeeProfile(UUIDEntityId(fpUuid1))
      actualResponse mustBe Right(Some(fp1))
    }

    "return matching profile by ID" in {
      val actualResponse = dao.getFeeProfile(IntEntityId(2))
      actualResponse mustBe Right(Some(fp2))
    }

    "return matching NONE if not found (UUID)" in {
      val actualResponse = dao.getFeeProfile(UUIDEntityId(UUID.randomUUID()))
      actualResponse mustBe Right(None)
    }

    "return matching NONE if not found (ID)" in {
      val actualResponse = dao.getFeeProfile(IntEntityId(1000))
      actualResponse mustBe Right(None)
    }

    "return matching NONE if isDeleted (UUID)" in {
      val actualResponse = dao.getFeeProfile(UUIDEntityId(fpUuidDeleted))
      actualResponse mustBe Right(None)
    }

    "return matching NONE if isDeleted (ID)" in {
      val actualResponse = dao.getFeeProfile(IntEntityId(5))
      actualResponse mustBe Right(None)
    }

  }

  "FeeProfileSqlDao getFeeProfileRangesByFeeProfileId" should {
    val r1 = FeeProfileRange(
      id = 1,
      feeProfileId = Some(3),
      max = Some(BigDecimal(1000)),
      min = Some(BigDecimal(0)),
      feeAmount = None,
      feeRatio = Some(BigDecimal(0.1)))
    val r2 = FeeProfileRange(
      id = 2,
      feeProfileId = Some(3),
      max = Some(BigDecimal(5000)),
      min = Some(BigDecimal(1001)),
      feeAmount = None,
      feeRatio = Some(BigDecimal(0.05)))
    val r3 = FeeProfileRange(
      id = 3,
      feeProfileId = Some(3),
      max = Some(BigDecimal(10000)),
      min = Some(BigDecimal(5001)),
      feeAmount = None,
      feeRatio = Some(BigDecimal(0.03)))
    "return ranges of profile matching UUID" in {
      val actualResponse = dao.getFeeProfileRangesByFeeProfileId(UUIDEntityId(fpUuid3))
      actualResponse mustBe Right(Seq(r1, r2, r3))
    }
    "return ranges of profile matching ID" in {
      val actualResponse = dao.getFeeProfileRangesByFeeProfileId(IntEntityId(3))
      actualResponse mustBe Right(Seq(r1, r2, r3))
    }
    "return empty list of ranges of profile matching UUID notFound" in {
      val actualResponse = dao.getFeeProfileRangesByFeeProfileId(UUIDEntityId(UUID.randomUUID()))
      actualResponse mustBe Right(Nil)
    }
    "return empty list of ranges of profile matching ID notfound" in {
      val actualResponse = dao.getFeeProfileRangesByFeeProfileId(IntEntityId(10000))
      actualResponse mustBe Right(Nil)
    }
  }

  "FeeProfileSqlDao update fee profile" should {
    val newUpdateTime = LocalDateTime.now()
    val baseDto = FeeProfileToUpdate(
      calculationMethod = "staircase_flat_fee",
      feeMethod = "add",
      taxIncluded = "tax_included",
      ranges = None,
      updatedAt = newUpdateTime,
      updatedBy = "unit-test",
      lastUpdatedAt = Some(now))
    val minFee = BigDecimal("10.00")

    "when only fee fields are changed" in {
      val resp = dao.updateFeeProfile(UUIDEntityId(fpUuid1), baseDto.copy(minFee = Some(minFee)))
      resp.map(_.flatMap(_.minFee)) mustBe Right(Some(minFee))
    }

    "when ranges are changed" in {
      val feeId = Some(3)
      val range1 = FeeProfileRangeToInsert(
        feeProfileId = feeId,
        max = Some(BigDecimal(99.99)),
        min = BigDecimal(5),
        feeAmount = None,
        feeRatio = Some(BigDecimal(1)))
      val range2 = FeeProfileRangeToInsert(
        feeProfileId = feeId,
        max = Some(BigDecimal(200)),
        min = BigDecimal(100),
        feeAmount = None,
        feeRatio = Some(BigDecimal(2)))
      val resp = dao.updateFeeProfile(UUIDEntityId(fpUuid3), baseDto.copy(ranges = Some(Seq(range1, range2))))
      resp.map(_.flatMap(_.ranges.map(_.size))) mustBe Right(Some(2))
    }

    "fail update when last_updated_at is incorrect (precondition fail)" in {
      val resp = dao.updateFeeProfile(UUIDEntityId(fpUuid1), baseDto.copy(minFee = Some(minFee)))
      resp.map(_.flatMap(_.minFee)) mustBe Left(PreconditionFailed(s"Update failed. Fee profile $fpUuid1 has been modified by another process."))
    }

    "fail delete when last_updated_at is incorrect (precondition fail)" in {
      val resp = dao.updateFeeProfile(UUIDEntityId(fpUuid1), baseDto.copy(deletedAt = Some(Utils.nowAsLocal())))
      resp.map(_.flatMap(_.minFee)) mustBe Left(PreconditionFailed(s"Update failed. Fee profile $fpUuid1 has been modified by another process."))
    }

    "when deletedAt is specified" in {
      val resp = dao.updateFeeProfile(UUIDEntityId(fpUuid1), baseDto.copy(lastUpdatedAt = Some(newUpdateTime), deletedAt = Some(Utils.nowAsLocal())))
      resp.map(_.flatMap(_.minFee)) mustBe Right(Some(minFee))
    }
  }
}
