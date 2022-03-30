package tech.pegb.backoffice.domain.parameter.dto

import java.time.LocalDateTime

import play.api.libs.json.JsValue
import tech.pegb.backoffice.domain.parameter.model.Platform

case class ParameterToCreate(
    key: String,
    value: JsValue,
    explanation: Option[String],
    metadataId: String,
    platforms: Seq[Platform],
    createdAt: LocalDateTime,
    createdBy: String)

