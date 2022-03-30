package tech.pegb.backoffice.dao.auth.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.auth.dto.{PermissionCriteria, PermissionToInsert, PermissionToUpdate}
import tech.pegb.backoffice.dao.auth.entity.Permission
import tech.pegb.backoffice.dao.auth.sql.PermissionSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[PermissionSqlDao])
trait PermissionDao extends Dao {

  def insertPermission(dto: PermissionToInsert): DaoResponse[Permission]

  def getPermissionByCriteria(criteria: PermissionCriteria, ordering: Option[OrderingSet], limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[Permission]]

  def countPermissionByCriteria(criteria: PermissionCriteria): DaoResponse[Int]

  def updatePermission(id: String, dto: PermissionToUpdate): DaoResponse[Option[Permission]]

  def getPermissionIdsByScopeId(scopeId: String): DaoResponse[Seq[String]]

}
