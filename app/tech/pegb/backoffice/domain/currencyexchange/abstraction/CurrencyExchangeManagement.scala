package tech.pegb.backoffice.domain.currencyexchange.abstraction

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.currencyexchange.dto.{CurrencyExchangeCriteria, SpreadUpdateDto}
import tech.pegb.backoffice.domain.currencyexchange.implementation.CurrencyExchangeMgmtService
import tech.pegb.backoffice.domain.currencyexchange.model.{CurrencyExchange, Spread}
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[CurrencyExchangeMgmtService])
trait CurrencyExchangeManagement {

  def getCurrencyExchangeByCriteria(
    criteria: CurrencyExchangeCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[CurrencyExchange]]]

  def getCurrencyExchangeByUUID(uuid: UUID)(implicit requestId: UUID): Future[ServiceResponse[CurrencyExchange]]

  def countCurrencyExchangeByCriteria(criteria: CurrencyExchangeCriteria): Future[ServiceResponse[Int]]

  def activateFX(
    id: UUID,
    doneAt: LocalDateTime,
    doneBy: String,
    lastUpdatedAt: Option[LocalDateTime])(
    implicit
    requestId: UUID): Future[ServiceResponse[CurrencyExchange]]

  def deactivateFX(
    id: UUID,
    doneAt: LocalDateTime,
    doneBy: String,
    lastUpdatedAt: Option[LocalDateTime])(
    implicit
    requestId: UUID): Future[ServiceResponse[CurrencyExchange]]

  def updateSpread(dto: SpreadUpdateDto)(implicit requestId: UUID): Future[ServiceResponse[Spread]]

  def deleteSpread(
    spreadId: UUID,
    fxId: UUID,
    doneAt: ZonedDateTime,
    doneBy: String,
    lastUpdatedAt: Option[LocalDateTime])(implicit requestId: UUID): Future[ServiceResponse[Spread]]

  def updateCurrencyExchangeStatus(id: Int, status: String): Future[ServiceResponse[Boolean]]

  def batchActivateFX(doneAt: LocalDateTime, doneBy: String)(implicit requestId: UUID): Future[ServiceResponse[Unit]]

  def batchDeactivateFX(doneAt: LocalDateTime, doneBy: String)(implicit requestId: UUID): Future[ServiceResponse[Unit]]
}
