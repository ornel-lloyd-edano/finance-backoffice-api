package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime

case class BusinessUnitToRemove(removedBy: String, removedAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]) {

}
