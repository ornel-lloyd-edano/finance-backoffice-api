package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime

case class BackOfficeUserToRemove(removedBy: String, removedAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]) {

}
