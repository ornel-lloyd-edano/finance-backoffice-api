package tech.pegb.backoffice.domain.fee

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.fee.abstraction.FeeProfileDao
import tech.pegb.backoffice.dao.fee.entity.FeeProfile
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserTiers
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerSubscription
import tech.pegb.backoffice.domain.customer.model.{IndividualUserTiers, UserType}
import tech.pegb.backoffice.domain.fee.dto.{FeeProfileRangeToCreate, FeeProfileToCreate}
import tech.pegb.backoffice.domain.fee.implementation.FeeProfileMgmtService
import tech.pegb.backoffice.domain.fee.model.FeeAttributes._
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.mapping.dao.domain.fee.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.fee.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class CreateFeeProfileMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val feeProfileDao = stub[FeeProfileDao]
  private val currencyDao: CurrencyDao = stub[CurrencyDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[FeeProfileDao].to(feeProfileDao),
      bind[CurrencyDao].to(currencyDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val feeProfileMgmtService = inject[FeeProfileMgmtService]
  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  "FeeProfileMgmtService createFeeProfile" should {

    "return created fee profile (flat_fee)" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("subscription_based"),
        userType = UserType("business"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = Some(BigDecimal(10.00)),
        percentageAmount = None,
        ranges = None,
        createdAt = now,
        createdBy = "pegbuser")

      val expected = FeeProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        feeType = "subscription_based",
        userType = "business",
        tier = "basic",
        subscription = "gold",
        transactionType = "p2p_domestic",
        channel = Some("mobile_application"),
        provider = None,
        instrument = Some("visa_debit"),
        calculationMethod = "flat_fee",
        maxFee = None,
        minFee = None,
        feeAmount = Some(BigDecimal(10.00)),
        feeRatio = None,
        feeMethod = "add",
        taxIncluded = "no_tax",
        ranges = None,
        currencyCode = "AED",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None,
        deletedAt = None)

      (currencyDao.getCurrenciesWithId _).when(Some(true))
        .returns(Right(List((1, "AED"), (2, "USD"), (3, "KES"))))
      (typesDao.getFeeTypes _).when()
        .returns(Right(List((11, "subscription_based", None), (12, "transaction_based", None))))
      (typesDao.getCustomerTypes _).when()
        .returns(Right(List((13, "business", None), (14, "individual_user", None))))
      (typesDao.getCustomerSubscriptions _).when()
        .returns(Right(List((15, "standard", None), (16, "gold", None), (17, "platinum", None))))
      (typesDao.getTransactionTypes _).when()
        .returns(Right(List((18, "p2p_domestic", None), (19, "p2p_international", None))))
      (typesDao.getChannels _).when()
        .returns(Right(List((20, "mobile_application", None))))
      (typesDao.getInstruments _).when()
        .returns(Right(List((21, "visa_debit", None))))
      (currencyDao.getAllNames _).when()
        .returns(Right(Set("AED", "USD", "KES")))
      (typesDao.getFeeCalculationMethod _).when()
        .returns(Right(List(
          (25, "flat_fee", None),
          (26, "flat_percentages", None),
          (27, "staircase_flat_fee", None),
          (28, "staircase_flat_percentages", None))))
      (typesDao.getFeeMethods _).when()
        .returns(Right(List((29, "add", None), (30, "deduct", None))))

      (feeProfileDao.getFeeProfileByCriteria _).when(feeProfileToCreate.asDaoCriteria, None, None, None)
        .returns(Right(Nil))
      (feeProfileDao.insertFeeProfile _).when(feeProfileToCreate.asDao(1, None))
        .returns(Right(expected))

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Right(expected.asDomain)
      }
    }

    "return created fee profile (flat_percentages)" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("transaction_based"),
        userType = UserType("business"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("flat_percentages"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = None,
        percentageAmount = Some(BigDecimal(25.00)),
        ranges = None,
        createdAt = now,
        createdBy = "pegbuser")

      val expected = FeeProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        feeType = feeProfileToCreate.feeType.underlying,
        userType = feeProfileToCreate.userType.underlying,
        tier = "basic",
        subscription = feeProfileToCreate.subscription.underlying,
        transactionType = feeProfileToCreate.transactionType.underlying,
        channel = feeProfileToCreate.channel.map(_.underlying),
        provider = feeProfileToCreate.otherParty,
        instrument = feeProfileToCreate.instrument,
        calculationMethod = feeProfileToCreate.calculationMethod.underlying,
        maxFee = feeProfileToCreate.maxFee,
        minFee = feeProfileToCreate.minFee,
        feeAmount = feeProfileToCreate.flatAmount,
        feeRatio = feeProfileToCreate.percentageAmount,
        feeMethod = feeProfileToCreate.feeMethod.underlying,
        taxIncluded = "no_tax",
        ranges = None,
        currencyCode = feeProfileToCreate.currencyCode.getCurrencyCode,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None,
        deletedAt = None)

      (currencyDao.getCurrenciesWithId _).when(Some(true))
        .returns(Right(List((1, "AED"), (2, "USD"), (3, "KES"))))
      (typesDao.getFeeTypes _).when()
        .returns(Right(List((11, "subscription_based", None), (12, "transaction_based", None))))
      (typesDao.getCustomerTypes _).when()
        .returns(Right(List((13, "business", None), (14, "individual_user", None))))
      (typesDao.getCustomerSubscriptions _).when()
        .returns(Right(List((15, "standard", None), (16, "gold", None), (17, "platinum", None))))
      (typesDao.getTransactionTypes _).when()
        .returns(Right(List((18, "p2p_domestic", None), (19, "p2p_international", None))))
      (typesDao.getChannels _).when()
        .returns(Right(List((20, "mobile_application", None))))
      (typesDao.getInstruments _).when()
        .returns(Right(List((21, "visa_debit", None))))
      (currencyDao.getAllNames _).when()
        .returns(Right(Set("AED", "USD", "KES")))
      (typesDao.getFeeCalculationMethod _).when()
        .returns(Right(List(
          (25, "flat_fee", None),
          (26, "flat_percentages", None),
          (27, "staircase_flat_fee", None),
          (28, "staircase_flat_percentages", None))))
      (typesDao.getFeeMethods _).when()
        .returns(Right(List((29, "add", None), (30, "deduct", None))))

      (feeProfileDao.getFeeProfileByCriteria _).when(feeProfileToCreate.asDaoCriteria, None, None, None)
        .returns(Right(Nil))
      (feeProfileDao.insertFeeProfile _).when(feeProfileToCreate.asDao(1, None))
        .returns(Right(expected))

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Right(expected.asDomain)
      }
    }

    "return created fee profile (staircase_flat_fee)" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("transaction_based"),
        userType = UserType("business"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("staircase_flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = None,
        percentageAmount = None,
        ranges = Some(Seq(
          FeeProfileRangeToCreate(
            from = BigDecimal(1),
            to = Some(BigDecimal(20)),
            flatAmount = Some(BigDecimal(10)),
            percentageAmount = None),
          FeeProfileRangeToCreate(
            from = BigDecimal(20),
            to = None,
            flatAmount = Some(BigDecimal(15)),
            percentageAmount = None))),
        createdAt = now,
        createdBy = "pegbuser")

      val expected = FeeProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        feeType = feeProfileToCreate.feeType.underlying,
        userType = feeProfileToCreate.userType.underlying,
        tier = "basic",
        subscription = feeProfileToCreate.subscription.underlying,
        transactionType = feeProfileToCreate.transactionType.underlying,
        channel = feeProfileToCreate.channel.map(_.underlying),
        provider = feeProfileToCreate.otherParty,
        instrument = feeProfileToCreate.instrument,
        calculationMethod = feeProfileToCreate.calculationMethod.underlying,
        maxFee = feeProfileToCreate.maxFee,
        minFee = feeProfileToCreate.minFee,
        feeAmount = feeProfileToCreate.flatAmount,
        feeRatio = feeProfileToCreate.percentageAmount,
        feeMethod = feeProfileToCreate.feeMethod.underlying,
        taxIncluded = "no_tax",
        ranges = None,
        currencyCode = feeProfileToCreate.currencyCode.getCurrencyCode,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None,
        deletedAt = None)

      (currencyDao.getCurrenciesWithId _).when(Some(true))
        .returns(Right(List((1, "AED"), (2, "USD"), (3, "KES"))))
      (typesDao.getFeeTypes _).when()
        .returns(Right(List((11, "subscription_based", None), (12, "transaction_based", None))))
      (typesDao.getCustomerTypes _).when()
        .returns(Right(List((13, "business", None), (14, "individual_user", None))))
      (typesDao.getCustomerSubscriptions _).when()
        .returns(Right(List((15, "standard", None), (16, "gold", None), (17, "platinum", None))))
      (typesDao.getTransactionTypes _).when()
        .returns(Right(List((18, "p2p_domestic", None), (19, "p2p_international", None))))
      (typesDao.getChannels _).when()
        .returns(Right(List((20, "mobile_application", None))))
      (typesDao.getInstruments _).when()
        .returns(Right(List((21, "visa_debit", None))))
      (currencyDao.getAllNames _).when()
        .returns(Right(Set("AED", "USD", "KES")))
      (typesDao.getFeeCalculationMethod _).when()
        .returns(Right(List(
          (25, "flat_fee", None),
          (26, "flat_percentages", None),
          (27, "staircase_flat_fee", None),
          (28, "staircase_flat_percentages", None))))
      (typesDao.getFeeMethods _).when()
        .returns(Right(List((29, "add", None), (30, "deduct", None))))

      (feeProfileDao.getFeeProfileByCriteria _).when(feeProfileToCreate.asDaoCriteria, None, None, None)
        .returns(Right(Nil))
      (feeProfileDao.insertFeeProfile _).when(feeProfileToCreate.asDao(1, None))
        .returns(Right(expected))

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Right(expected.asDomain)
      }
    }

    "return created fee profile (staircase_flat_percentages)" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("transaction_based"),
        userType = UserType("business"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("staircase_flat_percentages"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = None,
        percentageAmount = None,
        ranges = Some(Seq(
          FeeProfileRangeToCreate(
            from = BigDecimal(1),
            to = Some(BigDecimal(20)),
            flatAmount = None,
            percentageAmount = Some(BigDecimal(10))),
          FeeProfileRangeToCreate(
            from = BigDecimal(20),
            to = Some(BigDecimal(40)),
            flatAmount = None,
            percentageAmount = Some(BigDecimal(15))))),
        createdAt = now,
        createdBy = "pegbuser")

      val expected = FeeProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        feeType = feeProfileToCreate.feeType.underlying,
        userType = feeProfileToCreate.userType.underlying,
        tier = "basic",
        subscription = feeProfileToCreate.subscription.underlying,
        transactionType = feeProfileToCreate.transactionType.underlying,
        channel = feeProfileToCreate.channel.map(_.underlying),
        provider = feeProfileToCreate.otherParty,
        instrument = feeProfileToCreate.instrument,
        calculationMethod = feeProfileToCreate.calculationMethod.underlying,
        maxFee = feeProfileToCreate.maxFee,
        minFee = feeProfileToCreate.minFee,
        feeAmount = feeProfileToCreate.flatAmount,
        feeRatio = feeProfileToCreate.percentageAmount,
        feeMethod = feeProfileToCreate.feeMethod.underlying,
        taxIncluded = "no_tax",
        ranges = None,
        currencyCode = feeProfileToCreate.currencyCode.getCurrencyCode,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None,
        deletedAt = None)

      (currencyDao.getCurrenciesWithId _).when(Some(true))
        .returns(Right(List((1, "AED"), (2, "USD"), (3, "KES"))))
      (typesDao.getFeeTypes _).when()
        .returns(Right(List((11, "subscription_based", None), (12, "transaction_based", None))))
      (typesDao.getCustomerTypes _).when()
        .returns(Right(List((13, "business", None), (14, "individual_user", None))))
      (typesDao.getCustomerSubscriptions _).when()
        .returns(Right(List((15, "standard", None), (16, "gold", None), (17, "platinum", None))))
      (typesDao.getTransactionTypes _).when()
        .returns(Right(List((18, "p2p_domestic", None), (19, "p2p_international", None))))
      (typesDao.getChannels _).when()
        .returns(Right(List((20, "mobile_application", None))))
      (typesDao.getInstruments _).when()
        .returns(Right(List((21, "visa_debit", None))))
      (currencyDao.getAllNames _).when()
        .returns(Right(Set("AED", "USD", "KES")))
      (typesDao.getFeeCalculationMethod _).when()
        .returns(Right(List(
          (25, "flat_fee", None),
          (26, "flat_percentages", None),
          (27, "staircase_flat_fee", None),
          (28, "staircase_flat_percentages", None))))
      (typesDao.getFeeMethods _).when()
        .returns(Right(List((29, "add", None), (30, "deduct", None))))

      (feeProfileDao.getFeeProfileByCriteria _).when(feeProfileToCreate.asDaoCriteria, None, None, None)
        .returns(Right(Nil))
      (feeProfileDao.insertFeeProfile _).when(feeProfileToCreate.asDao(1, None))
        .returns(Right(expected))

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Right(expected.asDomain)
      }
    }

    "return error when similar fee already exists" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("subscription_based"),
        userType = UserType("business"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = Some(BigDecimal(10.00)),
        percentageAmount = None,
        ranges = None,
        createdAt = now,
        createdBy = "pegbuser")

      val expected = FeeProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        feeType = "subscription_based",
        userType = "business",
        tier = "basic",
        subscription = "gold",
        transactionType = "p2p_domestic",
        channel = Some("mobile_application"),
        provider = None,
        instrument = Some("visa_debit"),
        calculationMethod = "flat_fee",
        maxFee = None,
        minFee = None,
        feeAmount = Some(BigDecimal(10.00)),
        feeRatio = None,
        feeMethod = "add",
        taxIncluded = "no_tax",
        ranges = None,
        currencyCode = "AED",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None,
        deletedAt = None)

      (feeProfileDao.getFeeProfileByCriteria _).when(feeProfileToCreate.asDaoCriteria, None, None, None)
        .returns(Right(Seq(expected)))

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("Fee profile with same features already exists"))
      }
    }

    "return error when staircase_flat_fee contains percentageAmount" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("transaction_based"),
        userType = UserType("business"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("staircase_flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = None,
        percentageAmount = None,
        ranges = Some(Seq(
          FeeProfileRangeToCreate(
            from = BigDecimal(1),
            to = Some(BigDecimal(20)),
            flatAmount = None,
            percentageAmount = Some(BigDecimal(10))),
          FeeProfileRangeToCreate(
            from = BigDecimal(21),
            to = Some(BigDecimal(40)),
            flatAmount = Some(BigDecimal(15)),
            percentageAmount = None))),
        createdAt = now,
        createdBy = "pegbuser")

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("All flatAmount should be defined AND all percentageAmount should be empty for Non-percentage staircase type calculationMethod"))
      }
    }

    "return error when staircase_flat_percentages contains flat_fee" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("transaction_based"),
        userType = UserType("business"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("staircase_flat_percentages"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = None,
        percentageAmount = None,
        ranges = Some(Seq(
          FeeProfileRangeToCreate(
            from = BigDecimal(1),
            to = Some(BigDecimal(20)),
            flatAmount = None,
            percentageAmount = Some(BigDecimal(10))),
          FeeProfileRangeToCreate(
            from = BigDecimal(21),
            to = Some(BigDecimal(40)),
            flatAmount = Some(BigDecimal(15)),
            percentageAmount = None))),
        createdAt = now,
        createdBy = "pegbuser")

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("All percentageAmount should be defined AND all flatAmount should be empty for percentage staircase type calculationMethod"))
      }
    }

    "return error when invalid ranges" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("transaction_based"),
        userType = UserType("business"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("staircase_flat_percentages"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = None,
        percentageAmount = None,
        ranges = Some(Seq(
          FeeProfileRangeToCreate(
            from = BigDecimal(100),
            to = Some(BigDecimal(1040)),
            flatAmount = None,
            percentageAmount = Some(BigDecimal(10))),
          FeeProfileRangeToCreate(
            from = BigDecimal(21),
            to = Some(BigDecimal(40)),
            flatAmount = None,
            percentageAmount = Some(BigDecimal(10))))),
        createdAt = now,
        createdBy = "pegbuser")

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("Current range 'from' value (21) should be equal to Previous Range 'to' value (1040)"))
      }
    }

    "return error when invalid ranges (to = None appears no non-last range)" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("transaction_based"),
        userType = UserType("business"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("staircase_flat_percentages"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = None,
        percentageAmount = None,
        ranges = Some(Seq(
          FeeProfileRangeToCreate(
            from = BigDecimal(100),
            to = None,
            flatAmount = None,
            percentageAmount = Some(BigDecimal(10))),
          FeeProfileRangeToCreate(
            from = BigDecimal(2000),
            to = Some(BigDecimal(5000)),
            flatAmount = None,
            percentageAmount = Some(BigDecimal(10))))),
        createdAt = now,
        createdBy = "pegbuser")

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("Range 'to' value can only be empty for the last range element"))
      }
    }

    "return validation error when tier is not applicable for type (individual)" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("subscription_based"),
        userType = UserType("individual"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = Some(BigDecimal(10.00)),
        percentageAmount = None,
        ranges = None,
        createdAt = now,
        createdBy = "pegbuser")

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("Tier small is not a valid IndividualUser Tier"))
      }
    }

    "return validation error when tier is not applicable for type (business)" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("subscription_based"),
        userType = UserType("business"),
        tier = IndividualUserTiers.Basic,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = Some(BigDecimal(10.00)),
        percentageAmount = None,
        ranges = None,
        createdAt = now,
        createdBy = "pegbuser")

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("Tier basic is not a valid BusinessUser Tier"))
      }
    }

    "return error when currency is inactive" in {

      implicit val requestId = UUID.randomUUID()

      val feeProfileToCreate = FeeProfileToCreate(
        feeType = FeeType("subscription_based"),
        userType = UserType("business"),
        tier = BusinessUserTiers.Small,
        subscription = CustomerSubscription("gold"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        flatAmount = Some(BigDecimal(10.00)),
        percentageAmount = None,
        ranges = None,
        createdAt = now,
        createdBy = "pegbuser")

      val expected = FeeProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        feeType = "subscription_based",
        userType = "business",
        tier = "basic",
        subscription = "gold",
        transactionType = "p2p_domestic",
        channel = Some("mobile_application"),
        provider = None,
        instrument = Some("visa_debit"),
        calculationMethod = "flat_fee",
        maxFee = None,
        minFee = None,
        feeAmount = Some(BigDecimal(10.00)),
        feeRatio = None,
        feeMethod = "add",
        taxIncluded = "no_tax",
        ranges = None,
        currencyCode = "AED",
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None,
        deletedAt = None)

      (currencyDao.getCurrenciesWithId _).when(Some(true))
        .returns(Right(List((1, "PHP"), (2, "USD"), (3, "KES"))))
      (typesDao.getFeeTypes _).when()
        .returns(Right(List((11, "subscription_based", None), (12, "transaction_based", None))))
      (typesDao.getCustomerTypes _).when()
        .returns(Right(List((13, "business", None), (14, "individual_user", None))))
      (typesDao.getCustomerSubscriptions _).when()
        .returns(Right(List((15, "standard", None), (16, "gold", None), (17, "platinum", None))))
      (typesDao.getTransactionTypes _).when()
        .returns(Right(List((18, "p2p_domestic", None), (19, "p2p_international", None))))
      (typesDao.getChannels _).when()
        .returns(Right(List((20, "mobile_application", None))))
      (typesDao.getInstruments _).when()
        .returns(Right(List((21, "visa_debit", None))))
      (currencyDao.getAllNames _).when()
        .returns(Right(Set("AED", "USD", "KES")))
      (typesDao.getFeeCalculationMethod _).when()
        .returns(Right(List(
          (25, "flat_fee", None),
          (26, "flat_percentages", None),
          (27, "staircase_flat_fee", None),
          (28, "staircase_flat_percentages", None))))
      (typesDao.getFeeMethods _).when()
        .returns(Right(List((29, "add", None), (30, "deduct", None))))

      (feeProfileDao.getFeeProfileByCriteria _).when(feeProfileToCreate.asDaoCriteria, None, None, None)
        .returns(Right(Nil))
      (feeProfileDao.insertFeeProfile _).when(feeProfileToCreate.asDao(1, None))
        .returns(Right(expected))

      val res = feeProfileMgmtService.createFeeProfile(feeProfileToCreate)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError("no active currency id found for code AED"))
      }
    }

  }

}
