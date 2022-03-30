package tech.pegb.backoffice.domain.document.dto

import java.time.LocalDateTime
import java.util.UUID

case class DocumentToReject(
    id: UUID,
    rejectedBy: String,
    rejectedAt: LocalDateTime,
    reason: String,
    lastUpdatedAt: Option[LocalDateTime])
