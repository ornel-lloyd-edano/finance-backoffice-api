package tech.pegb.backoffice.domain.currencyexchange.implementation

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.UUID

import cats.data.EitherT
import cats.instances.either._
import cats.instances.future._
import cats.syntax.apply._
import cats.syntax.either._
import com.google.inject.Inject
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.currencyexchange.abstraction.{CurrencyExchangeDao, SpreadsDao}
import tech.pegb.backoffice.dao.currencyexchange.dto
import tech.pegb.backoffice.dao.model.CriteriaField
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.currencyexchange.abstraction.CurrencyExchangeManagement
import tech.pegb.backoffice.domain.currencyexchange.dto.{CurrencyExchangeCriteria, SpreadCriteria, SpreadUpdateDto}
import tech.pegb.backoffice.domain.currencyexchange.model.{CurrencyExchange, Spread}
import tech.pegb.backoffice.domain.account.abstraction.AccountManagement
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.domain.{BaseService, model}
import tech.pegb.backoffice.mapping.dao.domain.account.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.currencyexchange.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.currencyexchange.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.core.integration.abstraction.CurrencyExchangeCoreApiClient
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao

import scala.concurrent.{ExecutionContext, Future}

class CurrencyExchangeMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    fxApiClient: CurrencyExchangeCoreApiClient,
    dao: CurrencyExchangeDao,
    spreadsDao: SpreadsDao,
    typesDao: TypesDao,
    currencyDao: CurrencyDao,
    accountManagement: AccountManagement,
    accountDao: AccountDao) extends CurrencyExchangeManagement with BaseService {

  private implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  def getCurrencyExchangeByCriteria(
    criteria: CurrencyExchangeCriteria,
    orderBy: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[CurrencyExchange]]] = {
    (for {
      currenyExchangeDaoModelList ← EitherT(Future(dao.getCurrencyExchangeByCriteria(
        criteria = criteria.asDao,
        orderBy = orderBy.map(_.asDao),
        limit = limit,
        offset = offset).asServiceResponse))
    } yield {
      currenyExchangeDaoModelList.flatMap(_.asDomain.toOption)
    }).value
  }

  def getCurrencyExchangeByUUID(uuid: UUID)(implicit requestId: UUID): Future[ServiceResponse[CurrencyExchange]] = {

    val criteria = dto.CurrencyExchangeCriteria(id = Some(CriteriaField("uuid", uuid.toString)))
    (for {
      currencyExchangeDaoOption ← EitherT.fromEither[Future] {
        dao.getCurrencyExchangeByCriteria(criteria = criteria, Nil, None, None)
          .map(_.headOption)
          .asServiceResponse
      }
      currencyExchangeDaoModel ← EitherT.fromOption[Future](currencyExchangeDaoOption, notFoundError(s"Currency Exchange for id $uuid is not found"))
      dailyAmount ← EitherT.fromEither[Future](dao.getDailyAmount(currencyExchangeDaoModel.targetCurrencyAccountId, currencyExchangeDaoModel.baseCurrencyAccountId).asServiceResponse)
      finalReturn ← EitherT.fromEither[Future] {
        currencyExchangeDaoModel.asDomain.toEither.leftMap(throwable ⇒ validationError(throwable.getMessage))
      }
    } yield {
      finalReturn.copy(dailyAmount = dailyAmount)
    }).value
  }

  def countCurrencyExchangeByCriteria(criteria: CurrencyExchangeCriteria): Future[ServiceResponse[Int]] = {
    Future(dao.countTotalCurrencyExchangeByCriteria(criteria.asDao).asServiceResponse)
  }

  override def activateFX(
    id: UUID,
    doneAt: LocalDateTime,
    doneBy: String,
    lastUpdatedAt: Option[LocalDateTime])(
    implicit
    requestId: UUID): Future[ServiceResponse[CurrencyExchange]] = {
    updateFX(id, doneAt, doneBy, "active", lastUpdatedAt)
  }

  override def deactivateFX(
    id: UUID,
    doneAt: LocalDateTime,
    doneBy: String,
    lastUpdatedAt: Option[LocalDateTime])(
    implicit
    requestId: UUID): Future[ServiceResponse[CurrencyExchange]] = {
    updateFX(id, doneAt, doneBy, "inactive", lastUpdatedAt)
  }

  def getCurrencyExchangeSpreads(id: UUID, criteria: SpreadCriteria, ordering: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]) = ???

  private def updateFX(
    id: UUID,
    doneAt: LocalDateTime,
    doneBy: String,
    status: String,
    lastUpdatedAt: Option[LocalDateTime])(
    implicit
    requestId: UUID): Future[ServiceResponse[CurrencyExchange]] = {
    (for {
      fx ← EitherT.fromEither[Future](dao.findById(id).asServiceResponse)
      dx ← EitherT.fromEither[Future] {
        currencyDao.isCurrencyActive(fx.baseCurrency)
          .validateBoolResp("Base currency is not active") *>
          currencyDao.isCurrencyActive(fx.currencyCode).validateBoolResp("Currency is not active")
      }
      maybeAccount ← EitherT.fromEither[Future] {
        accountDao.getAccount(fx.targetCurrencyAccountUuid.toString).asServiceResponse
      }
      account ← EitherT.fromEither[Future] {
        maybeAccount
          .toRight(notFoundError("Account for currency exchange was not found"))
          .flatMap(a ⇒ a.asDomain
            .toEither
            .leftMap { exc ⇒
              logger.warn(s"Cannot convert dao account ${a.uuid} to domain: ")
              unknownError(exc.getMessage)
            })
      }
      _ ← EitherT.fromEither[Future] {
        if (account.accountStatus.underlying == "active") {
          Right(())
        } else {
          Left(validationError("Status of account for currency exchange was not active"))
        }
      }
      walletApiResult ← EitherT.liftF(fxApiClient.batchUpdateFxStatus(Seq(fx.id), status,
        "Change status from back-office", doneBy, lastUpdatedAt))
      updatedFx ← EitherT.fromEither[Future] {
        if (walletApiResult.isRight) {
          dao.findById(id)
            .asServiceResponse
            .flatMap(_.asDomain
              .toEither
              .leftMap(exc ⇒ validationError(exc.toString)))
        } else {
          Left(unknownError("Error from wallet core api. Please check logs."))
        }

      }
    } yield updatedFx).value
  }

  override def updateSpread(dto: SpreadUpdateDto)(implicit requestId: UUID): Future[ServiceResponse[Spread]] = {
    (for {
      fx ← EitherT(getCurrencyExchangeByUUID(dto.currencyExchangeId))
      mbExistingSpread ← EitherT.fromEither[Future](spreadsDao.getSpread(dto.id).asServiceResponse)
      existingSpread ← EitherT.fromEither[Future](mbExistingSpread.toRight(notFoundError(s"Spread ${dto.id} doesn't exist")))
      updatedSpread ← EitherT.fromEither[Future](spreadsDao.update(dto.id, dto.asDao)
        .asServiceResponse
        .map(_.asDomain(fx)))
      _ = fxApiClient.notifySpreadUpdated(existingSpread.id)
    } yield updatedSpread).value
  }

  //TODO domain should receive doneAt:LocalDateTime
  override def deleteSpread(
    spreadId: UUID,
    fxId: UUID,
    doneAt: ZonedDateTime,
    doneBy: String,
    lastUpdatedAt: Option[LocalDateTime])(
    implicit
    requestId: UUID): Future[ServiceResponse[Spread]] = {
    (for {
      fx ← EitherT(getCurrencyExchangeByUUID(fxId))
      updatedSpread ← EitherT.fromEither[Future](spreadsDao.update(spreadId, dto.SpreadUpdateDto(
        spread = BigDecimal(0),
        updatedBy = doneBy,
        updatedAt = doneAt.toLocalDateTimeUTC,
        deletedAt = Some(doneAt.toLocalDateTimeUTC),
        lastUpdatedAt = lastUpdatedAt //TODO: update
      )).asServiceResponse
        .map(_.asDomain(fx)))
    } yield updatedSpread).value
  }

  def updateCurrencyExchangeStatus(id: Int, status: String): Future[ServiceResponse[Boolean]] = Future {
    dao.updateCurrencyExchangeStatus(id, status).asServiceResponse
  }

  def batchActivateFX(doneAt: LocalDateTime, doneBy: String)(implicit requestId: UUID): Future[ServiceResponse[Unit]] = {
    fxApiClient.batchUpdateFxStatus(Seq.empty, "active", "Change status from back-office", doneBy, None)
  }

  def batchDeactivateFX(doneAt: LocalDateTime, doneBy: String)(implicit requestId: UUID): Future[ServiceResponse[Unit]] = {
    fxApiClient.batchUpdateFxStatus(Seq.empty, "inactive", "Change status from back-office", doneBy, None)
  }
}
