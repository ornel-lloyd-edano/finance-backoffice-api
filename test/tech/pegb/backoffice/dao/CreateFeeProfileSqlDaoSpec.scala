package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import tech.pegb.backoffice.dao.Dao.IntEntityId
import tech.pegb.backoffice.dao.DaoError.GenericDbError
import tech.pegb.backoffice.dao.fee.abstraction.FeeProfileDao
import tech.pegb.backoffice.dao.fee.dto.{FeeProfileRangeToInsert, FeeProfileToInsert}
import tech.pegb.backoffice.dao.fee.entity.FeeProfileRange
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.core.PegBTestApp

class CreateFeeProfileSqlDaoSpec extends PegBTestApp {

  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

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
    """.stripMargin

  private val dao = inject[FeeProfileDao]

  "FeeProfile insert " should {
    "insert Fee profile return newly created feeProfile" in {
      val feeProfileToInsert = FeeProfileToInsert(
        feeType = "transaction_based",
        userType = "business_user",
        tier = "small",
        subscriptionType = "gold",
        transactionType = "p2p_domestic",
        channel = Some("mobile_application"),
        provider = None,
        instrument = Some("visa_debit"),
        calculationMethod = s"${config.StaircaseFlatPercentage}",
        currencyId = 1,
        feeMethod = "add",
        taxIncluded = "tax_included",
        maxFee = Some(BigDecimal(100)),
        minFee = Some(BigDecimal(5)),
        feeAmount = None,
        feeRatio = None,
        ranges = None,
        createdAt = now,
        createdBy = "test")

      val result = dao.insertFeeProfile(feeProfileToInsert)
      result.isRight mustBe true
      val createdFeeProfile = result.right.get

      createdFeeProfile.id mustBe 1
      createdFeeProfile.feeType mustBe feeProfileToInsert.feeType
      createdFeeProfile.userType mustBe feeProfileToInsert.userType
      createdFeeProfile.tier mustBe feeProfileToInsert.tier
      createdFeeProfile.subscription mustBe feeProfileToInsert.subscriptionType
      createdFeeProfile.transactionType mustBe feeProfileToInsert.transactionType
      createdFeeProfile.channel mustBe feeProfileToInsert.channel
      createdFeeProfile.provider mustBe feeProfileToInsert.provider
      createdFeeProfile.instrument mustBe feeProfileToInsert.instrument
      createdFeeProfile.calculationMethod mustBe feeProfileToInsert.calculationMethod
      createdFeeProfile.currencyCode mustBe "AED"
      createdFeeProfile.feeMethod mustBe feeProfileToInsert.feeMethod
      createdFeeProfile.taxIncluded mustBe feeProfileToInsert.taxIncluded
      createdFeeProfile.maxFee mustBe feeProfileToInsert.maxFee
      createdFeeProfile.minFee mustBe feeProfileToInsert.minFee
      createdFeeProfile.feeAmount mustBe feeProfileToInsert.feeAmount
      createdFeeProfile.feeRatio mustBe feeProfileToInsert.feeRatio
      createdFeeProfile.createdAt mustBe feeProfileToInsert.createdAt
      createdFeeProfile.createdBy mustBe feeProfileToInsert.createdBy
      createdFeeProfile.updatedAt mustBe Some(feeProfileToInsert.createdAt)
      createdFeeProfile.updatedBy mustBe Some(feeProfileToInsert.createdBy)
    }

    "insert Fee profile with ranges return newly created feeProfile" in {
      val feeProfileToInsert = FeeProfileToInsert(
        feeType = "subscription_based",
        userType = "business_user",
        tier = "small",
        subscriptionType = "gold",
        transactionType = "p2p_domestic",
        channel = Some("mobile_application"),
        provider = None,
        instrument = Some("visa_debit"),
        calculationMethod = s"${config.StaircaseFlatFee}",
        currencyId = 1,
        feeMethod = "add",
        taxIncluded = "tax_included",
        maxFee = Some(BigDecimal(100)),
        minFee = Some(BigDecimal(5)),
        feeAmount = None,
        feeRatio = None,
        ranges = Some(Seq(
          FeeProfileRangeToInsert(
            feeProfileId = None,
            max = Some(BigDecimal(20.00)),
            min = BigDecimal(1.00),
            feeAmount = Some(BigDecimal(999.00)),
            feeRatio = None),
          FeeProfileRangeToInsert(
            feeProfileId = None,
            max = Some(BigDecimal(30.00)),
            min = BigDecimal(21.00),
            feeAmount = Some(BigDecimal(1500.00)),
            feeRatio = None))),
        createdAt = now,
        createdBy = "test")

      val result = dao.insertFeeProfile(feeProfileToInsert)
      result.isRight mustBe true
      val createdFeeProfile = result.right.get

      createdFeeProfile.id mustBe 2
      createdFeeProfile.feeType mustBe feeProfileToInsert.feeType
      createdFeeProfile.userType mustBe feeProfileToInsert.userType
      createdFeeProfile.tier mustBe feeProfileToInsert.tier
      createdFeeProfile.subscription mustBe feeProfileToInsert.subscriptionType
      createdFeeProfile.transactionType mustBe feeProfileToInsert.transactionType
      createdFeeProfile.channel mustBe feeProfileToInsert.channel
      createdFeeProfile.provider mustBe feeProfileToInsert.provider
      createdFeeProfile.instrument mustBe feeProfileToInsert.instrument
      createdFeeProfile.calculationMethod mustBe feeProfileToInsert.calculationMethod
      createdFeeProfile.currencyCode mustBe "AED"
      createdFeeProfile.feeMethod mustBe feeProfileToInsert.feeMethod
      createdFeeProfile.taxIncluded mustBe feeProfileToInsert.taxIncluded
      createdFeeProfile.maxFee mustBe feeProfileToInsert.maxFee
      createdFeeProfile.minFee mustBe feeProfileToInsert.minFee
      createdFeeProfile.feeAmount mustBe feeProfileToInsert.feeAmount
      createdFeeProfile.feeRatio mustBe feeProfileToInsert.feeRatio
      createdFeeProfile.createdAt mustBe feeProfileToInsert.createdAt
      createdFeeProfile.createdBy mustBe feeProfileToInsert.createdBy
      createdFeeProfile.updatedAt mustBe Some(feeProfileToInsert.createdAt)
      createdFeeProfile.updatedBy mustBe Some(feeProfileToInsert.createdBy)
      createdFeeProfile.ranges mustBe Some(Seq(
        FeeProfileRange(
          id = 1,
          feeProfileId = Some(createdFeeProfile.id),
          max = Some(BigDecimal(20)),
          min = Some(BigDecimal(1)),
          feeAmount = Some(BigDecimal(999)),
          feeRatio = None),
        FeeProfileRange(
          id = 2,
          feeProfileId = Some(createdFeeProfile.id),
          max = Some(BigDecimal(30)),
          min = Some(BigDecimal(21)),
          feeAmount = Some(BigDecimal(1500)),
          feeRatio = None)))
    }

    "insert single range row" in {
      val feeProfileRangeToInsert = FeeProfileRangeToInsert(
        feeProfileId = Some(1),
        max = Some(BigDecimal(100.00)),
        min = BigDecimal(5.00),
        feeAmount = Some(BigDecimal(50.00)),
        feeRatio = None)

      val result = dao.insertFeeProfileRange(IntEntityId(1), Seq(feeProfileRangeToInsert))

      result.isRight mustBe true
      val createdRange = result.right.get
      createdRange mustBe Seq(FeeProfileRange(
        id = 3,
        feeProfileId = Some(1),
        max = Some(BigDecimal(100)),
        min = Some(BigDecimal(5)),
        feeAmount = Some(BigDecimal(50)),
        feeRatio = None))
    }

    "insert multiple range row" in {
      val feeProfileRangesToInsert = Seq(
        FeeProfileRangeToInsert(
          feeProfileId = Some(1),
          max = Some(BigDecimal(1000.00)),
          min = BigDecimal(101.00),
          feeAmount = Some(BigDecimal(100.00)),
          feeRatio = None),
        FeeProfileRangeToInsert(
          feeProfileId = Some(1),
          max = Some(BigDecimal(5000.00)),
          min = BigDecimal(1001.00),
          feeAmount = Some(BigDecimal(500.00)),
          feeRatio = None),
        FeeProfileRangeToInsert(
          feeProfileId = Some(1),
          max = None,
          min = BigDecimal(5001.00),
          feeAmount = Some(BigDecimal(1000.00)),
          feeRatio = None))

      val result = dao.insertFeeProfileRange(IntEntityId(1), feeProfileRangesToInsert)

      result.isRight mustBe true
      val createdRange = result.right.get
      createdRange mustBe Seq(
        FeeProfileRange(
          id = 3,
          feeProfileId = Some(1),
          max = Some(BigDecimal(100)),
          min = Some(BigDecimal(5)),
          feeAmount = Some(BigDecimal(50)),
          feeRatio = None),
        FeeProfileRange(
          id = 4,
          feeProfileId = Some(1),
          max = Some(BigDecimal(1000)),
          min = Some(BigDecimal(101)),
          feeAmount = Some(BigDecimal(100)),
          feeRatio = None),
        FeeProfileRange(
          id = 5,
          feeProfileId = Some(1),
          max = Some(BigDecimal(5000)),
          min = Some(BigDecimal(1001)),
          feeAmount = Some(BigDecimal(500)),
          feeRatio = None),
        FeeProfileRange(
          id = 6,
          feeProfileId = Some(1),
          max = None,
          min = Some(BigDecimal(5001)),
          feeAmount = Some(BigDecimal(1000)),
          feeRatio = None))
    }

    "Error if no range to insert" in {

      val result = dao.insertFeeProfileRange(IntEntityId(1), Nil)

      result mustBe Left(GenericDbError("Failed to fetch created profile ranges"))
    }

    "insert Fee profile with null channel returns newly created feeProfile" in {
      val feeProfileToInsert = FeeProfileToInsert(
        feeType = "transaction_based",
        userType = "business_user",
        tier = "small",
        subscriptionType = "gold",
        transactionType = "p2p_domestic",
        channel = None,
        provider = None,
        instrument = Some("visa_debit"),
        calculationMethod = s"${config.StaircaseFlatPercentage}",
        currencyId = 1,
        feeMethod = "add",
        taxIncluded = "tax_included",
        maxFee = Some(BigDecimal(100)),
        minFee = Some(BigDecimal(5)),
        feeAmount = None,
        feeRatio = None,
        ranges = None,
        createdAt = now,
        createdBy = "test")

      val result = dao.insertFeeProfile(feeProfileToInsert)
      result.isRight mustBe true
      val createdFeeProfile = result.right.get

      createdFeeProfile.id mustBe 3
      createdFeeProfile.feeType mustBe feeProfileToInsert.feeType
      createdFeeProfile.userType mustBe feeProfileToInsert.userType
      createdFeeProfile.tier mustBe feeProfileToInsert.tier
      createdFeeProfile.subscription mustBe feeProfileToInsert.subscriptionType
      createdFeeProfile.transactionType mustBe feeProfileToInsert.transactionType
      createdFeeProfile.channel mustBe feeProfileToInsert.channel
      createdFeeProfile.provider mustBe feeProfileToInsert.provider
      createdFeeProfile.instrument mustBe feeProfileToInsert.instrument
      createdFeeProfile.calculationMethod mustBe feeProfileToInsert.calculationMethod
      createdFeeProfile.currencyCode mustBe "AED"
      createdFeeProfile.feeMethod mustBe feeProfileToInsert.feeMethod
      createdFeeProfile.taxIncluded mustBe feeProfileToInsert.taxIncluded
      createdFeeProfile.maxFee mustBe feeProfileToInsert.maxFee
      createdFeeProfile.minFee mustBe feeProfileToInsert.minFee
      createdFeeProfile.feeAmount mustBe feeProfileToInsert.feeAmount
      createdFeeProfile.feeRatio mustBe feeProfileToInsert.feeRatio
      createdFeeProfile.createdAt mustBe feeProfileToInsert.createdAt
      createdFeeProfile.createdBy mustBe feeProfileToInsert.createdBy
      createdFeeProfile.updatedAt mustBe Some(feeProfileToInsert.createdAt)
      createdFeeProfile.updatedBy mustBe Some(feeProfileToInsert.createdBy)
    }
  }
}
