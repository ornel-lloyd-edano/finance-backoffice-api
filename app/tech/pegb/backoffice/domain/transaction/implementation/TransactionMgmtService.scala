package tech.pegb.backoffice.domain.transaction.implementation

import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.core.integration.abstraction.TransactionsCoreApiClient
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.model.GroupingField
import tech.pegb.backoffice.dao.transaction.abstraction.{TransactionDao, TransactionReversalDao}
import tech.pegb.backoffice.domain.model.{Ordering, TransactionAggregatation}
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionManagement
import tech.pegb.backoffice.domain.transaction.dto.{TransactionCriteria, TxnCancellation, TxnReversal, _}
import tech.pegb.backoffice.domain.transaction.model.Transaction
import tech.pegb.backoffice.domain.{BaseService}
import tech.pegb.backoffice.mapping.api.domain.core.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.api.core.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TransactionMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    transactionDao: TransactionDao,
    txnReversalDao: TransactionReversalDao,
    accountDao: AccountDao,
    coreApiClient: TransactionsCoreApiClient) extends TransactionManagement with BaseService {

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  def getTransactionsByCriteria(
    criteria: TransactionCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Transaction]]] = Future {

    transactionDao.getTransactionsByCriteria(criteria.asDao(), orderBy.map(_.asDao), limit, offset)
      .fold(
        _.asDomainError.toLeft,
        transactions ⇒
          transactions.map(transaction ⇒ transaction.asDomain)
            .toList.sequence[Try, Transaction].toEither
            .leftMap { throwable ⇒
              logger.error(s"Error in getTransactionsByCriteria", throwable)
              dtoMappingError(
                s"""Failed to convert transaction entity to domain.
                   | Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}""".stripMargin)
            })
  }

  def countTransactionsByCriteria(criteria: TransactionCriteria): Future[ServiceResponse[Int]] = Future {
    transactionDao.countTotalTransactionsByCriteria(criteria.asDao()).leftMap(_.asDomainError)
  }

  def cancelTransaction(dto: TxnCancellation): Future[ServiceResponse[Seq[Transaction]]] = {
    //logger.info(s"cancelTransaction ${dto.toSmartString}")
    coreApiClient.cancelTransaction(dto.asCoreApi).map(_ ⇒
      transactionDao.getTransactionsByTxnId(dto.txnId).asServiceResponse(_.flatMap(_.asDomain.toOption)))
  }

  def revertTransaction(dto: TxnReversal): Future[ServiceResponse[Seq[Transaction]]] = {
    coreApiClient.revertTransaction(dto.asCoreApi).map(_.fold(
      error ⇒ unknownError(error.message).toLeft,
      reversalTxn ⇒ {
        transactionDao.getTransactionsByTxnId(reversalTxn.asDomain).fold(
          _.asDomainError.toLeft,
          _.flatMap(_.asDomain.toOption).toRight)
      }))
  }

  def getTxnReversalMetadata(reversedTxnId: String): ServiceResponse[Option[ReasonMetadata]] = {
    txnReversalDao.getTransactionReversalsByCriteriaById(reversedTxnId)
      .fold(
        _.asDomainError.toLeft,
        {
          case Some(result) ⇒
            Right(ReasonMetadata(result.reason, createdAt = result.createdAt, createdBy = result.createdBy).toOption)
          case None ⇒
            notFoundError(s"reversal transaction for id $reversedTxnId not found").toLeft
        })
  }

  def getTxnCancellationMetadata(cancelledTxnId: String): ServiceResponse[Option[ReasonMetadata]] = {
    Right(None)
  }

  override def sumTransactionsByCriteria(criteria: TransactionCriteria): Future[ServiceResponse[BigDecimal]] = ???

  override def aggregateTransactionByCriteriaAndPivots(criteria: TransactionCriteria, groupings: Seq[GroupingField]): Future[ServiceResponse[Seq[TransactionAggregatation]]] = ???

  override def getById(id: String): Future[ServiceResponse[Transaction]] = ???
}
