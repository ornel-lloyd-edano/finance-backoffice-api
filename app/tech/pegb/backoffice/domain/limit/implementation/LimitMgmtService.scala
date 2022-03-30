package tech.pegb.backoffice.domain.limit.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.core.integration.abstraction.LimitProfileCoreApiClient
import tech.pegb.backoffice.dao.Dao.UUIDEntityId
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.limit.abstraction.LimitProfileDao
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.limit.abstraction.{LimitManagement, LimitManagementValidation}
import tech.pegb.backoffice.domain.limit.dto.{LimitProfileCriteria, LimitProfileToCreate, LimitProfileToUpdate}
import tech.pegb.backoffice.domain.limit.model.LimitProfile
import tech.pegb.backoffice.domain.{FieldValueValidation, ServiceError, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.limit.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.limit.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class LimitMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    validationAbstraction: LimitManagementValidation,
    val typesDao: TypesDao,
    currencyDao: CurrencyDao,
    apiClient: LimitProfileCoreApiClient,
    limitProfileDao: LimitProfileDao) extends LimitManagement with FieldValueValidation {

  implicit val ec: ExecutionContext = executionContexts.blockingIoOperations

  //TODO pls add Try handling in limitProfile.asDomain
  //TODO pls use implicit tech.pegb.backoffice.mapping.domain.dao asEntityId instead of UUIDEntityId
  def getLimitProfile(id: UUID)(implicit requestId: UUID): Future[ServiceResponse[LimitProfile]] = Future {
    limitProfileDao.getLimitProfile(UUIDEntityId(id)).fold(
      _.asDomainError.toLeft,
      limitProfileOption ⇒ limitProfileOption match {
        case Some(limitProfile) if limitProfile.deletedAt.isEmpty ⇒ Right(limitProfile.asDomain)
        case _ ⇒ Left(notFoundError(s"LimitProfile with uuid ${id} not found"))
      })
  }

  //TODO pls add Try handling in _.asDomain
  def getLimitProfileByCriteria(criteria: LimitProfileCriteria, ordering: Seq[model.Ordering], limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[LimitProfile]]] = Future {
    limitProfileDao.getLimitProfileByCriteria(
      criteria = criteria.copy(isDeleted = Option(false)).asDao,
      ordering = ordering.asDao,
      limit = limit,
      offset = offset).map(_.map(_.asDomain)).asServiceResponse
  }

  def countLimitProfileByCriteria(criteria: LimitProfileCriteria): Future[ServiceResponse[Int]] = Future {
    limitProfileDao.countLimitProfileByCriteria(criteria.copy(isDeleted = Option(false)).asDao).asServiceResponse
  }

  //TODO change the line with find(_._2 == createDto.currencyCode.getCurrencyCode)  so that it returns Left(ServiceError) if currency was not found
  //so we avoid assigning an id=0 in getCurrencyId.map(_._1).getOrElse(0)
  def createLimitProfile(createDto: LimitProfileToCreate)(implicit requestId: UUID): Future[ServiceResponse[LimitProfile]] = {
    (for {
      _ ← createDto.interval
        .fold[EitherT[Future, ServiceError, String]](EitherT.pure(""))(t ⇒
          EitherT.fromEither[Future](
            validateFieldsFromKnownTypes(t.underlying, "interval")))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.userType.underlying, "user_type"))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.tier.underlying, "tier"))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.subscription.underlying, "subscription"))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.currencyCode.getCurrencyCode, "currency_code"))
      _ ← createDto.channel.fold[EitherT[Future, ServiceError, String]](EitherT.pure(""))(c ⇒
        EitherT.fromEither[Future](validateFieldsFromKnownTypes(c.underlying, "channel")))
      _ ← createDto.transactionType
        .fold[EitherT[Future, ServiceError, String]](EitherT.pure(""))(t ⇒
          EitherT.fromEither[Future](validateFieldsFromKnownTypes(t.underlying, "transaction_type")))
      _ ← createDto.instrument.fold[EitherT[Future, ServiceError, String]](EitherT.pure(""))(i ⇒
        EitherT.fromEither[Future](validateFieldsFromKnownTypes(i, "instrument")))
      existingProfiles ← EitherT.fromEither[Future](limitProfileDao.getLimitProfileByCriteria(createDto.asDaoCriteria).asServiceResponse)
      _ ← EitherT.cond[Future](
        existingProfiles.isEmpty, (), validationError("Limit profile with same features already exists"))
      matchingLimitProfiles ← EitherT.fromEither[Future](
        limitProfileDao.getLimitProfileByCriteria(createDto.asDaoCriteriaWithoutInterval).asServiceResponse)
      getCurrencyId ← currencyDao.getCurrenciesWithId(hasIsActiveFilter = true.some).map(_.find(_._2 == createDto.currencyCode.getCurrencyCode))
        .fold[EitherT[Future, ServiceError, (Int, String)]](
          error ⇒ EitherT.leftT(error.asDomainError),
          r ⇒ EitherT.fromOption[Future](r, validationError(s"no active currency id found for code ${createDto.currencyCode}")))
      createLimitProfiles ← EitherT(
        validationAbstraction.validateCurrentLimitWithExistingLimit(createDto.interval.map(_.asTimeInterval), createDto.maxIntervalAmount, createDto.maxCount, matchingLimitProfiles.map(_.asDomain))
          .fold(Future(limitProfileDao.insertLimitProfile(createDto.asDao(getCurrencyId._1)).asServiceResponse))(serviceError ⇒ Future.successful(Left(serviceError))))
      _ = apiClient.notifyLimitProfileUpdated(createLimitProfiles.id)
    } yield {
      createLimitProfiles.asDomain
    }).value
  }

  override def updateLimitProfileValues(
    id: UUID,
    updateDto: LimitProfileToUpdate)(
    implicit
    requestId: UUID): Future[ServiceResponse[LimitProfile]] = {
    val daoRespT = for {
      maybeExisting ← EitherT.fromEither[Future](limitProfileDao.getLimitProfile(id.asEntityId).asServiceResponse)
      existing ← EitherT.fromOption[Future](
        maybeExisting.map(_.asDomain),
        notFoundError(s"Profile $id was not found"))
      _ ← EitherT.fromEither[Future] {
        Try(LimitProfile.assertLimitProfileRequiredFields(
          existing.limitType,
          existing.interval,
          updateDto.maxIntervalAmount,
          updateDto.maxAmount,
          updateDto.minAmount,
          updateDto.maxCount,
          updateDto.maxBalanceAmount))
          .toEither
          .leftMap(exc ⇒ validationError(exc.getMessage))
      }
      matchingLimitProfiles ← EitherT.fromEither[Future](
        limitProfileDao.getLimitProfileByCriteria(existing.asDaoCriteriaWithoutInterval).asServiceResponse)
      maybeUpdated ← EitherT.fromEither[Future] {
        validationAbstraction.validateCurrentLimitWithExistingLimit(
          interval = existing.interval,
          maxIntervalAmount = updateDto.maxIntervalAmount,
          maxIntervalCount = updateDto.maxCount,
          matchingLimitProfiles = matchingLimitProfiles.filterNot(_.uuid == id).map(_.asDomain))
          .fold(limitProfileDao.updateLimitProfile(id.asEntityId, updateDto.asDao(existing.limitType)).asServiceResponse)(serviceError ⇒
            Left(serviceError))
      }
      updated ← EitherT.fromOption[Future](
        maybeUpdated,
        notFoundError(s"Profile $id was not found"))
      _ = apiClient.notifyLimitProfileUpdated(updated.id)
    } yield updated.asDomain
    daoRespT.value
  }

  override def deleteLimitProfile(
    id: UUID,
    deletedBy: String,
    deletedAt: LocalDateTime,
    updatedAt: Option[LocalDateTime])(
    implicit
    requestId: UUID): Future[ServiceResponse[LimitProfile]] = {
    val updateDto = (deletedBy, deletedAt, updatedAt)
    val daoRespT = for {
      maybeUpdated ← EitherT.fromEither[Future] {
        limitProfileDao.updateLimitProfile(id.asEntityId, updateDto.asDao).asServiceResponse
      }
      updated ← EitherT.fromOption[Future](
        maybeUpdated,
        notFoundError(s"Profile $id was not found"))
      _ = apiClient.notifyLimitProfileUpdated(updated.id)
    } yield updated.asDomain
    daoRespT.value
  }

  override def validateFieldsFromKnownTypes(
    fieldvalue: String,
    fieldName: String): ServiceResponse[String] = {
    val result = fieldName match {
      case "user_type" ⇒ typesDao.getCustomerTypes.map(_.find(_._2 == fieldvalue).map(_._2))
      case "tier" ⇒ typesDao.getCustomerTiers.map(_.find(_._2 == fieldvalue).map(_._2))
      case "subscription" ⇒ typesDao.getCustomerSubscriptions.map(_.find(_._2 == fieldvalue).map(_._2))
      case "transaction_type" ⇒ typesDao.getTransactionTypes.map(_.find(_._2 == fieldvalue).map(_._2))
      case "channel" ⇒ typesDao.getChannels.map(_.find(_._2 == fieldvalue).map(_._2))
      case "instrument" ⇒ typesDao.getInstruments.map(_.find(_._2 == fieldvalue).map(_._2))
      case "currency_code" ⇒ currencyDao.getAllNames.map(_.find(_ == fieldvalue))
      case "limit_type" ⇒ typesDao.getLimitTypes.map(_.find(_._2 == fieldvalue).map(_._2))
      case "interval" ⇒ typesDao.getTimeIntervalTypes.map(_.find(_._2 == fieldvalue).map(_._2))
      case _ ⇒ Right(Option.empty[String])
    }

    result.asServiceResponse
      .flatMap(_.toRight(ServiceError.validationError(s"provided element `$fieldvalue` is invalid ")))
  }
}
