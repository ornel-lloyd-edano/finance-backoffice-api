package tech.pegb.backoffice.domain.auth.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.auth._
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.auth.dto._
import tech.pegb.backoffice.domain.auth.model.BackOfficeUser
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[implementation.BackOfficeUserService])
trait BackOfficeUserService extends BaseService {
  def countActiveBackOfficeUsersByCriteria(criteria: Option[BackOfficeUserCriteria]): Future[ServiceResponse[Int]]

  def getActiveBackOfficeUsersByCriteria(
    criteria: Option[BackOfficeUserCriteria],
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[BackOfficeUser]]]

  def getBackOfficeUserByUsername(username: String): Future[ServiceResponse[BackOfficeUser]]

  def createBackOfficeUser(dto: BackOfficeUserToCreate, reactivateIfExisting: Boolean): Future[ServiceResponse[BackOfficeUser]]

  def updateBackOfficeUser(id: UUID, dto: BackOfficeUserToUpdate): Future[ServiceResponse[BackOfficeUser]]

  def removeBackOfficeUser(id: UUID, dto: BackOfficeUserToRemove): Future[ServiceResponse[Unit]]
}
