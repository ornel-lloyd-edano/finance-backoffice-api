package tech.pegb.backoffice.domain.document.dto

import java.time.LocalDateTime
import java.util.UUID

case class DocumentToApprove(
    id: UUID,
    approvedBy: String,
    approvedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime])
