package tech.pegb.backoffice.dao.transaction.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.transaction.dto.{TransactionCriteria, TransactionGroup, TransactionGroupings}
import tech.pegb.backoffice.dao.transaction.sql.TransactionGroupSqlDao

@ImplementedBy(classOf[TransactionGroupSqlDao])
trait TransactionGroupDao {

  def getTransactionGroups(criteria: TransactionCriteria, grouping: TransactionGroupings): DaoResponse[Seq[TransactionGroup]]

}
