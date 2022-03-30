package tech.pegb.backoffice.domain.notification.dto

import java.time.LocalDateTime

case class NotificationTemplateToCreate(
    createdAt: LocalDateTime,
    createdBy: String,
    name: String,
    titleResource: String,
    defaultTitle: String,
    contentResource: String,
    defaultContent: String,
    channels: Seq[String],
    description: Option[String]) {

}
