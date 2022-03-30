package tech.pegb.backoffice.domain.document.dto

import java.time.LocalDateTime
import java.util.UUID

case class DocumentToPersist(
    documentId: UUID,
    persistedBy: String,
    persistedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) {

}
