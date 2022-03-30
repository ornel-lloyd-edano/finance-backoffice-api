package tech.pegb.backoffice.domain.notification.model

import java.time.LocalDateTime
import java.util.UUID

case class Notification(
    id: UUID,
    templateId: UUID,
    operationId: String,
    channel: String,
    title: String,
    content: String,
    address: String,
    userId: Option[UUID],
    status: String,

    sentAt: Option[LocalDateTime],
    errorMsg: Option[String],
    retries: Option[Int],
    createdAt: LocalDateTime,
    updatedAt: Option[LocalDateTime]) {

}
