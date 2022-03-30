package tech.pegb.backoffice.domain.notification.dto

import java.time.LocalDateTime

case class NotificationTemplateToUpdate(
    updatedAt: LocalDateTime,
    updatedBy: String,
    channels: Option[Seq[String]] = None,
    isActive: Option[Boolean] = None,

    name: Option[String] = None,
    titleResource: Option[String] = None,
    defaultTitle: Option[String] = None,
    contentResource: Option[String] = None,
    defaultContent: Option[String] = None,
    description: Option[String] = None,
    lastUpdatedAt: Option[LocalDateTime] = None) {

}
