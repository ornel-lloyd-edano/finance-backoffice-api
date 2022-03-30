package tech.pegb.backoffice.mapping.domain.api.notification

import tech.pegb.backoffice.api.notification.{dto ⇒ api}
import tech.pegb.backoffice.domain.notification.model.{Notification, NotificationTemplate}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class NotificationTemplateApiAdapter(val arg: NotificationTemplate) extends AnyVal {
    def asApi = api.NotificationTemplateToRead(
      id = arg.id,
      name = arg.name,
      titleResource = arg.titleResource,
      defaultTitle = arg.defaultTitle,
      contentResource = arg.contentResource,
      defaultContent = arg.defaultContent,
      description = arg.description,
      channels = arg.channels,
      createdAt = arg.createdAt.toZonedDateTimeUTC,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
      isActive = arg.isActive)
  }

  implicit class NotificationApiAdapter(val arg: Notification) extends AnyVal {
    def asApi = api.NotificationToRead(
      id = arg.id, templateId = arg.templateId, userId = arg.userId,
      operationId = arg.operationId, channel = arg.channel, title = arg.title, content = arg.content,
      address = arg.address, status = arg.status, errorMsg = arg.errorMsg, retries = arg.retries,
      sentAt = arg.sentAt.map(_.toZonedDateTimeUTC), createdAt = arg.createdAt.toZonedDateTimeUTC,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
  }

}
