package tech.pegb.backoffice.domain.notification.dto

import java.time.LocalDateTime
import java.util.UUID

case class NotificationToCreate(
    createdBy: String,
    createdAt: LocalDateTime,
    templateId: UUID,
    channel: String,
    title: String,
    content: String,
    address: String,
    userId: Option[UUID],
    operationId: String) {

}
