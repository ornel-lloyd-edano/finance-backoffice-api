package tech.pegb.backoffice.domain.customer.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.application.abstraction.WalletApplicationManagement
import tech.pegb.backoffice.domain.application.model.WalletApplication
import tech.pegb.backoffice.domain.customer.implementation.CustomerWalletApplicationService

import scala.concurrent.Future

@ImplementedBy(classOf[CustomerWalletApplicationService])
trait CustomerWalletApplication extends BaseService {

  val walletApplicationManagement: WalletApplicationManagement

  def getWalletApplicationsByUserId(userUUID: UUID): Future[ServiceResponse[Set[WalletApplication]]]

  def getWalletApplicationByApplicationIdAndUserId(userUUID: UUID, applicationUUID: UUID): Future[ServiceResponse[WalletApplication]]

  def approveWalletApplicationByUserId(userUUID: UUID, applicationUUID: UUID, approvedBy: String, approvedAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[WalletApplication]]

  def rejectWalletApplicationByUserId(userUUID: UUID, applicationUUID: UUID, rejectedBy: String, rejectedAt: LocalDateTime, reason: String, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[WalletApplication]]
}
