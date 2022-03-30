package tech.pegb.backoffice.domain.fee.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.core.integration.abstraction.FeeProfileCoreApiClient
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.fee.abstraction.FeeProfileDao
import tech.pegb.backoffice.dao.fee.entity
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.fee.abstraction.FeeProfileManagement
import tech.pegb.backoffice.domain.fee.dto._
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.FeeCalculationMethod._
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.TaxInclusionTypes.TaxInclusionStringAdapter
import tech.pegb.backoffice.domain.fee.model.{FeeProfile, FeeProfileRange}
import tech.pegb.backoffice.domain.{FieldValueValidation, ServiceError, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.fee.Implicits.{FeeProfileAdapter, FeeProfileRangeAdapter}
import tech.pegb.backoffice.mapping.domain.dao.Implicits.{UUIDConverter, _}
import tech.pegb.backoffice.mapping.domain.dao.fee.Implicits.{FeeProfileRangeToUpdateAdapter, FeeProfileToDeleteAdapter, FeeProfileToUpdateAdapter, _}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future
import scala.util.Try

@Singleton
class FeeProfileMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    val typesDao: TypesDao,
    currencyDao: CurrencyDao,
    dao: FeeProfileDao,
    apiClient: FeeProfileCoreApiClient)
  extends FeeProfileManagement with FeeProfileModelValidations with FieldValueValidation {

  implicit val ec = executionContexts.blockingIoOperations

  def createFeeProfile(createDto: FeeProfileToCreate)(implicit requestId: UUID): Future[ServiceResponse[FeeProfile]] = {

    (for {
      _ ← EitherT.fromEither[Future](createDto.validate)
      existingProfiles ← EitherT.fromEither[Future](dao.getFeeProfileByCriteria(createDto.asDaoCriteria, None, None, None).asServiceResponse)
      _ ← EitherT.cond[Future](
        existingProfiles.isEmpty, (), validationError("Fee profile with same features already exists"))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.feeType.underlying, "fee_type"))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.userType.underlying, "user_type"))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.subscription.underlying, "subscription_type"))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.transactionType.underlying, "transaction_type"))
      _ ← createDto.channel.fold[EitherT[Future, ServiceError, String]](EitherT.rightT(""))(channel ⇒
        EitherT.fromEither[Future](validateFieldsFromKnownTypes(channel.underlying, "channel")))
      _ ← createDto.instrument.fold[EitherT[Future, ServiceError, String]](EitherT.rightT(""))(i ⇒
        EitherT.fromEither[Future](validateFieldsFromKnownTypes(i, "instrument")))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.currencyCode.getCurrencyCode, "currency_code"))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.calculationMethod.underlying, "calculation_method"))
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.feeMethod.underlying, "fee_method"))
      getCurrencyId ← currencyDao.getCurrenciesWithId(hasIsActiveFilter = true.some).map(_.find(_._2 == createDto.currencyCode.getCurrencyCode))
        .fold[EitherT[Future, ServiceError, (Int, String)]](
          error ⇒ EitherT.leftT(error.asDomainError),
          r ⇒ EitherT.fromOption[Future](r, validationError(s"no active currency id found for code ${createDto.currencyCode}")))
      createFeeProfiles ← EitherT.fromEither[Future](dao.insertFeeProfile(createDto.asDao(getCurrencyId._1, None)).asServiceResponse)
    } yield {
      createFeeProfiles.asDomain
    }).value
  }

  @deprecated("not being used by front-end", "")
  def addFeeProfileRanges(id: UUID, ranges: Seq[FeeProfileRangeToCreate],
    addedBy: String, addedAt: LocalDateTime): Future[ServiceResponse[FeeProfile]] = Future {
    implicit val mockRequestId = UUID.randomUUID()

    val entityId = id.asEntityId
    for {
      daoFeeProfile ← dao.getFeeProfile(entityId).fold(
        err ⇒ Left(unknownError(err.message)),
        _.map(f ⇒ Right(f)).getOrElse(Left(notFoundError(s"Fee profile id [$id] not found"))))

      domainFeeProfile ← Try(daoFeeProfile.asDomain).toEither.leftMap(_ ⇒ dtoMappingError("unable to convert to domain FeeProfile"))

      _ ← FeeProfile.validateRangeAmount(Option(domainFeeProfile.ranges.getOrElse(Seq.empty) ++ ranges), domainFeeProfile.calculationMethod)

      _ ← FeeProfile.validateRange(Option(domainFeeProfile.ranges.getOrElse(Seq.empty) ++ ranges))

      _ ← dao.insertFeeProfileRange(
        entityId,
        ranges.map(_.asDao(
          Option(daoFeeProfile.id),
          domainFeeProfile.calculationMethod.isPercentageType))).asServiceResponse

      updatedFeeProfile ← dao.updateFeeProfile(
        entityId,
        domainFeeProfile.asDaoUpdateDto(addedBy, addedAt, None)).fold(
          err ⇒ Left(unknownError(err.message)),
          _.map(f ⇒ Right(f)).getOrElse(Left(notFoundError(s"Fee profile id [$id] not found"))))

      _ = apiClient.notifyFeeProfileUpdated(updatedFeeProfile.id) // fire-and-forget
    } yield updatedFeeProfile.asDomain
  }

  def getFeeProfile(id: UUID)(implicit requestId: UUID): Future[ServiceResponse[FeeProfile]] = {
    (for {
      feeProfileOption ← EitherT(Future(dao.getFeeProfile(id.asEntityId).asServiceResponse))
      feeProfile ← EitherT.fromOption[Future](
        feeProfileOption,
        ServiceError.notFoundError(s"Fee profile id [$id] not found", Some(requestId)))
      ranges ← EitherT({
        val getRangesF: Future[ServiceResponse[Option[Seq[entity.FeeProfileRange]]]] =
          if (feeProfile.calculationMethod.isStairCaseType) {
            Future(dao.getFeeProfileRangesByFeeProfileId(feeProfile.id.asEntityId).map(Option(_)).asServiceResponse)
          } else {
            Future.successful(Right(None))
          }

        getRangesF
      })
      validatedFeeProfile ← EitherT.fromEither[Future](
        Try(feeProfile.copy(ranges = ranges).asDomain).toEither
          .leftMap(t ⇒ ServiceError.validationError(t.getMessage)))
    } yield {
      validatedFeeProfile
    }).value
  }

  def getFeeProfileByCriteria(
    criteriaDto: FeeProfileCriteria,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[FeeProfile]]] = Future {
    dao.getFeeProfileByCriteria(
      criteria = criteriaDto.asDao,
      ordering = ordering.asDao,
      limit = limit,
      offset = offset).map(_.map(_.asDomain)).asServiceResponse
  }

  def countFeeProfileByCriteria(criteriaDto: FeeProfileCriteria): Future[ServiceResponse[Int]] = Future {
    dao.countFeeProfileByCriteria(criteriaDto.asDao).asServiceResponse
  }

  def updateFeeProfile(id: UUID, updateDto: FeeProfileToUpdate)(implicit requestId: UUID) = Future {
    val entityId = id.asEntityId
    for {
      _ ← validateFieldsFromKnownTypes(updateDto.calculationMethod.underlying, "calculation_method")
      _ ← validateFieldsFromKnownTypes(updateDto.feeMethod.underlying, "fee_method")
      _ ← updateDto.validate
      maybeExisting ← dao.getFeeProfile(entityId).asServiceResponse
      maybeUpdated ← dao.updateFeeProfile(entityId, updateDto.asDao(maybeExisting.map(_.id))).asServiceResponse
      updated ← maybeUpdated.toRight(notFoundError(s"Fee profile $id was not found"))
      _ = apiClient.notifyFeeProfileUpdated(updated.id) // fire-and-forget
    } yield updated.asDomain
  }

  def deleteFeeProfile(id: UUID, deletedBy: String, deletedAt: LocalDateTime, updatedAt: Option[LocalDateTime])(implicit requestId: UUID) = Future {
    val entityId = id.asEntityId
    for {
      maybeExisting ← dao.getFeeProfile(entityId).asServiceResponse
      existing ← maybeExisting.toRight(notFoundError(s"Fee profile $id was not found"))
      dto = deletedAt.asDao(
        calculationMethod = existing.calculationMethod,
        feeMethod = existing.feeMethod,
        mbTaxIncluded = existing.taxIncluded.toTaxInclusionType,
        updatedAt = deletedAt,
        updatedBy = deletedBy,
        lastUpdatedAt = updatedAt)
      maybeUpdated ← dao.updateFeeProfile(entityId, dto).asServiceResponse
      updated ← maybeUpdated.toRight(notFoundError(s"Fee profile $id was not found"))
      _ = apiClient.notifyFeeProfileUpdated(updated.id) // fire-and-forget
    } yield updated.asDomain
  }

  def updateFeeProfileRange(
    rangeId: Int,
    feeProfileId: UUID,
    updateDto: FeeProfileRangeToUpdate)(
    implicit
    requestId: UUID): Future[ServiceResponse[FeeProfileRange]] = Future {
    for {
      mbExistingProfile ← dao.getFeeProfile(feeProfileId.asEntityId).asServiceResponse
      existingProfile ← mbExistingProfile
        .toRight(notFoundError(s"Fee profile $feeProfileId was not found"))
      existingRange ← existingProfile.ranges.flatMap(_.find(_.id == rangeId))
        .toRight(notFoundError(s"Fee profile $feeProfileId doesn't have fee profile range $rangeId"))
      validatedDto ← Either.cond(
        existingRange.min.contains(updateDto.from) && existingRange.max == updateDto.to,
        updateDto,
        validationError("Min and max of range should exactly match"))
      mbUpdated ← dao.updateFeeProfileRange(rangeId, validatedDto.asDao).asServiceResponse
      updated ← mbUpdated
        .toRight(notFoundError(s"Fee profile range $rangeId was not found"))
      _ = apiClient.notifyFeeProfileUpdated(existingProfile.id) // fire-and-forget
    } yield updated.asDomain
  }

  def deleteFeeProfileRange(
    rangeId: Int,
    feeProfileId: UUID)(
    implicit
    requestId: UUID): Future[ServiceResponse[FeeProfileRange]] = Future {
    for {
      mbExistingProfile ← dao.getFeeProfile(feeProfileId.asEntityId).asServiceResponse
      existingProfile ← mbExistingProfile
        .toRight(notFoundError(s"Fee profile $feeProfileId was not found"))
      mbDeleted ← dao.deleteFeeProfileRange(rangeId).asServiceResponse
      deleted ← mbDeleted
        .toRight(notFoundError(s"Fee profile range $rangeId was not found"))
      // we should generate update event, not a typo
      _ = apiClient.notifyFeeProfileUpdated(existingProfile.id) // fire-and-forget
    } yield deleted.asDomain
  }

  def validateFieldsFromKnownTypes(
    fieldvalue: String,
    fieldName: String): ServiceResponse[String] = {
    //TODO
    /*
      can we have an implicit in types domain
      ex. getFieldValue
      that will abstract away .map(_.find(_._2 == fieldvalue).map(_._2))
      also use triple equals in that implicit by importing the implicit from tech.backoffice.util
     */
    val result = fieldName match {
      case "fee_type" ⇒ typesDao.getFeeTypes.map(_.find(_._2 == fieldvalue).map(_._2))
      case "user_type" ⇒ typesDao.getCustomerTypes.map(_.find(_._2 == fieldvalue).map(_._2))
      case "subscription_type" ⇒ typesDao.getCustomerSubscriptions.map(_.find(_._2 == fieldvalue).map(_._2))
      case "transaction_type" ⇒ typesDao.getTransactionTypes.map(_.find(_._2 == fieldvalue).map(_._2))
      case "channel" ⇒ typesDao.getChannels.map(_.find(_._2 == fieldvalue).map(_._2))
      case "instrument" ⇒ typesDao.getInstruments.map(_.find(_._2 == fieldvalue).map(_._2))
      case "currency_code" ⇒ currencyDao.getAllNames.map(_.find(_ == fieldvalue))
      case "calculation_method" ⇒ typesDao.getFeeCalculationMethod.map(_.find(_._2 == fieldvalue).map(_._2))
      case "fee_method" ⇒ typesDao.getFeeMethods.map(_.find(_._2 == fieldvalue).map(_._2))
      case _ ⇒ Right(Option.empty[String])
    }

    result.asServiceResponse
      .flatMap(_.toRight(ServiceError.validationError(s"provided element `$fieldvalue` is invalid for '$fieldName'")))
  }

}
