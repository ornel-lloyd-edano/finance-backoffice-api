package tech.pegb.backoffice.dao.makerchecker.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.makerchecker.dto.BackofficeUserContact
import tech.pegb.backoffice.dao.makerchecker.sql.GetBackofficeUsersContactsSqlDao

@ImplementedBy(classOf[GetBackofficeUsersContactsSqlDao])
trait GetBackofficeUsersContactsDao extends Dao {

  def getBackofficeUsersContactsByRoleLvlAndBusinessUnit(lessThanOrEqualThisRoleLevel: Int, businessUnit: String): DaoResponse[Seq[BackofficeUserContact]]

}
