package tech.pegb.backoffice.domain.transaction.implementation

import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.core.integration.abstraction.TransactionsCoreApiClient
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.model.GroupingField
import tech.pegb.backoffice.dao.transaction.abstraction.{TransactionDao, TransactionReversalDao}
import tech.pegb.backoffice.domain.model.TransactionAggregatation
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionService
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.domain.transaction.model.Transaction
import tech.pegb.backoffice.domain.{BaseService}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future

class TransactionsReportingService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    accountDao: AccountDao,
    transactionDao: TransactionDao, //FIXME switch for TransactionReportingSqlDao, pending PWDB-235)
    txnReversalDao: TransactionReversalDao,
    coreApiClient: TransactionsCoreApiClient)
  extends TransactionMgmtService(
    conf = conf,
    executionContexts = executionContexts,
    userDao = userDao,
    accountDao = accountDao,
    transactionDao = transactionDao,
    txnReversalDao = txnReversalDao,
    coreApiClient = coreApiClient) with TransactionService with BaseService {

  override def getById(id: String): Future[ServiceResponse[Transaction]] = {
    Future {
      transactionDao.getTransactionsByUniqueId(id).fold(
        _.asDomainError.toLeft,
        transaction ⇒ {
          transaction.fold[ServiceResponse[Transaction]](
            Left(notFoundError(s"Transaction with id ${id} not found."))) {
              _.asDomain.toEither
                .leftMap { throwable ⇒
                  logger.error(s"Error in getTransactionByUniqueId", throwable)
                  dtoMappingError(s"Failed to convert transaction entity to domain. Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}")
                }
            }
        })
    }
  }

  override def sumTransactionsByCriteria(criteria: TransactionCriteria): Future[ServiceResponse[BigDecimal]] = Future {
    transactionDao.sumTotalTransactionsByCriteria(criteria.asDao()).leftMap(_.asDomainError)
  }

  override def aggregateTransactionByCriteriaAndPivots(
    criteria: TransactionCriteria,
    groupings: Seq[GroupingField]): Future[ServiceResponse[Seq[TransactionAggregatation]]] = {
    Future {
      transactionDao
        .aggregateTransactionByCriteriaAndPivots(criteria.asDao(), groupings)
        .map(_.flatMap(_.asDomain.toOption))
        .asServiceResponse
    }
  }
}
