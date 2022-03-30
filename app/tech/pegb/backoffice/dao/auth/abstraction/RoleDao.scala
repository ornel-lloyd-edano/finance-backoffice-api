package tech.pegb.backoffice.dao.auth.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao._
import tech.pegb.backoffice.dao.auth.dto.{RoleCriteria, RoleToCreate, RoleToUpdate}
import tech.pegb.backoffice.dao.auth.entity.Role
import tech.pegb.backoffice.dao.auth.sql.RoleSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[RoleSqlDao])
trait RoleDao {

  def createRole(dto: RoleToCreate): DaoResponse[Role]

  def updateRole(id: UUID, dto: RoleToUpdate): DaoResponse[Option[Role]]

  def countRolesByCriteria(criteria: Option[RoleCriteria]): DaoResponse[Int]

  def getRolesByCriteria(
    criteria: Option[RoleCriteria],
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[Role]]

}
