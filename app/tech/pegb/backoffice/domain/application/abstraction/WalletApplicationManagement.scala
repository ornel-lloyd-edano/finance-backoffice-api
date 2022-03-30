package tech.pegb.backoffice.domain.application.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.application.dto.WalletApplicationCriteria
import tech.pegb.backoffice.domain.application.implementation.WalletApplicationMgmtService
import tech.pegb.backoffice.domain.application.model.WalletApplication
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[WalletApplicationMgmtService])
trait WalletApplicationManagement {

  def getWalletApplicationById(id: UUID): Future[ServiceResponse[WalletApplication]]

  def getWalletApplicationByUserUuid(userUuid: UUID): Future[ServiceResponse[Set[WalletApplication]]]

  def getWalletApplicationsByCriteria(criteria: WalletApplicationCriteria, ordering: Seq[Ordering], limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[WalletApplication]]]

  def countWalletApplicationsByCriteria(criteria: WalletApplicationCriteria): Future[ServiceResponse[Int]]

  def approvePendingWalletApplication(id: UUID, approvedBy: String, approvedAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[WalletApplication]]

  def rejectPendingWalletApplication(id: UUID, rejectedBy: String, rejectedAt: LocalDateTime, reason: String, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[WalletApplication]]

  def persistApprovedFilesByInternalApplicationId(id: Int): Future[ServiceResponse[Seq[UUID]]]

}
