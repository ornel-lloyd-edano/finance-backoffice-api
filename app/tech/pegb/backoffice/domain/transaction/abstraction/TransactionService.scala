package tech.pegb.backoffice.domain.transaction.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.model.GroupingField
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.model.{Ordering, TransactionAggregatation}
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.domain.transaction.implementation.TransactionsReportingService
import tech.pegb.backoffice.domain.transaction.model.Transaction

import scala.concurrent.Future

@ImplementedBy(classOf[TransactionsReportingService])
trait TransactionService {

  def getTransactionsByCriteria(
    criteria: TransactionCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Transaction]]]

  def countTransactionsByCriteria(criteria: TransactionCriteria): Future[ServiceResponse[Int]]

  def sumTransactionsByCriteria(criteria: TransactionCriteria): Future[ServiceResponse[BigDecimal]]

  def aggregateTransactionByCriteriaAndPivots(
    criteria: TransactionCriteria,
    groupings: Seq[GroupingField]): Future[ServiceResponse[Seq[TransactionAggregatation]]]

  def getById(id: String): Future[ServiceResponse[Transaction]]
}
