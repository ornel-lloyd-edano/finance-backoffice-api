package tech.pegb.backoffice.domain.document.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.domain.document.model.DocumentStatus

case class DocumentToUpload(
    fileUploadedBy: String,
    fileUploadedAt: LocalDateTime,
    status: Option[DocumentStatus],
    lastUpdatedAt: Option[LocalDateTime]) {

}
