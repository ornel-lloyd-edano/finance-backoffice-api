package tech.pegb.backoffice.domain.aggregations

import java.time.LocalDateTime

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.test.Injecting
import tech.pegb.backoffice.dao.aggregations.dto.Entity
import tech.pegb.backoffice.dao.fee.abstraction.ThirdPartyFeeProfileDao
import tech.pegb.backoffice.dao.fee.entity.{ThirdPartyFeeProfile, ThirdPartyFeeProfileRange}
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionGroupDao
import tech.pegb.backoffice.dao.transaction.dto.TransactionGroup
import tech.pegb.backoffice.domain.aggregations.dto.TransactionGrouping
import tech.pegb.backoffice.domain.aggregations.implementation.ThirdPartyFeesExpressionCreator
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.mapping.domain.dao.aggregation.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class ThirdPartyFeesExpressionCreatorSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  lazy val dao = stub[ThirdPartyFeeProfileDao]
  lazy val txnGroupingDao = stub[TransactionGroupDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[ThirdPartyFeeProfileDao].to(dao),
      bind[TransactionGroupDao].to(txnGroupingDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val thirdPartyFeeExpressionCreator = inject[ThirdPartyFeesExpressionCreator]

  "ThirdPartyFeeExpressionCreator" should {
    "get flat fee expression in getThirdPartyFeesCalculationExpressionById" in {

      val mockId = "1"
      val mockResult = ThirdPartyFeeProfile(
        id = mockId,
        transactionType = None,
        provider = "pesalink",
        currencyCode = "KES",
        calculationMethod = "flat_fee",
        isActive = true,
        maxFee = None,
        minFee = None,
        feeAmount = BigDecimal("5.25").toOption,
        feeRatio = None,
        ranges = None,
        createdAt = LocalDateTime.now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None)

      (dao.getThirdPartyFeeProfile _).when(mockId).returns(Right(Option(mockResult)))

      val result = thirdPartyFeeExpressionCreator.getThirdPartyFeesCalculationExpressionById(Entity("transactions", Some("tx")), "amount", Some("third_party_fees"), mockId)

      val expected =
        s"""
           |5.25 as third_party_fees
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      whenReady(result) { result ⇒
        result.right.get mustBe expected
      }
    }

    "get flat fee expression in getThirdPartyFeesCalculationExpressionById (no alias)" in {

      val mockId = "1"
      val mockResult = ThirdPartyFeeProfile(
        id = mockId,
        transactionType = None,
        provider = "pesalink",
        currencyCode = "KES",
        calculationMethod = "flat_fee",
        isActive = true,
        maxFee = None,
        minFee = None,
        feeAmount = BigDecimal("5.25").toOption,
        feeRatio = None,
        ranges = None,
        createdAt = LocalDateTime.now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None)

      (dao.getThirdPartyFeeProfile _).when(mockId).returns(Right(Option(mockResult)))

      val result = thirdPartyFeeExpressionCreator.getThirdPartyFeesCalculationExpressionById(Entity("transactions", Some("tx")), "amount", None, mockId)

      val expected =
        s"""
           |5.25
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      whenReady(result) { result ⇒
        result.right.get mustBe expected
      }
    }

    "get flat percentage expression in getThirdPartyFeesCalculationExpressionById" in {

      val mockId = "1"
      val mockResult = ThirdPartyFeeProfile(
        id = mockId,
        transactionType = None,
        provider = "pesalink",
        currencyCode = "KES",
        calculationMethod = "flat_percentage",
        isActive = true,
        maxFee = None,
        minFee = None,
        feeAmount = None,
        feeRatio = BigDecimal("0.0075").toOption,
        ranges = None,
        createdAt = LocalDateTime.now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None)

      (dao.getThirdPartyFeeProfile _).when(mockId).returns(Right(Option(mockResult)))

      val result = thirdPartyFeeExpressionCreator.getThirdPartyFeesCalculationExpressionById(Entity("transactions", Some("tx")), "amount", Some("third_party_fees"), mockId)

      val expected =
        s"""
           |tx.amount * 0.0075 as third_party_fees
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      whenReady(result) { result ⇒
        result.right.get mustBe expected
      }
    }

    "get staircase flat fee expression in getThirdPartyFeesCalculationExpressionById" in {

      val mockId = "1"
      val mockResult = ThirdPartyFeeProfile(
        id = mockId,
        transactionType = None,
        provider = "pesalink",
        currencyCode = "KES",
        calculationMethod = "staircase_flat_fee",
        isActive = true,
        maxFee = None,
        minFee = None,
        feeAmount = None,
        feeRatio = None,
        ranges = Option(Seq(
          ThirdPartyFeeProfileRange(
            id = "1",
            thirdPartyFeeProfileId = "1",
            max = Some(BigDecimal("300")),
            min = Some(BigDecimal("100")),
            feeAmount = Some(BigDecimal("2.25")),
            feeRatio = None),
          ThirdPartyFeeProfileRange(
            id = "1",
            thirdPartyFeeProfileId = "1",
            max = Some(BigDecimal("500")),
            min = Some(BigDecimal("300")),
            feeAmount = Some(BigDecimal("3.25")),
            feeRatio = None),
          ThirdPartyFeeProfileRange(
            id = "1",
            thirdPartyFeeProfileId = "1",
            max = None,
            min = Some(BigDecimal("500")),
            feeAmount = Some(BigDecimal("4.25")),
            feeRatio = None))),
        createdAt = LocalDateTime.now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None)

      (dao.getThirdPartyFeeProfile _).when(mockId).returns(Right(Option(mockResult)))

      val result = thirdPartyFeeExpressionCreator.getThirdPartyFeesCalculationExpressionById(Entity("transactions", Some("tx")), "amount", Some("third_party_fees"), mockId)

      val expected =
        s"""
           |CASE
           |WHEN tx.amount >= 100 AND tx.amount <= 300 THEN 2.25
           |WHEN tx.amount > 300 AND tx.amount <= 500 THEN 3.25
           |WHEN tx.amount > 500 THEN 4.25
           |ELSE 0.0
           |END as third_party_fees
         """.stripMargin.trim

      whenReady(result) { result ⇒
        result.right.get mustBe expected
      }
    }

    "get staircase percentage fee expression in getThirdPartyFeesCalculationExpressionById" in {

      val mockId = "1"
      val mockResult = ThirdPartyFeeProfile(
        id = mockId,
        transactionType = None,
        provider = "pesalink",
        currencyCode = "KES",
        calculationMethod = "staircase_flat_percentage",
        isActive = true,
        maxFee = None,
        minFee = None,
        feeAmount = None,
        feeRatio = None,
        ranges = Option(Seq(
          ThirdPartyFeeProfileRange(
            id = "1",
            thirdPartyFeeProfileId = "1",
            max = Some(BigDecimal("300")),
            min = Some(BigDecimal("100")),
            feeAmount = None,
            feeRatio = Some(BigDecimal("0.0225"))),
          ThirdPartyFeeProfileRange(
            id = "1",
            thirdPartyFeeProfileId = "1",
            max = Some(BigDecimal("500")),
            min = Some(BigDecimal("300")),
            feeAmount = None,
            feeRatio = Some(BigDecimal("0.0325"))),
          ThirdPartyFeeProfileRange(
            id = "1",
            thirdPartyFeeProfileId = "1",
            max = None,
            min = Some(BigDecimal("500")),
            feeAmount = None,
            feeRatio = Some(BigDecimal("0.0425"))))),
        createdAt = LocalDateTime.now,
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None)

      (dao.getThirdPartyFeeProfile _).when(mockId).returns(Right(Option(mockResult)))

      val result = thirdPartyFeeExpressionCreator.getThirdPartyFeesCalculationExpressionById(Entity("transactions", Some("tx")), "amount", Some("third_party_fees"), mockId)

      val expected =
        s"""
           |CASE
           |WHEN tx.amount >= 100 AND tx.amount <= 300 THEN tx.amount * 0.0225
           |WHEN tx.amount > 300 AND tx.amount <= 500 THEN tx.amount * 0.0325
           |WHEN tx.amount > 500 THEN tx.amount * 0.0425
           |ELSE 0.0
           |END as third_party_fees
           """.stripMargin.trim

      whenReady(result) { result ⇒
        result.right.get mustBe expected
      }
    }

    "get nested fee calculation expression in getNestedThirdPartyFeesCalculationExpression" in {

      val mockTxnGroups = Seq(
        TransactionGroup(provider = Some("mpesa")),
        TransactionGroup(provider = Some("mpesa"), transactionType = Some("currency_exchange")),
        TransactionGroup(provider = Some("pesalink")))

      (txnGroupingDao.getTransactionGroups _)
        .when(
          TransactionCriteria.empty.asDao(isOtherPartyNotNull = Some(true)),
          TransactionGrouping(institution = true, transactionType = true, currencyCode = true).asDao2)
        .returns(Right(mockTxnGroups))

      val mockTPFeeProfile1 = ThirdPartyFeeProfile(
        id = "1",
        transactionType = None,
        provider = "mpesa",
        currencyCode = "KES",
        calculationMethod = "flat_fee",
        isActive = true,
        maxFee = None,
        minFee = None,
        feeAmount = Some(BigDecimal("10.50")),
        feeRatio = None,
        ranges = None,
        createdAt = LocalDateTime.now,
        createdBy = "tester",
        updatedAt = None,
        updatedBy = None)
      (dao.getThirdPartyFeeProfileByCriteria _)
        .when(("mpesa", None, None).asDao, None, None, None)
        .returns(Right(Seq(mockTPFeeProfile1)))

      val mockTPFeeProfile2 = ThirdPartyFeeProfile(
        id = "2",
        transactionType = None,
        provider = "mpesa",
        currencyCode = "KES",
        calculationMethod = "staircase_flat_fee",
        isActive = true,
        maxFee = None,
        minFee = None,
        feeAmount = None,
        feeRatio = None,
        ranges = Some(Seq(
          ThirdPartyFeeProfileRange("1", "2", min = Some(BigDecimal("120")), max = Some(BigDecimal("350")), feeAmount = Some(BigDecimal("0.25"))),
          ThirdPartyFeeProfileRange("2", "2", min = Some(BigDecimal("350")), max = Some(BigDecimal("600")), feeAmount = Some(BigDecimal("1.25"))),
          ThirdPartyFeeProfileRange("3", "2", min = Some(BigDecimal("600")), feeAmount = Some(BigDecimal("3.25"))))),
        createdAt = LocalDateTime.now,
        createdBy = "tester",
        updatedAt = None,
        updatedBy = None)
      (dao.getThirdPartyFeeProfileByCriteria _)
        .when(("mpesa", None, Some("currency_exchange")).asDao, None, None, None)
        .returns(Right(Seq(mockTPFeeProfile2)))

      val mockTPFeeProfile3 = ThirdPartyFeeProfile(
        id = "3",
        transactionType = None,
        provider = "pesalink",
        currencyCode = "KES",
        calculationMethod = "flat_percentage",
        isActive = true,
        maxFee = None,
        minFee = None,
        feeAmount = None,
        feeRatio = Some(BigDecimal("0.0275")),
        ranges = None,
        createdAt = LocalDateTime.now,
        createdBy = "tester",
        updatedAt = None,
        updatedBy = None)
      (dao.getThirdPartyFeeProfileByCriteria _)
        .when(("pesalink", None, None).asDao, None, None, None)
        .returns(Right(Seq(mockTPFeeProfile3)))

      val result = thirdPartyFeeExpressionCreator.getCompleteThirdPartyFeesCalculationNestedExpression(Entity("transactions", Some("tx")), "amount", Some("third_party_fees"))

      val expected =
        s"""
           |CASE
           |WHEN pr.name = 'mpesa' AND tx.type IS NULL AND c.currency_name IS NULL THEN
           |10.50
           |WHEN pr.name = 'mpesa' AND tx.type = 'currency_exchange' AND c.currency_name IS NULL THEN
           |CASE
           |WHEN tx.amount >= 120 AND tx.amount <= 350 THEN 0.25
           |WHEN tx.amount > 350 AND tx.amount <= 600 THEN 1.25
           |WHEN tx.amount > 600 THEN 3.25
           |ELSE 0.0
           |END
           |WHEN pr.name = 'pesalink' AND tx.type IS NULL AND c.currency_name IS NULL THEN
           |tx.amount * 0.0275
           |ELSE 0.0 END as third_party_fees
           """.stripMargin.trim

      whenReady(result) { result ⇒
        result.right.get mustBe expected
      }
    }
  }

}
