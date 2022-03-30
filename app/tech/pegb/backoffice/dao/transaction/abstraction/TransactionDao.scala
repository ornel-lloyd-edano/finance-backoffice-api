package tech.pegb.backoffice.dao.transaction.abstraction

import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.model.{GroupingField, Ordering}
import tech.pegb.backoffice.dao.transaction.dto.{TransactionAggregation, TransactionCriteria}
import tech.pegb.backoffice.dao.transaction.entity.Transaction

trait TransactionDao extends Dao {
  def aggregateTransactionByCriteriaAndPivots(
    criteria: TransactionCriteria,
    groupings: Seq[GroupingField]): DaoResponse[Seq[TransactionAggregation]]

  def countTotalTransactionsByCriteria(criteria: TransactionCriteria): DaoResponse[Int]

  def getTransactionsByCriteria(criteria: TransactionCriteria, orderBy: Seq[Ordering] = Nil, limit: Option[Int] = None, offset: Option[Int] = None): DaoResponse[Seq[Transaction]]

  def getTransactionsByTxnId(txnId: String): DaoResponse[Seq[Transaction]]

  def getOnFlyAggregation(criteria: TransactionCriteria, isLiability: Boolean): DaoResponse[Option[(BigDecimal, BigDecimal, BigDecimal)]]

  def getTransactionsByUniqueId(uniqueId: String): DaoResponse[Option[Transaction]]

  def sumTotalTransactionsByCriteria(criteria: TransactionCriteria): DaoResponse[BigDecimal]

}
