package tech.pegb.backoffice.domain.graphql

import com.google.inject.Inject
import tech.pegb.backoffice.dao.model.GroupingField
import tech.pegb.backoffice.domain.model.{CustomerAggregation, TransactionAggregatation}
import tech.pegb.backoffice.domain.transaction.model.Transaction
import tech.pegb.backoffice.util.{Logging, WithExecutionContexts}

import scala.concurrent.Future

class GraphQLRepo @Inject() (
    transactionsRepo: TransactionRepo,
    customerRepo: CustomersRepo,
    executionContexts: WithExecutionContexts) extends Logging {

  implicit val ec = executionContexts.genericOperations

  def aggregateTransactions(args: TransactionQueryArgs, groupings: Seq[GroupingField]): Future[Seq[TransactionAggregatation]] = {
    transactionsRepo.aggregate(args, groupings)
  }

  def aggregateCustomers(customerArgs: CustomerQueryArgs, trxArgs: TransactionQueryArgs, groupings: Seq[GroupingField]): Future[Seq[CustomerAggregation]] = {
    customerRepo.aggregate(customerArgs, trxArgs, groupings)
  }

  def transaction(id: String): Future[Option[Transaction]] = transactionsRepo.transaction(id)

  def transactions(args: TransactionQueryArgs): Future[Seq[Transaction]] = transactionsRepo.transactions(args)

  def sum(args: TransactionQueryArgs): Future[BigDecimal] = transactionsRepo.sum(args)

  def count(args: TransactionQueryArgs): Future[Int] = transactionsRepo.count(args)
}
