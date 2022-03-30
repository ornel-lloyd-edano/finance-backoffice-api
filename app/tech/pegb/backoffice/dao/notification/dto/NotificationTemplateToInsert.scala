package tech.pegb.backoffice.dao.notification.dto

import java.time.LocalDateTime

case class NotificationTemplateToInsert(
    createdAt: LocalDateTime,
    createdBy: String,
    name: String,
    titleResource: String,
    defaultTitle: String,
    contentResource: String,
    defaultContent: String,
    channels: String,
    description: Option[String],
    isActive: Boolean) {

}
