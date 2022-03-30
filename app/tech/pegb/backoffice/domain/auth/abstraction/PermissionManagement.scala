package tech.pegb.backoffice.domain.auth.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.auth.dto.{PermissionCriteria, PermissionToCreate, PermissionToUpdate}
import tech.pegb.backoffice.domain.auth.implementation.PermissionMgmtService
import tech.pegb.backoffice.domain.auth.model.Permission
import tech.pegb.backoffice.domain.model

import scala.concurrent.Future

@ImplementedBy(classOf[PermissionMgmtService])
trait PermissionManagement {

  def createPermission(createDto: PermissionToCreate, reactivate: Boolean): Future[ServiceResponse[Permission]]

  def getPermissionById(id: UUID): Future[ServiceResponse[Permission]]

  def getPermissionByCriteria(criteriaDto: PermissionCriteria, ordering: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Permission]]]

  def countByCriteria(criteriaDto: PermissionCriteria): Future[ServiceResponse[Int]]

  def updatePermissionById(id: UUID, updateDto: PermissionToUpdate): Future[ServiceResponse[Permission]]

  def deletePermissionById(id: UUID, updatedAt: LocalDateTime, updatedBy: String, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

}
