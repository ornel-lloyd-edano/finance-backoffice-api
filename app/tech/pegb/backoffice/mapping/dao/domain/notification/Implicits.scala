package tech.pegb.backoffice.mapping.dao.domain.notification

import java.util.UUID

import tech.pegb.backoffice.dao.notification.entity.{Notification, NotificationTemplate}
import tech.pegb.backoffice.domain.notification.model
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class NotificationTemplateEntityToDomainAdapter(val arg: NotificationTemplate) extends AnyVal {
    def asDomain = model.NotificationTemplate(
      id = UUID.fromString(arg.uuid),
      name = arg.name,
      titleResource = arg.titleResource,
      defaultTitle = arg.defaultTitle,
      contentResource = arg.contentResource,
      defaultContent = arg.defaultContent,
      description = arg.description,
      channels = if (!arg.channels.hasSomething || arg.channels.trim.matches("\\[[ ]*\\]"))
        Seq.empty[String] else arg.channels.trim.stripEnclosingQuotes.replaceAll("[\\[\\]]", "").split(",").map(_.trim),
      createdAt = arg.createdAt,
      updatedAt = arg.updatedAt,
      isActive = arg.isActive)
  }

  implicit class NotificationEntityToDomainAdapter(val arg: Notification) extends AnyVal {
    def asDomain = model.Notification(
      id = UUID.fromString(arg.uuid),
      templateId = UUID.fromString(arg.templateUuid),
      operationId = arg.operationId,
      channel = arg.channel,
      title = arg.title,
      content = arg.content,
      address = arg.address,
      userId = arg.userUuid.map(uuid ⇒ UUID.fromString(uuid)),
      status = arg.status,
      createdAt = arg.createdAt,
      updatedAt = arg.updatedAt,
      sentAt = arg.sentAt,
      errorMsg = arg.errorMsg,
      retries = arg.retries)
  }

}
