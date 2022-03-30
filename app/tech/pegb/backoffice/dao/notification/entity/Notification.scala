package tech.pegb.backoffice.dao.notification.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class Notification(
    id: Int,
    uuid: String,
    userId: Option[Int],
    userUuid: Option[String],
    templateId: Int,
    templateUuid: String,
    channel: String,
    title: String,
    content: String,
    address: String,
    status: String,
    errorMsg: Option[String],
    retries: Option[Int],
    operationId: String,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime],
    sentAt: Option[LocalDateTime])

object Notification {

  implicit val format = Json.format[Notification]
}
