package tech.pegb.backoffice.domain.commission.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import javax.inject.Singleton
import tech.pegb.backoffice.dao.commission.abstraction.CommissionProfileDao
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.commission.abstraction.CommissionProfileManagement
import tech.pegb.backoffice.domain.commission.dto.{CommissionProfileCriteria, CommissionProfileToCreate, CommissionProfileToUpdate}
import tech.pegb.backoffice.domain.commission.model.CommissionProfile
import tech.pegb.backoffice.domain.{BaseService, FieldValueValidation, ServiceError, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.commission.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.commission.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.Future

@Singleton
class CommissionProfileMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    val typesDao: TypesDao,
    currencyDao: CurrencyDao,
    dao: CommissionProfileDao)
  extends CommissionProfileManagement with BaseService with FieldValueValidation {

  implicit val ec = executionContexts.blockingIoOperations

  def createCommissionProfile(dto: CommissionProfileToCreate): Future[ServiceResponse[CommissionProfile]] = {
    (for {
      _ ← EitherT.fromEither[Future](dto.validate)
      existingProfiles ← EitherT.fromEither[Future](dao.getCommissionProfileByCriteria(dto.asDaoCriteria, None, None, None).asServiceResponse)
      _ ← EitherT.cond[Future](
        existingProfiles.isEmpty, (), validationError("Commission profile with same features already exists"))
      _ ← dto.channel.fold[EitherT[Future, ServiceError, String]](EitherT.rightT(""))(channel ⇒
        EitherT.fromEither[Future](validateFieldsFromKnownTypes(channel, "channel")))
      _ ← dto.instrument.fold[EitherT[Future, ServiceError, String]](EitherT.rightT(""))(i ⇒
        EitherT.fromEither[Future](validateFieldsFromKnownTypes(i, "instrument")))
      getCurrencyId ← currencyDao.getCurrenciesWithId(hasIsActiveFilter = true.some).map(_.find(_._2 == dto.currencyCode.getCurrencyCode))
        .fold[EitherT[Future, ServiceError, (Int, String)]](
          error ⇒ EitherT.leftT(error.asDomainError),
          r ⇒ EitherT.fromOption[Future](r, notFoundError(s"no currency id found for code ${dto.currencyCode}")))
      createResp ← EitherT.fromEither[Future](dao.insertCommissionProfile(dto.asDao(getCurrencyId._1)).asServiceResponse)
      domainDto ← EitherT.fromEither[Future](createResp.asDomain.toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert commission profile entity to domain: ${t.getMessage}")))
    } yield {
      domainDto
    }).value

  }

  def getCommissionProfile(id: UUID): Future[ServiceResponse[CommissionProfile]] = {
    (for {
      getResult ← EitherT(getCommissionProfileByCriteria(CommissionProfileCriteria(uuid = UUIDLike(id.toString).some), Nil, None, None))
      profile ← EitherT.fromOption[Future](getResult.headOption, notFoundError(s"CommissionProfile $id not found"))
      range ← EitherT.fromEither[Future](dao.getCommissionProfileRangeByCommissionId(profile.id).asServiceResponse)
    } yield {
      if (range.isEmpty) {
        profile
      } else {
        profile.addRanges(range.map(_.asDomain).some)
      }
    }).value
  }

  def getCommissionProfileByCriteria(
    criteriaDto: CommissionProfileCriteria,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[CommissionProfile]]] = Future {

    dao.getCommissionProfileByCriteria(
      criteria = criteriaDto.asDao,
      ordering = ordering.asDao,
      limit = limit,
      offset = offset).map(_.flatMap(_.asDomain.toOption)).asServiceResponse

  }

  def countCommissionProfileByCriteria(criteriaDto: CommissionProfileCriteria): Future[ServiceResponse[Int]] = Future {
    dao.countCommissionProfileByCriteria(criteriaDto.asDao).asServiceResponse
  }

  def updateCommissionProfile(id: UUID, dto: CommissionProfileToUpdate): Future[ServiceResponse[CommissionProfile]] = ???

  def deleteCommissionProfile(id: UUID, deletedBy: String, deletedAt: LocalDateTime, updatedAt: Option[LocalDateTime]): Future[ServiceResponse[CommissionProfile]] = ???

  def validateFieldsFromKnownTypes(fieldvalue: String, fieldName: String): ServiceResponse[String] = {
    val result = fieldName match {
      case "channel" ⇒ typesDao.getChannels.map(_.find(_._2 == fieldvalue).map(_._2))
      case "instrument" ⇒ typesDao.getInstruments.map(_.find(_._2 == fieldvalue).map(_._2))
      case _ ⇒ Right(Option.empty[String])
    }

    result.asServiceResponse
      .flatMap(_.toRight(ServiceError.validationError(s"provided element `$fieldvalue` is invalid for '$fieldName'")))

  }

}
