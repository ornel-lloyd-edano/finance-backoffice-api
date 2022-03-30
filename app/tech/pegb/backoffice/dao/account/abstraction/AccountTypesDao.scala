package tech.pegb.backoffice.dao.account.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.account.dto.{AccountTypeToUpdate, AccountTypeToUpsert}
import tech.pegb.backoffice.dao.account.entity.AccountAttributes.AccountType
import tech.pegb.backoffice.dao.account.sql.AccountTypesSqlDao

@ImplementedBy(classOf[AccountTypesSqlDao])
trait AccountTypesDao extends Dao {
  def getAll: DaoResponse[Set[AccountType]]

  def update(id: Int, accountTypeToUpdate: AccountTypeToUpdate): DaoResponse[Option[AccountType]]

  def bulkUpsert(
    dto: Seq[AccountTypeToUpsert],
    createdAt: LocalDateTime,
    createdBy: String): DaoResponse[Seq[AccountType]]
}
