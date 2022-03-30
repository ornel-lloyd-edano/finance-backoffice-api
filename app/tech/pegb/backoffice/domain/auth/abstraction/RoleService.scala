package tech.pegb.backoffice.domain.auth.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.auth.implementation.{RoleService ⇒ RoleServiceT}
import tech.pegb.backoffice.domain.auth.dto.{RoleCriteria, RoleToCreate, RoleToUpdate}
import tech.pegb.backoffice.domain.auth.model.Role
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[RoleServiceT])
trait RoleService extends BaseService {

  def countActiveRolesByCriteria(criteria: Option[RoleCriteria]): Future[ServiceResponse[Int]]

  def getActiveRolesByCriteria(
    criteria: Option[RoleCriteria],
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Role]]]

  def createActiveRole(dto: RoleToCreate, reactivateIfExisting: Boolean): Future[ServiceResponse[Role]]

  def updateRole(id: UUID, dto: RoleToUpdate): Future[ServiceResponse[Role]]

  def removeRole(id: UUID, updatedBy: String, updatedAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]
}
