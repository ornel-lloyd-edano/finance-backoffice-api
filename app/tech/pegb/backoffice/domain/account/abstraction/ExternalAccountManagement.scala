package tech.pegb.backoffice.domain.account.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.LatestVersionService
import tech.pegb.backoffice.domain.account.dto.{ExternalAccountCriteria, ExternalAccountToCreate, ExternalAccountToUpdate}
import tech.pegb.backoffice.domain.account.implementation.ExternalAccountMgmtService
import tech.pegb.backoffice.domain.account.model.ExternalAccount
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[ExternalAccountMgmtService])
trait ExternalAccountManagement extends LatestVersionService[ExternalAccountCriteria, ExternalAccount] {

  def createExternalAccount(dto: ExternalAccountToCreate): Future[ServiceResponse[ExternalAccount]]

  def getExternalAccountByCriteria(criteria: ExternalAccountCriteria, orderBy: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[ExternalAccount]]]

  def getLatestVersion(criteria: ExternalAccountCriteria): Future[ServiceResponse[Option[ExternalAccount]]]

  def count(criteria: ExternalAccountCriteria): Future[ServiceResponse[Int]]

  def updateExternalAccount(criteria: ExternalAccountCriteria, dto: ExternalAccountToUpdate): Future[ServiceResponse[ExternalAccount]]

  def deleteExternalAccount(criteria: ExternalAccountCriteria, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]
}
