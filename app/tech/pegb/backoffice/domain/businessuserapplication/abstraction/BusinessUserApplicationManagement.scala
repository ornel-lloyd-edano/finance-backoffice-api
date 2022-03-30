package tech.pegb.backoffice.domain.businessuserapplication.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.businessuserapplication.dto._
import tech.pegb.backoffice.domain.businessuserapplication.implementation.BusinessUserApplicationMgmtService
import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessUserApplication, ContactAddress, ContactPerson}
import tech.pegb.backoffice.domain.model

import scala.concurrent.Future

@ImplementedBy(classOf[BusinessUserApplicationMgmtService])
trait BusinessUserApplicationManagement {

  def createBusinessUserApplication(dto: BusinessUserApplicationToCreate): Future[ServiceResponse[BusinessUserApplication]]

  def createBusinessUserApplicationConfig(dto: BusinessUserApplicationConfigToCreate): Future[ServiceResponse[BusinessUserApplication]]

  def createBusinessUserContactInfo(
    applicationId: UUID,
    contacts: Seq[ContactPerson],
    addresses: Seq[ContactAddress],
    createdBy: String,
    createdAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[BusinessUserApplication]]

  def getBusinessUserApplicationById(uuid: UUID, stageDataIncluded: Seq[String]): Future[ServiceResponse[BusinessUserApplication]]

  def getBusinessUserApplicationByCriteria(
    criteria: BusinessUserApplicationCriteria,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[BusinessUserApplication]]]

  def countBusinessUserApplicationByCriteria(criteria: BusinessUserApplicationCriteria): Future[ServiceResponse[Int]]

  def submitBusinessUserApplication(
    applicationId: UUID,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

  def approveBusinessUserApplication(
    applicationId: UUID,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

  def cancelBusinessUserApplication(
    applicationId: UUID,
    explanation: String,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

  def rejectBusinessUserApplication(
    applicationId: UUID,
    explanation: String,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

  def sendForCorrectionBusinessUserApplication(
    applicationId: UUID,
    explanation: String,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

}
