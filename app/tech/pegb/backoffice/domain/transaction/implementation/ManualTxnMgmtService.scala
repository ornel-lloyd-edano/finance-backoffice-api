package tech.pegb.backoffice.domain.transaction.implementation

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.core.integration.abstraction.ManualTransactionCoreApiClient
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.transaction.abstraction.SettlementDao
import tech.pegb.backoffice.domain.{BaseService, ServiceError}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.abstraction.ManualTransactionManagement
import tech.pegb.backoffice.domain.transaction.dto.{ManualTxnCriteria, ManualTxnToCreate, SettlementFxHistoryCriteria, SettlementRecentAccountCriteria}
import tech.pegb.backoffice.domain.transaction.model.{ManualTransaction, SettlementFxHistory, SettlementRecentAccount}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.api.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class ManualTxnMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    settlementsDao: SettlementDao,
    currencyDao: CurrencyDao,
    accountsDao: AccountDao,
    coreApiClient: ManualTransactionCoreApiClient)
  extends ManualTransactionManagement with BaseService {

  implicit val ec = executionContexts.genericOperations

  def getManualTransactionsByCriteria(
    isGrouped: Boolean,
    criteria: ManualTxnCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[ManualTransaction]]] = Future {

    settlementsDao.getSettlementsByCriteria(criteria.asDao, orderBy.asDao, limit, offset)
      .asServiceResponse(results ⇒ {
        val mappedResults = results.flatMap(result ⇒ result.asDomain.toOption)
        if (results.size > mappedResults.size) {
          logger.warn(s"DTO mapping error in getManualTransactionsByCriteria. " +
            s"Some entities from the db was not parsed. Query: limit=${limit}, offset=${offset}, criteria= ${criteria.toSmartString}")
        }
        mappedResults
      })

  }

  def countManualTransactionsByCriteria(
    isGrouped: Boolean,
    criteria: ManualTxnCriteria): Future[ServiceResponse[Int]] = Future {

    settlementsDao.countSettlementsByCriteria(criteria.asDao).asServiceResponse
  }

  def createManualTransactions(manualTxn: ManualTxnToCreate): Future[ServiceResponse[ManualTransaction]] = {

    val manualTxnLines = manualTxn.manualTxnLines

    (for {
      _ ← EitherT.fromEither[Future](manualTxn.validate)

      accountNumberWithIdAndCurrency ← EitherT.fromEither[Future](accountsDao.getAccountsByCriteria(criteria = Some(manualTxn.manualTxnLines.map(_.accountNumber).asDao))
        .map(_.map(account ⇒ (account.accountNumber, (account.id, account.currency))).toMap)
        .asServiceResponse)

      _ ← {
        logger.debug("validating accounts and currencies")
        EitherT.cond[Future](manualTxnLines.forall { line ⇒
          line.currency.getCurrencyCode == accountNumberWithIdAndCurrency(line.accountNumber)._2
        }, (), ServiceError.validationError("An account number doesnt match it's currency in the Transaction Lines"))
      }

      manualTxnLinesToCreateForCoreApi ← EitherT.fromEither[Future](manualTxn.asApi(accountNumberWithIdAndCurrency.map(x ⇒ (x._1 → x._2._1))).toEither
        .leftMap(_ ⇒ ServiceError.dtoMappingError("Unable to map domain manual txn lines to core api dto", UUID.randomUUID().toOption)))

      currencyCodeAndId ← EitherT.fromEither[Future](currencyDao.getCurrenciesWithId().map(_.toMap.map(_.swap)).leftMap(_.asDomainError))

      manualTxnCreated ← EitherT.fromEither[Future]({
        val mappedManualTxnLines = manualTxnLines.flatMap(line ⇒ {
          Try {
            val (accountId, currency) = accountNumberWithIdAndCurrency(key = line.accountNumber)
            val currencyId = currencyCodeAndId(key = currency)
            (currencyId, accountId)
          } match {
            case Success((currencyId, accountId)) ⇒
              Some(line.asDao(accountId, currencyId))
            case Failure(error) ⇒
              None
          }
        })

        if (mappedManualTxnLines.size < manualTxnLines.size) {
          Left(ServiceError.dtoMappingError("Unable to map 1 or more manual transaction lines to settlement_lines because either currency code or account number was not found", UUID.randomUUID().toOption))
        } else {
          val settlementToInsert = manualTxn.asDao(currencyCodeAndId, settlementLines = mappedManualTxnLines)
          settlementsDao.insertSettlement(settlementToInsert)
            .fold(
              _.asDomainError.toLeft,
              daoResult ⇒ daoResult.asDomain.toEither.fold(
                error ⇒ {
                  logger.error("Error in createManualTransactions", error)
                  Left(ServiceError.dtoMappingError("Error in createManualTransactions. Unable to map dao entity to domain model", UUID.randomUUID().toOption))
                },
                result ⇒ Right((result, daoResult.id))))
        }

      })

      walletCoreApiResult ← EitherT(
        coreApiClient.createManualTransaction(manualTxnCreated._2, manualTxnLinesToCreateForCoreApi, manualTxn.manualTxnFxDetails.isDefined)
          .map(_.leftMap(error ⇒ ServiceError.externalServiceError(s"Wallet Core API failed. Reason: ${error.message}", UUID.randomUUID().toOption))))

    } yield {

      manualTxnCreated._1

    }).value
  }

  def countSettlementFxHistory(criteria: SettlementFxHistoryCriteria): Future[ServiceResponse[Int]] = Future {
    settlementsDao.countSettlementFxHistory(criteria.asDao).asServiceResponse
  }

  def getSettlementFxHistory(
    criteria: SettlementFxHistoryCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[SettlementFxHistory]]] = Future {
    settlementsDao.getSettlementFxHistory(criteria.asDao, orderBy.asDao, limit, offset)
      .map(_.map(_.asDomain))
      .asServiceResponse
  }

  def getSettlementRecentAccount(
    criteria: SettlementRecentAccountCriteria,
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[SettlementRecentAccount]]] = Future {
    settlementsDao.getSettlementRecentAccounts(criteria.asDao, limit, offset)
      .map(_.map(_.asDomain))
      .asServiceResponse
  }
}
