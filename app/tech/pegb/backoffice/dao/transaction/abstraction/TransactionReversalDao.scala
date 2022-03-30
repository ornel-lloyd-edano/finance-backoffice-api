package tech.pegb.backoffice.dao.transaction.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.transaction.entity.TransactionReversal
import tech.pegb.backoffice.dao.transaction.sql.TransactionReversalSqlDao

@ImplementedBy(classOf[TransactionReversalSqlDao])
trait TransactionReversalDao extends Dao {

  def getTransactionReversalsByCriteriaById(reversedTransactionId: String): DaoResponse[Option[TransactionReversal]]

}
