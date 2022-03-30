package tech.pegb.backoffice.domain.notification.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class NotificationCriteria(
    id: Option[UUIDLike] = None,
    templateId: Option[UUIDLike] = None,
    channel: Option[String] = None,
    title: Option[String] = None,
    content: Option[String] = None,
    address: Option[String] = None,
    userId: Option[UUIDLike] = None,
    status: Option[String] = None,
    operationId: Option[String] = None,
    createdAtFrom: Option[LocalDateTime] = None,
    createdAtTo: Option[LocalDateTime] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch {

}
