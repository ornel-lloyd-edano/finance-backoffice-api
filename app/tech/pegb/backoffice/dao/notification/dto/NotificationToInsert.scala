package tech.pegb.backoffice.dao.notification.dto

import java.time.LocalDateTime

case class NotificationToInsert(
    templateId: Int,
    channel: String,
    title: String,
    content: String,
    address: String,
    userId: Option[Int],
    status: String,
    operationId: String,
    sentAt: Option[LocalDateTime] = None,
    errorMsg: Option[String] = None,
    retries: Option[Int] = None,
    createdBy: String,
    createdAt: LocalDateTime) {

}
