package tech.pegb.backoffice.domain.notification.model

import java.time.LocalDateTime
import java.util.UUID

case class NotificationTemplate(
    id: UUID,
    name: String,
    titleResource: String,
    defaultTitle: String,
    contentResource: String,
    defaultContent: String,
    description: Option[String],
    channels: Seq[String],
    createdAt: LocalDateTime,
    updatedAt: Option[LocalDateTime],
    isActive: Boolean) {

}
