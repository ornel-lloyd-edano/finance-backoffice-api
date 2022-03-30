package tech.pegb.backoffice.api.swagger.model

import tech.pegb.backoffice.api.notification.dto.NotificationTemplateToRead

case class NotificationTemplatePaginatedResults(
    total: Int,
    results: Array[NotificationTemplateToRead],
    limit: Option[Int],
    offset: Option[Int]) {

}
