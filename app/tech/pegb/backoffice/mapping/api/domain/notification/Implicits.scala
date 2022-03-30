package tech.pegb.backoffice.mapping.api.domain.notification

import java.time.ZonedDateTime
import java.util.UUID

import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.notification.dto.{NotificationTemplateToCreate, NotificationTemplateToUpdate}
import tech.pegb.backoffice.domain.notification.{dto ⇒ domain}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.UUIDLike

object Implicits {

  implicit class NotificationTemplateToCreateDomainAdapter(val arg: NotificationTemplateToCreate) extends AnyVal {
    def asDomain(doneAt: ZonedDateTime, doneBy: String) = domain.NotificationTemplateToCreate(
      createdAt = doneAt.toLocalDateTimeUTC,
      createdBy = doneBy.sanitize,
      name = arg.name.sanitize,
      titleResource = s"${arg.name}_title".sanitize,
      defaultTitle = arg.defaultTitle,
      contentResource = s"${arg.name}_content".sanitize,
      defaultContent = arg.defaultContent,
      channels = arg.channels.map(_.sanitize),
      description = arg.description.map(_.sanitize))
  }

  //id:Option[UUIDLike], name:Option[String], channel:Option[String], createdAtFrom:Option[LocalDateTimeFrom], createdAtTo:Option[LocalDateTimeTo], isActive:Option[Boolean], partialMatchFields:Option[String]
  implicit class NotificationTemplateCriteriaDomainAdapter(val arg: (Option[UUIDLike], Option[String], Option[String], Option[LocalDateTimeFrom], Option[LocalDateTimeTo], Option[Boolean], Option[String])) extends AnyVal {
    def asDomain = domain.NotificationTemplateCriteria(
      id = arg._1, name = arg._2.map(_.sanitize), channel = arg._3.map(_.sanitize),
      createdAtFrom = arg._4.map(_.localDateTime), createdAtTo = arg._5.map(_.localDateTime),
      isActive = arg._6, partialMatchFields = arg._7.toSeqByComma.toSet)
  }

  implicit class FindByIdCriteriaDomainAdapter(val arg: UUID) extends AnyVal {
    def asNotificationTemplateCriteria = domain.NotificationTemplateCriteria(id = UUIDLike(arg.toString).toOption)

    def asNotificationCriteria = domain.NotificationCriteria(id = UUIDLike(arg.toString).toOption)
  }

  //(id:Option[UUIDLike], templateId:Option[UUIDLike], userId:Option[UUIDLike], operationId:Option[String], channel:Option[String], title:Option[String], content:Option[String], address:Option[String], status:Option[String], createdAtFrom:Option[LocalDateTimeFrom], createdAtTo:Option[LocalDateTimeTo], partialMatchFields:Option[String])
  implicit class NotificationCriteriaDomainAdapter(val arg: (Option[UUIDLike], Option[UUIDLike], Option[UUIDLike], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[LocalDateTimeFrom], Option[LocalDateTimeTo], Option[String])) extends AnyVal {

    def asDomain = domain.NotificationCriteria(
      id = arg._1, templateId = arg._2, userId = arg._3, operationId = arg._4.map(_.sanitize), channel = arg._5.map(_.sanitize),
      title = arg._6.map(_.sanitize), content = arg._7.map(_.sanitize),
      address = arg._8.map(_.sanitize), status = arg._9.map(_.sanitize),
      createdAtFrom = arg._10.map(_.localDateTime), createdAtTo = arg._11.map(_.localDateTime),
      partialMatchFields = arg._12.toSeqByComma.toSet)
  }

  implicit class NotificationTemplateToUpdateDomainAdapter(val arg: NotificationTemplateToUpdate) extends AnyVal {
    def asDomain(doneAt: ZonedDateTime, doneBy: String) = domain.NotificationTemplateToUpdate(
      updatedAt = doneAt.toLocalDateTimeUTC,
      updatedBy = doneBy,
      channels = arg.channels,
      isActive = arg.isActive,
      name = arg.name,
      titleResource = arg.titleResource,
      defaultTitle = arg.defaultTitle,
      contentResource = arg.contentResource,
      defaultContent = arg.defaultContent,
      description = arg.description,
      lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
  }

}
