package tech.pegb.backoffice.domain.auth.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.auth.dto.{ScopeCriteria, ScopeToCreate, ScopeToUpdate}
import tech.pegb.backoffice.domain.auth.implementation.ScopeMgmtService
import tech.pegb.backoffice.domain.auth.model.Scope
import tech.pegb.backoffice.domain.model

import scala.concurrent.Future

@ImplementedBy(classOf[ScopeMgmtService])
trait ScopeManagement {

  def createScope(createDto: ScopeToCreate, reactivate: Boolean): Future[ServiceResponse[Scope]]

  def getScopeById(id: UUID): Future[ServiceResponse[Scope]]

  def getScopeByCriteria(criteriaDto: ScopeCriteria, ordering: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Scope]]]

  def countByCriteria(criteriaDto: ScopeCriteria): Future[ServiceResponse[Int]]

  def updateScopeById(id: UUID, updateDto: ScopeToUpdate): Future[ServiceResponse[Scope]]

  def deleteScopeById(id: UUID, updatedAt: LocalDateTime, updatedBy: String, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

}
