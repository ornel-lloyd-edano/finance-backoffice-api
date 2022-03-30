package tech.pegb.backoffice.domain.commission

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.{Currency, UUID}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.{Binding, bind}
import tech.pegb.backoffice.dao.commission.abstraction.CommissionProfileDao
import tech.pegb.backoffice.dao.commission.entity.{CommissionProfile, CommissionProfileRange}
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessTypes, BusinessUserTiers}
import tech.pegb.backoffice.domain.commission.abstraction.CommissionProfileManagement
import tech.pegb.backoffice.domain.commission.dto.{CommissionProfileCriteria, CommissionProfileRangeToCreate, CommissionProfileToCreate}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.model.TransactionType
import tech.pegb.backoffice.domain.types.enum.CommissionCalculationMethods
import tech.pegb.core.PegBNoDbTestApp
import tech.pegb.backoffice.mapping.domain.dao.commission.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.commission.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.util.UUIDLike

class CommissionMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val dao: CommissionProfileDao = stub[CommissionProfileDao]
  private val currencyDao: CurrencyDao = stub[CurrencyDao]

  override def additionalBindings: Seq[Binding[_]] = {
    super.additionalBindings ++ Seq(
      bind[CommissionProfileDao].toInstance(dao),
      bind[CurrencyDao].to(currencyDao))
  }

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val service = inject[CommissionProfileManagement]

  "Flat Percentage Commission Management Service " should {

    "return created commission_profile when valid input" in {
      val dto = CommissionProfileToCreate(
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.FlatPercentage,
        maxCommission = BigDecimal(100).some,
        minCommission = BigDecimal(10).some,
        flatAmount = None,
        percentageAmount = BigDecimal(0.25).some,
        ranges = None,
        createdBy = "pegbuser",
        createdAt = now)

      val expected = CommissionProfile(
        id = 1,
        uuid = dto.uuid.toString,
        businessType = "merchant",
        tier = "small",
        subscriptionType = "standard",
        transactionType = "cashin",
        currencyId = 1,
        currencyCode = "KES",
        channel = None,
        instrument = None,
        calculationMethod = "flat_percentage",
        maxCommission = BigDecimal(100).some,
        minCommission = BigDecimal(10).some,
        commissionAmount = None,
        commissionRatio = BigDecimal(0.25).some,
        ranges = None,
        createdBy = "pegbuser",
        updatedBy = "pegbuser",
        createdAt = now,
        updatedAt = now,
        deletedAt = None)

      (currencyDao.getCurrenciesWithId _).when(true.some)
        .returns(Right(List((1, "AED"), (2, "USD"), (3, "KES"))))
      (typesDao.getChannels _).when()
        .returns(Right(List((20, "mobile_application", None))))
      (typesDao.getInstruments _).when()
        .returns(Right(List((21, "visa_debit", None))))

      (dao.getCommissionProfileByCriteria _).when(dto.asDaoCriteria, None, None, None)
        .returns(Right(Nil))
      (dao.insertCommissionProfile _).when(dto.asDao(3))
        .returns(Right(expected))

      val res = service.createCommissionProfile(dto)
      whenReady(res) { actual ⇒
        actual mustBe expected.asDomain.toEither
      }
    }

    "return error when flat_percentage has flatAmount" in {
      val dto = CommissionProfileToCreate(
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.FlatPercentage,
        maxCommission = BigDecimal(100).some,
        minCommission = BigDecimal(10).some,
        flatAmount = BigDecimal(123).some,
        percentageAmount = BigDecimal(0.25).some,
        ranges = None,
        createdBy = "pegbuser",
        createdAt = now)

      val res = service.createCommissionProfile(dto)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("assertion failed: Flat Amount should not be defined if calculation method is percentage type"))
      }
    }

    "return error when flat_percentage has range" in {
      val range = CommissionProfileRangeToCreate(
        from = BigDecimal(10),
        to = BigDecimal(100).some,
        flatAmount = None,
        percentageAmount = BigDecimal(0.25).some)
      val dto = CommissionProfileToCreate(
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.FlatPercentage,
        maxCommission = None,
        minCommission = None,
        flatAmount = None,
        percentageAmount = BigDecimal(0.25).some,
        ranges = Some(Seq(range)),
        createdBy = "pegbuser",
        createdAt = now)

      val res = service.createCommissionProfile(dto)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("assertion failed: Range can only be defined if calculation method is staircase type"))
      }
    }

    "return error when max is less than min" in {
      val range = CommissionProfileRangeToCreate(
        from = BigDecimal(10),
        to = BigDecimal(100).some,
        flatAmount = None,
        percentageAmount = BigDecimal(0.25).some)
      val dto = CommissionProfileToCreate(
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.FlatPercentage,
        maxCommission = BigDecimal(1).some,
        minCommission = BigDecimal(2).some,
        flatAmount = None,
        percentageAmount = BigDecimal(0.25).some,
        ranges = Some(Seq(range)),
        createdBy = "pegbuser",
        createdAt = now)

      val res = service.createCommissionProfile(dto)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("assertion failed: Range can only be defined if calculation method is staircase type"))
      }
    }

  }

  "Staircase Percentage Commission Management Service " should {

    "return created commission_profile when valid input" in {
      val ranges = Seq(
        CommissionProfileRangeToCreate(
          from = BigDecimal(10),
          to = BigDecimal(100).some,
          flatAmount = None,
          percentageAmount = BigDecimal(0.5).some),
        CommissionProfileRangeToCreate(
          from = BigDecimal(100),
          to = BigDecimal(1000).some,
          flatAmount = None,
          percentageAmount = BigDecimal(0.25).some))

      val dto = CommissionProfileToCreate(
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.StaircaseFlatPercentage,
        maxCommission = None,
        minCommission = None,
        flatAmount = None,
        percentageAmount = None,
        ranges = ranges.some,
        createdBy = "pegbuser",
        createdAt = now)

      val expected = CommissionProfile(
        id = 1,
        uuid = dto.uuid.toString,
        businessType = "merchant",
        tier = "small",
        subscriptionType = "standard",
        transactionType = "cashin",
        currencyId = 1,
        currencyCode = "KES",
        channel = None,
        instrument = None,
        calculationMethod = "flat_percentage",
        maxCommission = BigDecimal(100).some,
        minCommission = BigDecimal(10).some,
        commissionAmount = None,
        commissionRatio = BigDecimal(0.25).some,
        ranges = Seq(CommissionProfileRange(
          id = 1,
          commissionProfileId = 1,
          min = BigDecimal(10),
          max = BigDecimal(100).some,
          commissionAmount = None,
          commissionRatio = BigDecimal(0.5).some,
          createdAt = now,
          updatedAt = now)).some,
        createdBy = "pegbuser",
        updatedBy = "pegbuser",
        createdAt = now,
        updatedAt = now,
        deletedAt = None)

      (currencyDao.getCurrenciesWithId _).when(true.some)
        .returns(Right(List((1, "AED"), (2, "USD"), (3, "KES"))))
      (typesDao.getChannels _).when()
        .returns(Right(List((20, "mobile_application", None))))
      (typesDao.getInstruments _).when()
        .returns(Right(List((21, "visa_debit", None))))

      (dao.getCommissionProfileByCriteria _).when(dto.asDaoCriteria, None, None, None)
        .returns(Right(Nil))
      (dao.insertCommissionProfile _).when(dto.asDao(3))
        .returns(Right(expected))

      val res = service.createCommissionProfile(dto)
      whenReady(res) { actual ⇒
        actual mustBe expected.asDomain.toEither
      }
    }

    "return error when staircase has flatAmount" in {
      val ranges = Seq(
        CommissionProfileRangeToCreate(
          from = BigDecimal(10),
          to = BigDecimal(100).some,
          flatAmount = None,
          percentageAmount = BigDecimal(0.5).some),
        CommissionProfileRangeToCreate(
          from = BigDecimal(100),
          to = BigDecimal(1000).some,
          flatAmount = None,
          percentageAmount = BigDecimal(0.25).some))

      val dto = CommissionProfileToCreate(
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.StaircaseFlatPercentage,
        maxCommission = None,
        minCommission = None,
        flatAmount = BigDecimal(100).some,
        percentageAmount = None,
        ranges = ranges.some,
        createdBy = "pegbuser",
        createdAt = now)

      val res = service.createCommissionProfile(dto)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("assertion failed: Global Flat Amount should not be defined if calculation method is staircase type"))
      }
    }

    "return error when staircase has global percentage" in {
      val ranges = Seq(
        CommissionProfileRangeToCreate(
          from = BigDecimal(10),
          to = BigDecimal(100).some,
          flatAmount = None,
          percentageAmount = BigDecimal(0.5).some),
        CommissionProfileRangeToCreate(
          from = BigDecimal(100),
          to = BigDecimal(1000).some,
          flatAmount = None,
          percentageAmount = BigDecimal(0.25).some))

      val dto = CommissionProfileToCreate(
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.StaircaseFlatPercentage,
        maxCommission = BigDecimal(100).some,
        minCommission = BigDecimal(10).some,
        flatAmount = None,
        percentageAmount = BigDecimal(0.5).some,
        ranges = ranges.some,
        createdBy = "pegbuser",
        createdAt = now)

      val res = service.createCommissionProfile(dto)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("assertion failed: Global Percentage Amount should not be defined if calculation method is staircase type"))
      }
    }

    "return error when range has gap" in {
      val ranges = Seq(
        CommissionProfileRangeToCreate(
          from = BigDecimal(10),
          to = BigDecimal(100).some,
          flatAmount = None,
          percentageAmount = BigDecimal(0.5).some),
        CommissionProfileRangeToCreate(
          from = BigDecimal(101),
          to = BigDecimal(1000).some,
          flatAmount = None,
          percentageAmount = BigDecimal(0.25).some))

      val dto = CommissionProfileToCreate(
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.StaircaseFlatPercentage,
        maxCommission = None,
        minCommission = None,
        flatAmount = None,
        percentageAmount = None,
        ranges = ranges.some,
        createdBy = "pegbuser",
        createdAt = now)

      val res = service.createCommissionProfile(dto)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("Current range 'from' value (101) should be equal to Previous Range 'to' value (100)"))
      }
    }
  }
  "Staircase FlatAmount Commission Management Service " should {

    "return created commission_profile when valid input" in {
      val ranges = Seq(
        CommissionProfileRangeToCreate(
          from = BigDecimal(10),
          to = BigDecimal(100).some,
          flatAmount = BigDecimal(50).some,
          percentageAmount = None),
        CommissionProfileRangeToCreate(
          from = BigDecimal(100),
          to = BigDecimal(1000).some,
          flatAmount = BigDecimal(40).some,
          percentageAmount = None))

      val dto = CommissionProfileToCreate(
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.StaircaseFlatAmount,
        maxCommission = None,
        minCommission = None,
        flatAmount = None,
        percentageAmount = None,
        ranges = ranges.some,
        createdBy = "pegbuser",
        createdAt = now)

      val expected = CommissionProfile(
        id = 1,
        uuid = dto.uuid.toString,
        businessType = "merchant",
        tier = "small",
        subscriptionType = "standard",
        transactionType = "cashin",
        currencyId = 1,
        currencyCode = "KES",
        channel = None,
        instrument = None,
        calculationMethod = "flat_percentage",
        maxCommission = BigDecimal(100).some,
        minCommission = BigDecimal(10).some,
        commissionAmount = None,
        commissionRatio = BigDecimal(0.25).some,
        ranges = Seq(CommissionProfileRange(
          id = 1,
          commissionProfileId = 1,
          min = BigDecimal(10),
          max = BigDecimal(100).some,
          commissionAmount = None,
          commissionRatio = BigDecimal(0.5).some,
          createdAt = now,
          updatedAt = now)).some,
        createdBy = "pegbuser",
        updatedBy = "pegbuser",
        createdAt = now,
        updatedAt = now,
        deletedAt = None)

      (currencyDao.getCurrenciesWithId _).when(true.some)
        .returns(Right(List((1, "AED"), (2, "USD"), (3, "KES"))))
      (typesDao.getChannels _).when()
        .returns(Right(List((20, "mobile_application", None))))
      (typesDao.getInstruments _).when()
        .returns(Right(List((21, "visa_debit", None))))

      (dao.getCommissionProfileByCriteria _).when(dto.asDaoCriteria, None, None, None)
        .returns(Right(Nil))
      (dao.insertCommissionProfile _).when(dto.asDao(3))
        .returns(Right(expected))

      val res = service.createCommissionProfile(dto)
      whenReady(res) { actual ⇒
        actual mustBe expected.asDomain.toEither
      }
    }

    "return error when staircase has flatPercentage" in {
      val ranges = Seq(
        CommissionProfileRangeToCreate(
          from = BigDecimal(10),
          to = BigDecimal(100).some,
          flatAmount = BigDecimal(50).some,
          percentageAmount = None),
        CommissionProfileRangeToCreate(
          from = BigDecimal(100),
          to = BigDecimal(1000).some,
          flatAmount = BigDecimal(40).some,
          percentageAmount = None))

      val dto = CommissionProfileToCreate(
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.StaircaseFlatAmount,
        maxCommission = None,
        minCommission = None,
        flatAmount = BigDecimal(100).some,
        percentageAmount = None,
        ranges = ranges.some,
        createdBy = "pegbuser",
        createdAt = now)

      val res = service.createCommissionProfile(dto)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("assertion failed: Global Flat Amount should not be defined if calculation method is staircase type"))
      }
    }

  }

  "GET commission profiles and ranges" should {
    val cp1 = 1
    val cpUUID1 = UUID.randomUUID()
    val cp2 = 2
    val cpUUID2 = UUID.randomUUID()

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
        updatedAt = now))
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

    "Return list of profiles in getByCriteria" in {
      val criteria = CommissionProfileCriteria()
      val ordering = Seq(Ordering("calculation_method", Ordering.ASCENDING), Ordering("commission_amount", Ordering.DESCENDING))

      (dao.getCommissionProfileByCriteria _).when(criteria.asDao, ordering.asDao, None, None)
        .returns(Right(Seq(c1, c2)))

      val res = service.getCommissionProfileByCriteria(criteria, ordering, None, None)

      whenReady(res) { actual ⇒
        actual mustBe Right(Seq(c1, c2).flatMap(_.asDomain.toOption))
      }
    }

    "Return matching of profile and ranges in getById" in {
      val criteria = CommissionProfileCriteria(uuid = UUIDLike(cpUUID2.toString).some)

      (dao.getCommissionProfileByCriteria _).when(criteria.asDao, None, None, None)
        .returns(Right(Seq(c2)))

      (dao.getCommissionProfileRangeByCommissionId _).when(c2.id)
        .returns(Right(c2Ranges))

      val res = service.getCommissionProfile(cpUUID2)

      whenReady(res) { actual ⇒
        actual mustBe Right(c2.addRanges(c2Ranges.some).asDomain.get)
      }
    }
  }

}
