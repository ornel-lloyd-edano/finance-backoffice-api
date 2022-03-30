package tech.pegb.backoffice.domain.customer.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.customer.implementation.BusinessUserMgmtService
import tech.pegb.backoffice.domain.customer.model.VelocityPortalUser
import tech.pegb.backoffice.domain.model

import scala.concurrent.Future

@ImplementedBy(classOf[BusinessUserMgmtService])
trait BusinessUserManagement {

  //TODO: Update DTOs and models when DB design is finished
  def getTxnConfig(userId: UUID): Future[ServiceResponse[_]]

  def countTxnConfig(userId: UUID): Future[ServiceResponse[Int]]

  //VP users
  def getVelocityUsers(
    userId: UUID,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[VelocityPortalUser]]]

  def countVelocityUsers(userId: UUID): Future[ServiceResponse[Int]]

  def getVelocityUsersById(
    userId: UUID,
    vpUserId: UUID): Future[ServiceResponse[VelocityPortalUser]]

  def resetVelocityUserPin(
    userId: UUID,
    vpUserId: UUID,
    reason: String,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

  //Documents
  def getDocuments(userId: UUID): Future[ServiceResponse[_]]

  def countDocuments(userId: UUID): Future[ServiceResponse[Int]]

}
