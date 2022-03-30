package tech.pegb.backoffice.domain.notification.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.dao.notification.abstraction.{NotificationDao, NotificationTemplateDao}
import tech.pegb.backoffice.dao.notification.dto.{NotificationTemplateToUpdate ⇒ DaoNotificationTemplateToUpdate}
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.notification.abstraction.NotificationManagementService
import tech.pegb.backoffice.domain.notification.dto._
import tech.pegb.backoffice.domain.notification.model.{Notification, NotificationTemplate}
import tech.pegb.backoffice.domain.{BaseService, FieldValueValidation, ServiceError, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.notification.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.notification.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts

import scala.concurrent.Future

class NotificationMgmtServiceImpl @Inject() (
    executionContexts: WithExecutionContexts,
    notifTemplateDao: NotificationTemplateDao,
    notifDao: NotificationDao,
    val typesDao: TypesDao) extends NotificationManagementService with BaseService with FieldValueValidation {
  implicit val ec = executionContexts.blockingIoOperations

  def createNotificationTemplate(dto: NotificationTemplateToCreate): Future[ServiceResponse[NotificationTemplate]] = {
    (for {
      _ ← EitherT.fromEither[Future]({
        dto.channels.map(validateFieldsFromKnownTypes(_, "communication_channels"))
          .toList.sequence[ServiceResponse, String]
      })
      notificationTemplate ← EitherT.fromEither[Future](notifTemplateDao.insertNotificationTemplate(dto.asDao).asServiceResponse)
    } yield {
      notificationTemplate.asDomain
    }).value
  }

  def getNotificationTemplatesByCriteria(
    criteria: Option[NotificationTemplateCriteria],
    orderBy: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[NotificationTemplate]]] = Future {
    notifTemplateDao.getNotificationTemplateByCriteria(
      criteria.map(_.asDao),
      orderBy.asDao,
      limit,
      offset).map(_.map(_.asDomain)).asServiceResponse
  }

  private def updateNotificationTemplateStatus(id: UUID, doneBy: String, doneAt: LocalDateTime, isActive: Boolean,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    (for {
      _ ← EitherT.fromEither[Future](notifTemplateDao.updateNotificationTemplate(
        uuid = id.toString,
        dto = DaoNotificationTemplateToUpdate(
          updatedAt = doneAt,
          updatedBy = doneBy,
          isActive = isActive.some,
          lastUpdatedAt = lastUpdatedAt)).asServiceResponse)
    } yield {
      ()
    }).value
  }

  def activateNotificationTemplate(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    updateNotificationTemplateStatus(id, doneBy, doneAt, isActive = true, lastUpdatedAt: Option[LocalDateTime])
  }

  def deactivateNotificationTemplate(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    updateNotificationTemplateStatus(id, doneBy, doneAt, isActive = false, lastUpdatedAt: Option[LocalDateTime])
  }

  def createNotification(dto: NotificationToCreate): Future[ServiceResponse[Notification]] = {
    /*(for {
      notificationTemplateGetResult ← EitherT.fromEither[Future](notifTemplateDao.getNotificationTemplateByCriteria(
        DaoNotificationTemplateCriteria(uuid = CriteriaField("uuid", dto.templateId.toString).some).some,
        none, none, none).asServiceResponse)
      notificationTemplate ← EitherT.fromOption[Future](notificationTemplateGetResult.headOption, notFoundEntityError(s"no notification template found for id ${dto.templateId}"))
      _ ← EitherT.fromEither[Future](validateFieldsFromKnownTypes(dto.channel, "communication_channels"))
      //notification ← EitherT.fromEither[Future](notifDao.insertNotification(dto.)
    } yield {
      ???
    }).value*/
    //TODO: not needed for now per Salih
    ???
  }

  def getNotificationsByCriteria(
    criteria: Option[NotificationCriteria],
    orderBy: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Notification]]] = Future {
    notifDao.getNotificationByCriteria(
      criteria.map(_.asDao),
      orderBy.asDao,
      limit,
      offset).map(_.map(_.asDomain)).asServiceResponse
  }

  def countNotificationTemplatesByCriteria(criteria: Option[NotificationTemplateCriteria]): Future[ServiceResponse[Int]] = Future {
    notifTemplateDao.getCountByCriteria(criteria.map(_.asDao)).asServiceResponse
  }

  def countNotificationsByCriteria(criteria: Option[NotificationCriteria]): Future[ServiceResponse[Int]] = Future {
    notifDao.getCountByCriteria(criteria.map(_.asDao())).asServiceResponse
  }

  def validateFieldsFromKnownTypes(fieldvalue: String, fieldName: String): ServiceResponse[String] = {
    val result = fieldName match {
      case "communication_channels" ⇒ typesDao.getCommunicationChannels.map(_.find(_._2 == fieldvalue).map(_._2))
      case _ ⇒ Right(Option.empty[String])
    }

    result.asServiceResponse
      .flatMap(_.toRight(ServiceError.validationError(s"provided element `$fieldvalue` is invalid for '$fieldName'")))

  }

  def updateNotificationTemplate(id: UUID, dto: NotificationTemplateToUpdate) = Future {
    notifTemplateDao.updateNotificationTemplate(id.toString, dto.asDao).map(_ ⇒ ()).asServiceResponse
  }
}
