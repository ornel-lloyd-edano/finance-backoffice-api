package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.data.NonEmptyList
import cats.implicits._
import tech.pegb.backoffice.dao.commission.dto.{CommissionProfileCriteria, CommissionProfileRangeToInsert, CommissionProfileToInsert}
import tech.pegb.backoffice.dao.commission.entity.{CommissionProfile, CommissionProfileRange}
import tech.pegb.backoffice.dao.commission.sql.CommissionProfileSqlDao
import tech.pegb.backoffice.dao.model.{Ordering, OrderingSet}
import tech.pegb.core.PegBTestApp

class CommissionProfileSqlDaoSpec extends PegBTestApp {

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val dao = inject[CommissionProfileSqlDao]

  val cp1 = 1
  val cp2 = 2
  val cp3 = 3

  val cpUUID1 = UUID.randomUUID()
  val cpUUID2 = UUID.randomUUID()
  val cpUUID3 = UUID.randomUUID()

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
       |INSERT INTO commission_profiles(
       |id, uuid, business_type, tier, subscription_type, transaction_type, currency_id, channel, instrument,
       |calculation_method, max_commission, min_commission, commission_amount, commission_ratio,
       |created_by, updated_by, created_at, updated_at)
       |VALUES
       |($cp1, '$cpUUID1', 'merchant', 'small', 'standard', 'cashin', 1, null, null,
       |'flat_percentage', 100, 10, null, 0.01,
       | 'pegbuser', 'pegbuser', '$now', '$now'),
       |($cp2, '$cpUUID2', 'super_merchant', 'big', 'standard', 'cashout', 2, 'atm', null,
       |'staircase_flat_amount', null, null, 35, null,
       | 'pegbuser', 'pegbuser', '$now', '$now');
       |
       |INSERT INTO commission_profiles(
       |id, uuid, business_type, tier, subscription_type, transaction_type, currency_id, channel, instrument,
       |calculation_method, max_commission, min_commission, commission_amount, commission_ratio,
       |created_by, updated_by, created_at, updated_at, deleted_at)
       |VALUES
       |($cp3, '$cpUUID3', 'merchant', 'small', 'standard', 'cashout', 1, null, null,
       |'staircase_flat_percentage', 100, 10, null, 0.01,
       | 'pegbuser', 'pegbuser', '$now', '$now', '$now');
       |
       |INSERT INTO commission_profile_ranges(
       |id, commission_profile_id, min, max, commission_amount, commission_ratio, created_at, updated_at)
       |VALUES
       |(1, $cp2, 0, 50, 0, 0.5, '$now', '$now'),
       |(2, $cp2, 50, 100, 0, 0.25, '$now', '$now');
     """.stripMargin

  "CommissionProfileSqlDao" should {
    val c1 = CommissionProfile(
      id = cp1,
      uuid = cpUUID1.toString,
      businessType = "merchant",
      tier = "small",
      subscriptionType = "standard",
      transactionType = "cashin",
      currencyId = 1,
      currencyCode = "AED",
      channel = None,
      instrument = None,
      calculationMethod = "flat_percentage",
      maxCommission = BigDecimal(100).some,
      minCommission = BigDecimal(10).some,
      commissionAmount = None,
      commissionRatio = BigDecimal(0.01).some,
      ranges = None,
      createdBy = "pegbuser",
      updatedBy = "pegbuser",
      createdAt = now,
      updatedAt = now,
      deletedAt = None)

    val c2Ranges = Seq(
      CommissionProfileRange(
        id = 1,
        commissionProfileId = cp2,
        min = BigDecimal(0),
        max = BigDecimal(50).some,
        commissionAmount = BigDecimal(0).some,
        commissionRatio = BigDecimal(0.5).some,
        createdAt = now,
        updatedAt = now),
      CommissionProfileRange(
        id = 2,
        commissionProfileId = cp2,
        min = BigDecimal(50),
        max = BigDecimal(100).some,
        commissionAmount = BigDecimal(0).some,
        commissionRatio = BigDecimal(0.25).some,
        createdAt = now,
        updatedAt = now)
    )
    val c2 = CommissionProfile(
      id = cp2,
      uuid = cpUUID2.toString,
      businessType = "super_merchant",
      tier = "big",
      subscriptionType = "standard",
      transactionType = "cashout",
      currencyId = 2,
      currencyCode = "USD",
      channel = "atm".some,
      instrument = None,
      calculationMethod = "staircase_flat_amount",
      maxCommission = None,
      minCommission = None,
      commissionAmount = BigDecimal(35).some,
      commissionRatio = None,
      ranges = None,
      createdBy = "pegbuser",
      updatedBy = "pegbuser",
      createdAt = now,
      updatedAt = now,
      deletedAt = None)

    val c3 = CommissionProfile(
      id = cp3,
      uuid = cpUUID3.toString,
      businessType = "merchant",
      tier = "small",
      subscriptionType = "standard",
      transactionType = "cashout",
      currencyId = 1,
      currencyCode = "AED",
      channel = None,
      instrument = None,
      calculationMethod = "staircase_flat_percentage",
      maxCommission = BigDecimal(100).some,
      minCommission = BigDecimal(10).some,
      commissionAmount = None,
      commissionRatio = BigDecimal(0.01).some,
      ranges = None,
      createdBy = "pegbuser",
      updatedBy = "pegbuser",
      createdAt = now,
      updatedAt = now,
      deletedAt = now.some)

    "return all business user application in getCommissionProfileByCriteria all" in {
      val criteria = CommissionProfileCriteria()

      val orderingSet = OrderingSet(Ordering("id", Ordering.DESC)).some
      val resp = dao.getCommissionProfileByCriteria(criteria, orderingSet, None, None)

      resp mustBe Right(Seq(c3, c2, c1))

    }

    "return business user application in getCommissionProfileByCriteria filter by transactionType" in {
      val criteria = CommissionProfileCriteria(
        transactionType = model.CriteriaField("", "cashin").some)

      val resp = dao.getCommissionProfileByCriteria(criteria, None, None, None)

      resp mustBe Right(Seq(c1))

    }

    "return business user application in getCommissionProfileByCriteria filter by currency" in {
      val criteria = CommissionProfileCriteria(
        currency = model.CriteriaField("", "AED").some)

      val orderingSet = OrderingSet(Ordering("transaction_type", Ordering.ASC)).some
      val resp = dao.getCommissionProfileByCriteria(criteria, None, None, None)

      resp mustBe Right(Seq(c1, c3))

    }

    "return business user application in getCommissionProfileByCriteria filter by id" in {
      val criteria = CommissionProfileCriteria(
        uuid = model.CriteriaField("", cpUUID1.toString).some)

      val orderingSet = OrderingSet(Ordering("transaction_type", Ordering.ASC)).some
      val resp = dao.getCommissionProfileByCriteria(criteria, None, None, None)

      resp mustBe Right(Seq(c1))

    }

    "return business user application in getCommissionProfileByCriteria filter by tier" in {
      val criteria = CommissionProfileCriteria(
        tier = model.CriteriaField("", "big".toString).some)

      val orderingSet = OrderingSet(Ordering("transaction_type", Ordering.ASC)).some
      val resp = dao.getCommissionProfileByCriteria(criteria, None, None, None)

      resp mustBe Right(Seq(c2))

    }

    "return business user application in getCommissionProfileByCriteria filter by channel" in {
      val criteria = CommissionProfileCriteria(
        channel = model.CriteriaField("", "atm".toString).some)

      val orderingSet = OrderingSet(Ordering("transaction_type", Ordering.ASC)).some
      val resp = dao.getCommissionProfileByCriteria(criteria, None, None, None)

      resp mustBe Right(Seq(c2))

    }

    "return business user application in getCommissionProfileByCriteria filter by isDeleted" in {
      val criteria = CommissionProfileCriteria(
        isDeleted = model.CriteriaField("", false).some)

      val orderingSet = OrderingSet(Ordering("transaction_type", Ordering.ASC)).some
      val resp = dao.getCommissionProfileByCriteria(criteria, None, None, None)

      resp mustBe Right(Seq(c1, c2))

    }

    "return count of all business user application in countCommissionProfileByCriteria all" in {
      val criteria = CommissionProfileCriteria()

      val resp = dao.countCommissionProfileByCriteria(criteria)

      resp mustBe Right(3)

    }

    "return count business user application in countCommissionProfileByCriteria filter by isDeleted" in {
      val criteria = CommissionProfileCriteria(
        isDeleted = model.CriteriaField("", false).some)

      val orderingSet = OrderingSet(Ordering("transaction_type", Ordering.ASC)).some
      val resp = dao.countCommissionProfileByCriteria(criteria)

      resp mustBe Right(2)

    }

    "return ranges of business user application by id" in {
      val resp = dao.getCommissionProfileRangeByCommissionId(cp2)

      resp mustBe Right(c2Ranges)

    }

  }

  "Insert commission profile" should {
    "succeed when inserting profile without ranges" in {
      val dto = CommissionProfileToInsert(
        uuid = UUID.randomUUID().toString,
        businessType = "merchant",
        tier = "medium",
        subscriptionType = "standard",
        transactionType = "cashin",
        currencyId = 1,
        channel = None,
        instrument = None,
        calculationMethod = "flat_percentage",
        maxCommission = BigDecimal(100).some,
        minCommission = BigDecimal(10).some,
        commissionAmount = None,
        commissionRatio = BigDecimal(0.01).some,
        ranges = None,
        createdBy = "kira",
        createdAt = now)

      val resp = dao.insertCommissionProfile(dto)
      resp.map(_.uuid) mustBe Right(dto.uuid)
      resp.map(_.businessType) mustBe Right(dto.businessType)
      resp.map(_.tier) mustBe Right(dto.tier)
      resp.map(_.subscriptionType) mustBe Right(dto.subscriptionType)
      resp.map(_.currencyId) mustBe Right(dto.currencyId)
      resp.map(_.currencyCode) mustBe Right("AED")
      resp.map(_.instrument) mustBe Right(dto.instrument)
      resp.map(_.calculationMethod) mustBe Right(dto.calculationMethod)
      resp.map(_.maxCommission) mustBe Right(dto.maxCommission)
      resp.map(_.minCommission) mustBe Right(dto.minCommission)
      resp.map(_.commissionAmount) mustBe Right(dto.commissionAmount)
      resp.map(_.commissionRatio) mustBe Right(dto.commissionRatio)
      resp.map(_.createdAt) mustBe Right(dto.createdAt)
      resp.map(_.createdBy) mustBe Right(dto.createdBy)
      resp.map(_.updatedAt) mustBe Right(dto.createdAt)
      resp.map(_.updatedBy) mustBe Right(dto.createdBy)
      resp.map(_.ranges) mustBe Right(None)

      val criteria = CommissionProfileCriteria(
        uuid = model.CriteriaField("", dto.uuid).some)

      val selectResp = dao.getCommissionProfileByCriteria(criteria, None, None, None)
      val head = selectResp.map(_.head)
      head.map(_.uuid) mustBe Right(dto.uuid)
      head.map(_.businessType) mustBe Right(dto.businessType)
      head.map(_.tier) mustBe Right(dto.tier)
      head.map(_.subscriptionType) mustBe Right(dto.subscriptionType)
      head.map(_.currencyId) mustBe Right(dto.currencyId)
      head.map(_.currencyCode) mustBe Right("AED")
      head.map(_.instrument) mustBe Right(dto.instrument)
      head.map(_.calculationMethod) mustBe Right(dto.calculationMethod)
      head.map(_.maxCommission) mustBe Right(dto.maxCommission)
      head.map(_.minCommission) mustBe Right(dto.minCommission)
      head.map(_.commissionAmount) mustBe Right(dto.commissionAmount)
      head.map(_.commissionRatio) mustBe Right(dto.commissionRatio)
      head.map(_.createdAt) mustBe Right(dto.createdAt)
      head.map(_.createdBy) mustBe Right(dto.createdBy)
      head.map(_.updatedAt) mustBe Right(dto.createdAt)
      head.map(_.updatedBy) mustBe Right(dto.createdBy)
      head.map(_.ranges) mustBe Right(None)
    }

    "succeed when inserting profile with ranges" in {

      val range = Seq(
        CommissionProfileRangeToInsert(
          min = BigDecimal(1).some,
          max = BigDecimal(50).some,
          commissionAmount = BigDecimal(10).some,
          commissionRatio = None,
        ),
        CommissionProfileRangeToInsert(
          min = BigDecimal(50).some,
          max = BigDecimal(100).some,
          commissionAmount = None,
          commissionRatio = BigDecimal(0.05).some,
        )
      )
      val dto = CommissionProfileToInsert(
        uuid = UUID.randomUUID().toString,
        businessType = "merchant",
        tier = "medium",
        subscriptionType = "standard",
        transactionType = "cashout",
        currencyId = 1,
        channel = None,
        instrument = None,
        calculationMethod = "staircase_flat_percentage",
        maxCommission = BigDecimal(100).some,
        minCommission = BigDecimal(10).some,
        commissionAmount = None,
        commissionRatio = BigDecimal(0.01).some,
        ranges = NonEmptyList(range.head, range.tail.toList).some,
        createdBy = "kira",
        createdAt = now)

      val resp = dao.insertCommissionProfile(dto)
      resp.map(_.uuid) mustBe Right(dto.uuid)
      resp.map(_.businessType) mustBe Right(dto.businessType)
      resp.map(_.tier) mustBe Right(dto.tier)
      resp.map(_.subscriptionType) mustBe Right(dto.subscriptionType)
      resp.map(_.currencyId) mustBe Right(dto.currencyId)
      resp.map(_.currencyCode) mustBe Right("AED")
      resp.map(_.instrument) mustBe Right(dto.instrument)
      resp.map(_.calculationMethod) mustBe Right(dto.calculationMethod)
      resp.map(_.maxCommission) mustBe Right(dto.maxCommission)
      resp.map(_.minCommission) mustBe Right(dto.minCommission)
      resp.map(_.commissionAmount) mustBe Right(dto.commissionAmount)
      resp.map(_.commissionRatio) mustBe Right(dto.commissionRatio)
      resp.map(_.createdAt) mustBe Right(dto.createdAt)
      resp.map(_.createdBy) mustBe Right(dto.createdBy)
      resp.map(_.updatedAt) mustBe Right(dto.createdAt)
      resp.map(_.updatedBy) mustBe Right(dto.createdBy)
      resp.map(_.ranges) mustBe Right(None)

      val criteria = CommissionProfileCriteria(
        uuid = model.CriteriaField("", dto.uuid).some)

      val selectResp = dao.getCommissionProfileByCriteria(criteria, None, None, None)
      val head = selectResp.map(_.head)
      head.map(_.uuid) mustBe Right(dto.uuid)
      head.map(_.businessType) mustBe Right(dto.businessType)
      head.map(_.tier) mustBe Right(dto.tier)
      head.map(_.subscriptionType) mustBe Right(dto.subscriptionType)
      head.map(_.currencyId) mustBe Right(dto.currencyId)
      head.map(_.currencyCode) mustBe Right("AED")
      head.map(_.instrument) mustBe Right(dto.instrument)
      head.map(_.calculationMethod) mustBe Right(dto.calculationMethod)
      head.map(_.maxCommission) mustBe Right(dto.maxCommission)
      head.map(_.minCommission) mustBe Right(dto.minCommission)
      head.map(_.commissionAmount) mustBe Right(dto.commissionAmount)
      head.map(_.commissionRatio) mustBe Right(dto.commissionRatio)
      head.map(_.createdAt) mustBe Right(dto.createdAt)
      head.map(_.createdBy) mustBe Right(dto.createdBy)
      head.map(_.updatedAt) mustBe Right(dto.createdAt)
      head.map(_.updatedBy) mustBe Right(dto.createdBy)
      head.map(_.ranges) mustBe Right(None)

      val selectRange = dao.getCommissionProfileRangeByCommissionId(head.right.get.id)
      selectRange.map(_.size) mustBe (Right(2))
      selectRange.map(_.head.commissionProfileId) mustBe resp.map(_.id)
      selectRange.map(_.head.min) mustBe Right(range(0).min.get)
      selectRange.map(_.head.max) mustBe Right(range(0).max)
      selectRange.map(_.head.commissionAmount) mustBe Right(range(0).commissionAmount)
      selectRange.map(_.head.commissionRatio) mustBe Right(range(0).commissionRatio)

      selectRange.map(_(1).commissionProfileId) mustBe resp.map(_.id)
      selectRange.map(_(1).min) mustBe Right(range(1).min.get)
      selectRange.map(_(1).max) mustBe Right(range(1).max)
      selectRange.map(_(1).commissionAmount) mustBe Right(range(1).commissionAmount)
      selectRange.map(_(1).commissionRatio) mustBe Right(range(1).commissionRatio)
    }
  }
}
