package tech.pegb.backoffice.domain.fee

import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.{Binding, bind}
import tech.pegb.backoffice.core.integration.abstraction.FeeProfileCoreApiClient
import tech.pegb.backoffice.dao.Dao.EntityId
import tech.pegb.backoffice.dao.fee.abstraction.FeeProfileDao
import tech.pegb.backoffice.dao.fee.dto.{FeeProfileRangeToInsert ⇒ DaoFeeProfileRangeToInsert, FeeProfileRangeToUpdate ⇒ DaoFeeProfileRangeToUpdate, FeeProfileToUpdate ⇒ DaoFeeProfileToUpdate}
import tech.pegb.backoffice.dao.fee.entity.{FeeProfile ⇒ DaoFeeProfile, FeeProfileRange ⇒ DaoFeeProfileRange}
import tech.pegb.backoffice.domain.fee.abstraction.FeeProfileManagement
import tech.pegb.backoffice.domain.fee.dto.{FeeProfileRangeToCreate, FeeProfileRangeToUpdate, FeeProfileToUpdate}
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.{FeeCalculationMethod, FeeMethod, TaxInclusionTypes}

import scala.concurrent.Future
import tech.pegb.backoffice.mapping.dao.domain.fee.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits.UUIDConverter
import tech.pegb.core.PegBNoDbTestApp

class FeeProfileMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {
  private val feeDao: FeeProfileDao = stub[FeeProfileDao]

  // TODO: try bind not to sql child, but to generic dao
  // private val feeHistoryDao: FeeProfileHistorySqlDao = stub[FeeProfileHistorySqlDao]
  // private val typesDao: TypesDao = stub[TypesDao]
  private val apiClient: FeeProfileCoreApiClient = stub[FeeProfileCoreApiClient]

  override def additionalBindings: Seq[Binding[_]] = {
    super.additionalBindings ++ Seq(
      bind[FeeProfileDao].toInstance(feeDao),
      bind[FeeProfileCoreApiClient].toInstance(apiClient))
  }

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  "Fee profile service" should {
    val service = inject[FeeProfileManagement]
    val feeId = 90
    val feeUuid = UUID.randomUUID()
    val feeRangeId = 456
    val mockRequestLocalTime = mockRequestDate.toLocalDateTime
    val existing = DaoFeeProfile(
      id = feeId,
      uuid = feeUuid,
      feeType = "transaction_based",
      userType = "individual",
      tier = "basic",
      subscription = "standard",
      transactionType = "p2p_domestic",
      channel = Some("mobile_money"),
      provider = None,
      instrument = None,
      calculationMethod = "staircase_flat_percentages",
      maxFee = None,
      minFee = None,
      feeAmount = None,
      feeRatio = None,
      feeMethod = "add",
      taxIncluded = "tax_not_included",
      ranges = Some(Seq(
        DaoFeeProfileRange(
          id = feeRangeId,
          feeProfileId = Some(feeId),
          min = Some(BigDecimal(1)),
          max = Some(BigDecimal(300)),
          feeAmount = None,
          feeRatio = None))),
      currencyCode = "AED", //lets use a more common currency, apparently this currency is not known in JVM in Jenkins "BYN",
      createdAt = mockRequestDate.minusHours(6).toLocalDateTime,
      createdBy = getClass.getSimpleName,
      updatedAt = Some(mockRequestLocalTime),
      updatedBy = Some(mockRequestFrom),
      deletedAt = None)

    "update existing fee profile" in {
      val dto = FeeProfileToUpdate(
        calculationMethod = FeeCalculationMethod(existing.calculationMethod),
        feeMethod = FeeMethod(existing.feeMethod),
        taxInclusion = TaxInclusionTypes.TaxIncluded,
        maxFee = Some(BigDecimal(50)),
        minFee = Some(BigDecimal(2)),
        flatAmount = None,
        percentageAmount = None,
        ranges = Some(Seq(
          FeeProfileRangeToCreate(
            from = BigDecimal(5),
            to = Some(BigDecimal(30)),
            flatAmount = None,
            percentageAmount = Some(BigDecimal(0.3))))),
        updatedAt = mockRequestLocalTime,
        updatedBy = mockRequestFrom,
        lastUpdatedAt = None)
      val expectedDto = DaoFeeProfileToUpdate(
        calculationMethod = dto.calculationMethod.underlying,
        feeMethod = dto.feeMethod.underlying,
        taxIncluded = "tax_included",
        maxFee = dto.maxFee,
        minFee = dto.minFee,
        feeAmount = Some(null),
        feeRatio = Some(null),
        ranges = Some(Seq(
          DaoFeeProfileRangeToInsert(
            feeProfileId = Some(feeId),
            min = BigDecimal(5),
            max = Some(BigDecimal(30)),
            feeAmount = None,
            feeRatio = Option(BigDecimal(0.3))))),
        updatedAt = mockRequestLocalTime,
        updatedBy = mockRequestFrom,
        lastUpdatedAt = dto.lastUpdatedAt)
      val expectedUpdate = DaoFeeProfile(
        id = feeId,
        uuid = feeUuid,
        feeType = existing.feeType,
        userType = existing.userType,
        tier = "basic",
        subscription = existing.subscription,
        transactionType = existing.transactionType,
        channel = existing.channel,
        provider = existing.provider,
        instrument = existing.instrument,
        calculationMethod = dto.calculationMethod.underlying,
        maxFee = dto.maxFee,
        minFee = dto.minFee,
        feeAmount = dto.flatAmount,
        feeRatio = dto.percentageAmount,
        feeMethod = dto.feeMethod.underlying,
        taxIncluded = "tax_included",
        ranges = Some(Seq(
          DaoFeeProfileRange(
            id = feeRangeId,
            feeProfileId = Some(feeId),
            max = Some(BigDecimal(30)),
            min = Some(BigDecimal(5)),
            feeAmount = None,
            feeRatio = Option(BigDecimal(0.3))))),
        currencyCode = existing.currencyCode,
        createdAt = existing.createdAt,
        createdBy = existing.createdBy,
        updatedAt = Some(mockRequestLocalTime),
        updatedBy = Some(mockRequestFrom),
        deletedAt = None)
      val expectedOutcome = Right(expectedUpdate.asDomain)

      (typesDao.getFeeCalculationMethod _).when()
        .returns(Right(List(
          (25, "flat_fee", None),
          (26, "flat_percentages", None),
          (27, "staircase_flat_fee", None),
          (28, "staircase_flat_percentages", None))))
      (typesDao.getFeeMethods _).when()
        .returns(Right(List((29, "add", None), (30, "deduct", None))))
      (feeDao.getFeeProfile(_: EntityId))
        .when(feeUuid.asEntityId)
        .returns(Right(Some(existing)))
      (feeDao.updateFeeProfile(_: EntityId, _: DaoFeeProfileToUpdate))
        .when(feeUuid.asEntityId, expectedDto)
        .returns(Right(Some(expectedUpdate)))
      (apiClient.notifyFeeProfileUpdated(_: Int)(_: UUID))
        .when(feeId, *)
        .returns(Future.successful(Right(())))
        .once()

      whenReady(service.updateFeeProfile(feeUuid, dto)(mockRequestId)) { resp ⇒
        resp mustBe expectedOutcome
      }
    }

    "update existing fee profile range" in {
      val dto = FeeProfileRangeToUpdate(
        from = BigDecimal(1),
        to = Some(BigDecimal(300)),
        flatAmount = None,
        percentageAmount = Some(BigDecimal(5)))
      val expectedDto = DaoFeeProfileRangeToUpdate(
        max = dto.to,
        min = dto.from,
        feeAmount = None,
        feeRatio = Option(dto.percentageAmount.get))
      val existingRange = DaoFeeProfileRange(
        id = feeRangeId,
        feeProfileId = Some(feeId),
        max = Some(BigDecimal(300)),
        min = Some(BigDecimal(1)),
        feeAmount = None,
        feeRatio = Some(BigDecimal(5)))
      val expectedOutcome = Right(existingRange.asDomain)

      (feeDao.getFeeProfile(_: EntityId))
        .when(feeUuid.asEntityId)
        .returns(Right(Some(existing)))
      (feeDao.updateFeeProfileRange(_: Int, _: DaoFeeProfileRangeToUpdate))
        .when(feeRangeId, expectedDto)
        .returns(Right(Some(existingRange)))
      (apiClient.notifyFeeProfileUpdated(_: Int)(_: UUID))
        .when(feeId, *)
        .returns(Future.successful(Right(())))
        .once()

      whenReady(service.updateFeeProfileRange(feeRangeId, feeUuid, dto)(mockRequestId)) { resp ⇒
        resp mustBe expectedOutcome
      }
    }

    "delete existing fee profile" in {
      val expectedDto = DaoFeeProfileToUpdate(
        calculationMethod = existing.calculationMethod,
        feeMethod = existing.feeMethod,
        taxIncluded = "tax_not_included",
        ranges = None,
        deletedAt = Some(mockRequestLocalTime),
        updatedAt = mockRequestLocalTime,
        updatedBy = mockRequestFrom,
        lastUpdatedAt = None)
      val expectedOutcome = Right(existing.asDomain)

      (feeDao.getFeeProfile(_: EntityId))
        .when(feeUuid.asEntityId)
        .returns(Right(Some(existing)))
      (feeDao.updateFeeProfile(_: EntityId, _: DaoFeeProfileToUpdate))
        .when(feeUuid.asEntityId, expectedDto)
        .returns(Right(Some(existing)))
      (apiClient.notifyFeeProfileUpdated(_: Int)(_: UUID))
        .when(feeId, *)
        .returns(Future.successful(Right(())))
        .once()

      whenReady(service.deleteFeeProfile(feeUuid, mockRequestFrom, mockRequestLocalTime, None)(mockRequestId)) { resp ⇒
        resp mustBe expectedOutcome
      }
    }

    "delete existing fee profile range" in {
      val existingRange = DaoFeeProfileRange(
        id = feeRangeId,
        feeProfileId = Some(feeId),
        max = Some(BigDecimal(300)),
        min = Some(BigDecimal(1)),
        feeAmount = None,
        feeRatio = Some(BigDecimal(5)))
      val expectedOutcome = Right(existingRange.asDomain)

      (feeDao.getFeeProfile(_: EntityId))
        .when(feeUuid.asEntityId)
        .returns(Right(Some(existing)))
      (feeDao.deleteFeeProfileRange(_: Int)(_: UUID))
        .when(feeRangeId, *)
        .returns(Right(Some(existingRange)))
      (apiClient.notifyFeeProfileUpdated(_: Int)(_: UUID))
        .when(feeId, *)
        .returns(Future.successful(Right(())))
        .once()

      whenReady(service.deleteFeeProfileRange(feeRangeId, feeUuid)(mockRequestId)) { resp ⇒
        resp mustBe expectedOutcome
      }
    }

    "add fee profile range" in {
      val mockId = UUID.randomUUID()

      import tech.pegb.backoffice.mapping.domain.dao.fee.Implicits._

      val existingFeeProfile = DaoFeeProfile(
        id = 1,
        uuid = mockId,
        feeType = "transaction_based",
        userType = "individual_user",
        tier = "basic",
        subscription = "standard",
        transactionType = "p2p_domestic",
        channel = Some("mobile_application"),
        provider = None,
        instrument = Some("visa_debit"),
        calculationMethod = "staircase_flat_percentage",
        maxFee = None,
        minFee = None,
        feeAmount = None,
        feeRatio = None,
        feeMethod = "add",
        taxIncluded = "tax_included",
        ranges = Option(
          Seq(
            DaoFeeProfileRange(id = 1, feeProfileId = Option(1),
              min = Option(BigDecimal(20)), max = Option(BigDecimal(50)), feeAmount = None,
              feeRatio = Option(BigDecimal(0.5))),

            DaoFeeProfileRange(id = 2, feeProfileId = Option(1),
              min = Option(BigDecimal(50)), max = Option(BigDecimal(99)), feeAmount = None,
              feeRatio = Option(BigDecimal(1.25))))),
        currencyCode = "AED",
        createdAt = LocalDateTime.now,
        createdBy = "pegbuser",
        updatedAt = Some(mockRequestLocalTime),
        updatedBy = Some("george"),
        deletedAt = None)

      (feeDao.getFeeProfile _).when(mockId.asEntityId).returns(Right(Some(existingFeeProfile)))

      val inputRanges = Seq(
        FeeProfileRangeToCreate.empty.copy(
          from = BigDecimal(99), to = Option(BigDecimal(500)), percentageAmount = Option(BigDecimal(2.5))),

        FeeProfileRangeToCreate.empty.copy(
          from = BigDecimal(500), to = Option(BigDecimal(1000)), percentageAmount = Option(BigDecimal(10.5))),

        FeeProfileRangeToCreate.empty.copy(
          from = BigDecimal(1000), to = Option(BigDecimal(5000)), percentageAmount = Option(BigDecimal(20.5))))

      val expectedRanges: Seq[DaoFeeProfileRangeToInsert] = inputRanges.map(_.asDao(Some(existingFeeProfile.id), false))

      var lastRangeId = existingFeeProfile.ranges.map(_.last.id).get
      val feeProfileRanges: Seq[DaoFeeProfileRange] = expectedRanges.map(r ⇒ {
        lastRangeId = lastRangeId + 1
        DaoFeeProfileRange(id = lastRangeId, feeProfileId = r.feeProfileId,
          max = r.max, min = Some(r.min), feeAmount = r.feeAmount,
          feeRatio = r.feeRatio)
      })

      (feeDao.insertFeeProfileRange(_: EntityId, _: Seq[DaoFeeProfileRangeToInsert])(_: Option[Connection]))
        .when(mockId.asEntityId, expectedRanges, None).returns(Right(feeProfileRanges))

      val doneBy = "analyn"
      val doneAt = LocalDateTime.now
      val updatedFeeProfile: DaoFeeProfile = existingFeeProfile.copy(
        ranges = Option(existingFeeProfile.ranges.getOrElse(Seq.empty) ++ feeProfileRanges.map(r ⇒ {
          DaoFeeProfileRange(id = r.id, Some(existingFeeProfile.id),
            min = r.min, max = r.max, feeAmount = r.feeAmount, feeRatio = r.feeRatio)
        })),
        updatedBy = Some(doneBy),
        updatedAt = Some(doneAt))

      (feeDao.updateFeeProfile _)
        .when(mockId.asEntityId, existingFeeProfile.asDomain.asDaoUpdateDto(doneBy, doneAt, None))
        .returns(Right(Some(updatedFeeProfile)))

      whenReady(service.addFeeProfileRanges(mockId, inputRanges, doneBy, doneAt)) { resp ⇒
        resp mustBe Right(updatedFeeProfile.asDomain)
      }

    }
  }
}
