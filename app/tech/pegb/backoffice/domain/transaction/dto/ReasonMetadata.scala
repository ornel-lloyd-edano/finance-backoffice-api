package tech.pegb.backoffice.domain.transaction.dto

import java.time.LocalDateTime

case class ReasonMetadata(reason: String, createdAt: LocalDateTime, createdBy: String) {

}
