package tech.pegb.backoffice.domain.notification.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class NotificationTemplateCriteria(
    id: Option[UUIDLike] = None,
    name: Option[String] = None,
    titleResource: Option[String] = None,
    contentResource: Option[String] = None,
    channel: Option[String] = None,
    createdAtFrom: Option[LocalDateTime] = None,
    createdAtTo: Option[LocalDateTime] = None,
    isActive: Option[Boolean] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch {

}
