package tech.pegb.backoffice.api.swagger.model

import tech.pegb.backoffice.api.notification.dto.{NotificationToRead}

case class NotificationPaginatedResults(
    total: Int,
    results: Array[NotificationToRead],
    limit: Option[Int],
    offset: Option[Int]) {

}
