package tech.pegb.backoffice.domain.limit

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import tech.pegb.backoffice.core.integration.abstraction.LimitProfileCoreApiClient
import tech.pegb.backoffice.dao.Dao.EntityId
import tech.pegb.backoffice.dao.DaoError.PreconditionFailed
import tech.pegb.backoffice.dao.limit.abstraction.LimitProfileDao
import tech.pegb.backoffice.dao.limit.entity.{LimitProfile ⇒ DaoLimitProfile}
import tech.pegb.backoffice.dao.limit.dto.{LimitProfileCriteria, LimitProfileToUpdate ⇒ DaoLimitProfileToUpdate}
import tech.pegb.backoffice.dao.limit.sql.{LimitProfileHistorySqlDao, LimitProfileSqlDao}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.limit.abstraction.{LimitManagement, LimitManagementValidation}
import tech.pegb.backoffice.domain.limit.dto.LimitProfileToUpdate
import tech.pegb.backoffice.domain.limit.model._
import tech.pegb.backoffice.mapping.dao.domain.limit.Implicits.LimitProfileAdapter
import tech.pegb.backoffice.mapping.domain.dao.Implicits.UUIDConverter
import tech.pegb.backoffice.mapping.domain.dao.limit.Implicits.UpdateDtoAdapter
import tech.pegb.backoffice.util.Utils
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.PegBNoDbTestApp

import scala.concurrent.Future

class LimitMgmtSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val limitToUpdateUuid = UUID.randomUUID()
  private val limitToUpdateDto = LimitProfileToUpdate(
    maxIntervalAmount = Some(BigDecimal(5000)),
    maxAmount = Some(BigDecimal(999.99)),
    minAmount = None,
    maxCount = None,
    maxBalanceAmount = None,
    updatedBy = "LimitMgmtSpec",
    updatedAt = Utils.nowAsLocal(),
    lastUpdatedAt = None)
  private val limitToDeleteDaoDto = {
    val now = Utils.nowAsLocal()
    DaoLimitProfileToUpdate(
      deletedAt = Some(now),
      updatedBy = "LimitMgmtSpec",
      updatedAt = now,
      lastUpdatedAt = None)
  }
  private val limitDao: LimitProfileDao = stub[LimitProfileDao]
  private val limitHistoryDao: LimitProfileHistorySqlDao = stub[LimitProfileHistorySqlDao]

  private val limitManagementValidationAbstraction = stub[LimitManagementValidation]
  private val apiClient: LimitProfileCoreApiClient = stub[LimitProfileCoreApiClient]

  override def additionalBindings: Seq[Binding[_]] = {
    super.additionalBindings ++ Seq[Binding[_]](
      bind[LimitProfileDao].toInstance(limitDao),
      bind[LimitProfileHistorySqlDao].toInstance(limitHistoryDao),
      bind[LimitManagementValidation].toInstance(limitManagementValidationAbstraction),
      bind[LimitProfileCoreApiClient].toInstance(apiClient))
  }

  "LimitMgmt" should {
    val service = inject[LimitManagement]

    "update limit" in {
      val updatedLimitProfile = DaoLimitProfile(
        id = 1,
        uuid = limitToUpdateUuid,
        limitType = LimitType.TransactionBased.underlying,
        userType = "individual_user",
        tier = "tier",
        subscription = "standard",
        transactionType = Some("transaction-type"),
        channel = Some("channel"),
        provider = Some("other-party"),
        instrument = Some("instrument"),
        maxIntervalAmount = limitToUpdateDto.maxIntervalAmount,
        maxAmount = limitToUpdateDto.maxAmount,
        minAmount = limitToUpdateDto.minAmount,
        maxCount = limitToUpdateDto.maxCount,
        interval = Some(TimeIntervals.Daily.toString),
        currencyCode = "AED",
        deletedAt = None,
        createdBy = "unit-test",
        createdAt = limitToUpdateDto.updatedAt.minusSeconds(30L),
        updatedBy = Some(limitToUpdateDto.updatedBy),
        updatedAt = Some(limitToUpdateDto.updatedAt))

      implicit val requestId: UUID = UUID.randomUUID()
      (limitDao.getLimitProfile(_: EntityId))
        .when(limitToUpdateUuid.asEntityId)
        .returns(Right(Some(updatedLimitProfile)))
      (limitDao.getLimitProfileByCriteria _)
        .when(LimitProfileCriteria(
          limitType = CriteriaField(LimitProfileSqlDao.cLimitType, updatedLimitProfile.limitType).some,
          userType = CriteriaField(LimitProfileSqlDao.cUserType, updatedLimitProfile.userType).some,
          tier = CriteriaField(LimitProfileSqlDao.cTier, updatedLimitProfile.tier).some,
          subscription = CriteriaField(LimitProfileSqlDao.cSubscription, updatedLimitProfile.subscription).some,
          currencyCode = CriteriaField(LimitProfileSqlDao.cCurrencyName, updatedLimitProfile.currencyCode).some,
          channel = updatedLimitProfile.channel.map(CriteriaField(LimitProfileSqlDao.cChannel, _)),
          transactionType = updatedLimitProfile.transactionType.map(CriteriaField(LimitProfileSqlDao.cTransactionType, _)),
          instrument = updatedLimitProfile.instrument.map(CriteriaField(LimitProfileSqlDao.cInstrument, _)),
          provider = updatedLimitProfile.provider.map(o ⇒ CriteriaField(Provider.cName, o, MatchTypes.Exact)),
          isDeleted = CriteriaField(LimitProfileSqlDao.cDeletedAt, false).some), None, None, None)
        .returns(Right(Seq(updatedLimitProfile)))
      (limitManagementValidationAbstraction.validateCurrentLimitWithExistingLimit(_: Option[TimeInterval], _: Option[BigDecimal], _: Option[Int], _: Seq[LimitProfile])(_: UUID))
        .when(
          updatedLimitProfile.interval.map(TimeIntervalWrapper(_).asTimeInterval),
          updatedLimitProfile.maxIntervalAmount,
          updatedLimitProfile.maxCount,
          Nil,
          *)
        .returns(None)
      (limitDao.updateLimitProfile(_: EntityId, _: DaoLimitProfileToUpdate))
        .when(limitToUpdateUuid.asEntityId, limitToUpdateDto.asDao(LimitType.TransactionBased))
        .returns(Right(Some(updatedLimitProfile)))
      (apiClient.notifyLimitProfileUpdated(_: Int)(_: UUID))
        .when(updatedLimitProfile.id, requestId)
        .returns(Future.successful(Right(())))
      val resp = service.updateLimitProfileValues(limitToUpdateUuid, limitToUpdateDto)
      whenReady(resp) { limitOrError ⇒
        limitOrError mustBe Right(updatedLimitProfile.asDomain)
      }
    }

    "delete limit" in {
      val limitProfile = DaoLimitProfile(
        id = 1,
        uuid = limitToUpdateUuid,
        limitType = LimitType.TransactionBased.underlying,
        userType = "individual_user",
        tier = "tier",
        subscription = "standard",
        transactionType = Some("transaction-type"),
        channel = Some("channel"),
        provider = Some("other-party"),
        instrument = Some("instrument"),
        maxIntervalAmount = limitToUpdateDto.maxIntervalAmount,
        maxAmount = limitToUpdateDto.maxAmount,
        minAmount = limitToUpdateDto.minAmount,
        maxCount = limitToUpdateDto.maxCount,
        interval = Some(TimeIntervals.Daily.toString),
        currencyCode = "AED",
        deletedAt = None,
        createdBy = "unit-test",
        createdAt = limitToUpdateDto.updatedAt.minusSeconds(30L),
        updatedBy = Some(limitToUpdateDto.updatedBy),
        updatedAt = Some(limitToUpdateDto.updatedAt))
      implicit val requestId: UUID = UUID.randomUUID()
      (limitDao.updateLimitProfile(_: EntityId, _: DaoLimitProfileToUpdate))
        .when(limitToUpdateUuid.asEntityId, limitToDeleteDaoDto)
        .returns(Right(Some(limitProfile)))
      (apiClient.notifyLimitProfileUpdated(_: Int)(_: UUID))
        .when(limitProfile.id, requestId)
        .returns(Future.successful(Right(())))
      val resp = service.deleteLimitProfile(
        id = limitToUpdateUuid,
        deletedBy = limitToDeleteDaoDto.updatedBy,
        deletedAt = limitToDeleteDaoDto.updatedAt,
        updatedAt = limitToDeleteDaoDto.lastUpdatedAt)
      whenReady(resp) { limitOrError ⇒
        limitOrError mustBe Right(limitProfile.asDomain)
      }
    }

    "return error on update limit (precondition error)" in {
      val updatedLimitProfile = DaoLimitProfile(
        id = 1,
        uuid = limitToUpdateUuid,
        limitType = LimitType.TransactionBased.underlying,
        userType = "individual_user",
        tier = "tier",
        subscription = "standard",
        transactionType = Some("transaction-type"),
        channel = Some("channel"),
        provider = Some("other-party"),
        instrument = Some("instrument"),
        maxIntervalAmount = limitToUpdateDto.maxIntervalAmount,
        maxAmount = limitToUpdateDto.maxAmount,
        minAmount = limitToUpdateDto.minAmount,
        maxCount = limitToUpdateDto.maxCount,
        interval = Some(TimeIntervals.Daily.toString),
        currencyCode = "AED",
        deletedAt = None,
        createdBy = "unit-test",
        createdAt = limitToUpdateDto.updatedAt.minusSeconds(30L),
        updatedBy = Some(limitToUpdateDto.updatedBy),
        updatedAt = Some(limitToUpdateDto.updatedAt))
      implicit val requestId: UUID = UUID.randomUUID()

      val fakeLastUpdateTime = LocalDateTime.now()
      val updateDto = limitToUpdateDto.copy(lastUpdatedAt = Option(fakeLastUpdateTime))

      (limitDao.getLimitProfile(_: EntityId))
        .when(limitToUpdateUuid.asEntityId)
        .returns(Right(Some(updatedLimitProfile)))
      (limitDao.updateLimitProfile(_: EntityId, _: DaoLimitProfileToUpdate))
        .when(limitToUpdateUuid.asEntityId, updateDto.asDao(LimitType.TransactionBased))
        .returns(Left(PreconditionFailed(s"Update failed. Limit profile ${limitToUpdateUuid} has been modified by another process.")))
      (limitDao.getLimitProfileByCriteria _)
        .when(LimitProfileCriteria(
          limitType = CriteriaField(LimitProfileSqlDao.cLimitType, updatedLimitProfile.limitType).some,
          userType = CriteriaField(LimitProfileSqlDao.cUserType, updatedLimitProfile.userType).some,
          tier = CriteriaField(LimitProfileSqlDao.cTier, updatedLimitProfile.tier).some,
          subscription = CriteriaField(LimitProfileSqlDao.cSubscription, updatedLimitProfile.subscription).some,
          currencyCode = CriteriaField(LimitProfileSqlDao.cCurrencyName, updatedLimitProfile.currencyCode).some,
          channel = updatedLimitProfile.channel.map(CriteriaField(LimitProfileSqlDao.cChannel, _)),
          transactionType = updatedLimitProfile.transactionType.map(CriteriaField(LimitProfileSqlDao.cTransactionType, _)),
          instrument = updatedLimitProfile.instrument.map(CriteriaField(LimitProfileSqlDao.cInstrument, _)),
          provider = updatedLimitProfile.provider.map(o ⇒ CriteriaField(Provider.cName, o, MatchTypes.Exact)),
          isDeleted = CriteriaField(LimitProfileSqlDao.cDeletedAt, false).some), None, None, None)
        .returns(Right(Seq(updatedLimitProfile)))
      (limitManagementValidationAbstraction.validateCurrentLimitWithExistingLimit(_: Option[TimeInterval], _: Option[BigDecimal], _: Option[Int], _: Seq[LimitProfile])(_: UUID))
        .when(
          updatedLimitProfile.interval.map(TimeIntervalWrapper(_).asTimeInterval),
          updatedLimitProfile.maxIntervalAmount,
          updatedLimitProfile.maxCount,
          Nil,
          *)
        .returns(None)

      val resp = service.updateLimitProfileValues(limitToUpdateUuid, updateDto)
      whenReady(resp) { limitOrError ⇒
        limitOrError mustBe Left(ServiceError.staleResourceAccessError(s"Update failed. Limit profile ${limitToUpdateUuid} has been modified by another process.", requestId.toOption))
      }
    }

    "return error on delete limit (precondition error)" in {

      implicit val requestId: UUID = UUID.randomUUID()
      val fakeLastUpdateTime = LocalDateTime.now()
      val updateDto = limitToDeleteDaoDto.copy(lastUpdatedAt = Option(fakeLastUpdateTime))

      (limitDao.updateLimitProfile(_: EntityId, _: DaoLimitProfileToUpdate))
        .when(limitToUpdateUuid.asEntityId, updateDto)
        .returns(Left(PreconditionFailed(s"Update failed. Limit profile ${limitToUpdateUuid} has been modified by another process.")))

      val resp = service.deleteLimitProfile(
        id = limitToUpdateUuid,
        deletedBy = limitToDeleteDaoDto.updatedBy,
        deletedAt = limitToDeleteDaoDto.updatedAt,
        updatedAt = Option(fakeLastUpdateTime))
      whenReady(resp) { limitOrError ⇒
        limitOrError mustBe Left(ServiceError.staleResourceAccessError(s"Update failed. Limit profile ${limitToUpdateUuid} has been modified by another process.", requestId.toOption))
      }
    }
  }

}
