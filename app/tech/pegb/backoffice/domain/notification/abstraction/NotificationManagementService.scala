package tech.pegb.backoffice.domain.notification.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.notification.dto.{NotificationCriteria, NotificationTemplateCriteria, NotificationTemplateToCreate, NotificationTemplateToUpdate, NotificationToCreate}
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.notification.implementation.NotificationMgmtServiceImpl
import tech.pegb.backoffice.domain.notification.model.{Notification, NotificationTemplate}

import scala.concurrent.Future

@ImplementedBy(classOf[NotificationMgmtServiceImpl])
trait NotificationManagementService {

  def createNotificationTemplate(dto: NotificationTemplateToCreate): Future[ServiceResponse[NotificationTemplate]]

  def getNotificationTemplatesByCriteria(
    criteria: Option[NotificationTemplateCriteria],
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[NotificationTemplate]]]

  def countNotificationTemplatesByCriteria(criteria: Option[NotificationTemplateCriteria]): Future[ServiceResponse[Int]]

  def activateNotificationTemplate(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

  def deactivateNotificationTemplate(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

  def updateNotificationTemplate(id: UUID, dto: NotificationTemplateToUpdate): Future[ServiceResponse[Unit]]

  def createNotification(dto: NotificationToCreate): Future[ServiceResponse[Notification]]

  def getNotificationsByCriteria(
    criteria: Option[NotificationCriteria],
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Notification]]]

  def countNotificationsByCriteria(criteria: Option[NotificationCriteria]): Future[ServiceResponse[Int]]
}
