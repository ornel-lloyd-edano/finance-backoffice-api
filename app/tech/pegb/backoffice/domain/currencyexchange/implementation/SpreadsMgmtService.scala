package tech.pegb.backoffice.domain.currencyexchange.implementation

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.dao.currencyexchange.abstraction.SpreadsDao
import tech.pegb.backoffice.dao.currencyexchange.dto
import tech.pegb.backoffice.dao.model.CriteriaField
import tech.pegb.backoffice.domain.currencyexchange.abstraction.{CurrencyExchangeManagement, SpreadsManagement}
import tech.pegb.backoffice.domain.currencyexchange.dto.{SpreadCriteria, SpreadToCreate}
import tech.pegb.backoffice.domain.currencyexchange.model.Spread
import tech.pegb.backoffice.domain.{BaseService, ServiceError, model}
import tech.pegb.backoffice.domain.account.abstraction.AccountManagement
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.backoffice.mapping.domain.dao.currencyexchange.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.currencyexchange.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future
import scala.util.Try

@Singleton
class SpreadsMgmtService @Inject() (
    executionContexts: WithExecutionContexts,
    currencyExchangeManagement: CurrencyExchangeManagement,
    accountManagement: AccountManagement,
    spreadsDao: SpreadsDao) extends SpreadsManagement with BaseService {

  implicit val ec = executionContexts.blockingIoOperations

  def getSpread(id: UUID): Future[ServiceResponse[Spread]] = {
    implicit val fakeRequestId: UUID = UUID.randomUUID() //TODO take from controller
    Future(spreadsDao.getSpread(id)).flatMap(
      _.fold(
        _.asDomainError.toLeft.toFuture,
        {
          case Some(spread) if spread.deletedAt.isEmpty ⇒
            currencyExchangeManagement.getCurrencyExchangeByUUID(spread.currencyExchangeUuid).map(
              _.right.flatMap(currencyExchange ⇒
                Try(spread.asDomain(currencyExchange)).fold(
                  error ⇒ Left(validationError(s"Corrupt data for spread id [$id]. Reason: ${error.getCleanedMessage}")),
                  spread ⇒ Right(spread))))
          case _ ⇒ Future.successful(Left(notFoundError(s"spread with id [$id] not found")))
        }))
  }

  def getSpreadByCriteria(criteria: SpreadCriteria, orderBy: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Spread]]] = {
    implicit val fakeRequestId = UUID.randomUUID() //TODO take from controller
    val excludeSoftDeleted = criteria.copy(isDeleted = Option(false))
    Future(spreadsDao.getSpreadsByCriteria(excludeSoftDeleted.asDao, orderBy.asDao, limit, offset))
      .flatMap(
        _.fold(
          _.asDomainError.toLeft.toFuture,
          spreads ⇒ {
            val results: Seq[Future[Option[Spread]]] = spreads.map(spread ⇒ {
              currencyExchangeManagement.getCurrencyExchangeByUUID(spread.currencyExchangeUuid)
                .map(_.fold(
                  error ⇒ {
                    logger.warn(s"unable to get currency exchange domain object for spread id [${spread.uuid}]")
                    None
                  },
                  currencyExchange ⇒ Option(spread.asDomain(currencyExchange))))
            })

            Future.sequence(results).map(_.flatten).map(Right(_))
          }))
  }

  def countSpreadByCriteria(criteria: SpreadCriteria): Future[ServiceResponse[Int]] = Future {
    val excludeSoftDeleted = criteria.copy(isDeleted = Option(false))
    spreadsDao.countSpreadsByCriteria(excludeSoftDeleted.asDao).asServiceResponse
  }

  def createSpread(spreadToCreate: SpreadToCreate)(implicit requestId: UUID): Future[ServiceResponse[Spread]] = {
    val spreadsCriteria = dto.SpreadCriteria(
      currencyExchangeId = Some(CriteriaField("currency_exchange_id", spreadToCreate.currencyExchangeId.toString)),
      transactionType = Some(spreadToCreate.transactionType.underlying),
      channel = spreadToCreate.channel.map(_.underlying),
      recipientInstitution = spreadToCreate.institution,
      isDeletedAtNotNull = Some(false))

    (for {
      matchingSpreadList ← EitherT(Future(spreadsDao.getSpreadsByCriteria(spreadsCriteria, None, None, None).asServiceResponse))
      createSpreadResult ← EitherT(
        matchingSpreadList.headOption
          .fold(Future(spreadsDao.createSpread(spreadToCreate.asDao).asServiceResponse))(_ ⇒ Future.successful(Left(
            ServiceError.duplicateError(
              s"Spread: (transaction_type: ${spreadToCreate.transactionType}, channel: ${spreadToCreate.channel}, institution: ${spreadToCreate.institution}) already exists for currency_exchange: ${spreadToCreate.currencyExchangeId}", requestId.toOption)))))
      currencyExchangeDomain ← EitherT(currencyExchangeManagement.getCurrencyExchangeByUUID(spreadToCreate.currencyExchangeId))
    } yield {
      createSpreadResult.asDomain(currencyExchangeDomain)
    }).value
  }
}
