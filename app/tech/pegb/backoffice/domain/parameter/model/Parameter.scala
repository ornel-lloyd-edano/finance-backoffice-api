package tech.pegb.backoffice.domain.parameter.model

import java.time.LocalDateTime
import java.util.UUID

import play.api.libs.json.JsValue

case class Parameter(
    id: UUID,
    key: String,
    value: JsValue,
    explanation: Option[String],
    metadataId: String,
    platforms: Seq[Platform],
    createdAt: Option[LocalDateTime],
    createdBy: Option[String],
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])
