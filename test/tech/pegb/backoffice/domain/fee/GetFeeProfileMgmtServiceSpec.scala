package tech.pegb.backoffice.domain.fee

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.test.Injecting
import tech.pegb.backoffice.dao.fee.abstraction.FeeProfileDao
import tech.pegb.backoffice.dao.fee.entity.{FeeProfile, FeeProfileRange}
import tech.pegb.backoffice.domain.{BaseService, ServiceError}
import tech.pegb.backoffice.domain.fee.dto.FeeProfileCriteria
import tech.pegb.backoffice.domain.fee.implementation.FeeProfileMgmtService
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.FeeType
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.TaxInclusionTypes.NoTax
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.fee.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.fee.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class GetFeeProfileMgmtServiceSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures with BaseService {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val feeProfileDao = stub[FeeProfileDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[FeeProfileDao].to(feeProfileDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val feeProfileMgmtService = inject[FeeProfileMgmtService]
  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val now = LocalDateTime.now(mockClock)

  val fpUuid1 = UUID.randomUUID()
  val fpUuid2 = UUID.randomUUID()
  val fpUuid3 = UUID.randomUUID()

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
    feeAmount = Some(BigDecimal(150)),
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
    tier = "basic",
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
    ranges = None,
    currencyCode = "USD",
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = Some(now),
    updatedBy = Some("george"),
    deletedAt = None)

  val r1 = FeeProfileRange(
    id = 1,
    feeProfileId = Some(3),
    max = Some(BigDecimal(1000)),
    min = Some(BigDecimal(0)),
    feeAmount = None,
    feeRatio = None)
  val r2 = FeeProfileRange(
    id = 2,
    feeProfileId = Some(3),
    max = Some(BigDecimal(5000)),
    min = Some(BigDecimal(1001)),
    feeAmount = None,
    feeRatio = None)
  val r3 = FeeProfileRange(
    id = 3,
    feeProfileId = Some(3),
    max = Some(BigDecimal(10000)),
    min = Some(BigDecimal(5001)),
    feeAmount = None,
    feeRatio = None)

  "FeeProfile countFeeProfileByCriteria" should {
    "return count based on criteria" in {
      val feeProfileCriteria = FeeProfileCriteria(
        feeType = Some(FeeType("transaction_based")))

      (feeProfileDao.countFeeProfileByCriteria _).when(feeProfileCriteria.asDao)
        .returns(Right(2))

      val result = feeProfileMgmtService.countFeeProfileByCriteria(feeProfileCriteria)

      whenReady(result) { actual ⇒
        actual mustBe Right(2)
      }
    }
  }

  "FeeProfile getFeeProfileByCriteria" should {
    "return list of feeProfiles statisfying query" in {
      val feeProfileCriteria = FeeProfileCriteria()

      feeProfileCriteria.asDao.taxIncluded.isEmpty mustBe true

      val ordering = Seq(Ordering("calculation_method", Ordering.ASCENDING), Ordering("fee_amount", Ordering.DESCENDING))

      (feeProfileDao.getFeeProfileByCriteria _).when(feeProfileCriteria.asDao, ordering.asDao, None, None)
        .returns(Right(Seq(fp2, fp1, fp3)))

      val result = feeProfileMgmtService.getFeeProfileByCriteria(feeProfileCriteria, ordering, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(fp2.asDomain, fp1.asDomain, fp3.asDomain))
      }
    }
    "return list of feeProfiles statisfying no_tax" in {
      val feeProfileCriteria = FeeProfileCriteria(taxInclusion = Some(NoTax))

      feeProfileCriteria.asDao.taxIncluded.get.value.contains("no_tax") mustBe true

      val ordering = Seq(Ordering("calculation_method", Ordering.ASCENDING), Ordering("fee_amount", Ordering.DESCENDING))

      (feeProfileDao.getFeeProfileByCriteria _).when(feeProfileCriteria.asDao, ordering.asDao, None, None)
        .returns(Right(Seq(fp2)))

      val result = feeProfileMgmtService.getFeeProfileByCriteria(feeProfileCriteria, ordering, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(fp2.asDomain))
      }
    }
  }

  "FeeProfile getFeeProfile" should {
    "return fee profile matching uuid" in {
      implicit val requestId = UUID.randomUUID()
      (feeProfileDao.getFeeProfile _).when(fpUuid1.asEntityId)
        .returns(Right(Some(fp1)))

      val result = feeProfileMgmtService.getFeeProfile(fpUuid1)

      whenReady(result) { actual ⇒
        actual mustBe Right(fp1.asDomain)
      }
    }
    "return fee profile matching uuid (staircase)" in {
      implicit val requestId = UUID.randomUUID()

      val range = Seq(r1, r2, r3)

      (feeProfileDao.getFeeProfile _).when(fpUuid3.asEntityId)
        .returns(Right(Some(fp3)))
      (feeProfileDao.getFeeProfileRangesByFeeProfileId _).when(fp3.id.asEntityId)
        .returns(Right(range))

      val result = feeProfileMgmtService.getFeeProfile(fpUuid3)

      whenReady(result) { actual ⇒
        actual mustBe Right(fp3.copy(ranges = Some(range)).asDomain)
      }
    }
    "return notFoundError when profile doesnt exist" in {
      implicit val requestId = UUID.randomUUID()
      val fakeUUID = UUID.randomUUID()
      (feeProfileDao.getFeeProfile _).when(fakeUUID.id.asEntityId)
        .returns(Right(None))

      val result = feeProfileMgmtService.getFeeProfile(fakeUUID)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.notFoundError(s"Fee profile id [$fakeUUID] not found", requestId.toOption))
      }
    }
  }

}
