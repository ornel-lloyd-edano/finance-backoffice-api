package tech.pegb.backoffice.dao.notification.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class NotificationTemplate(
    id: Int,
    uuid: String,
    name: String,
    titleResource: String,
    defaultTitle: String,
    contentResource: String,
    defaultContent: String,
    description: Option[String],
    channels: String,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],
    isActive: Boolean)

object NotificationTemplate {

  implicit val format = Json.format[NotificationTemplate]

}
