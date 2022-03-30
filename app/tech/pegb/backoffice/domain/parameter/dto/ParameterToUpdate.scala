package tech.pegb.backoffice.domain.parameter.dto

import java.time.LocalDateTime

import play.api.libs.json.JsValue
import tech.pegb.backoffice.domain.parameter.model.Platform

case class ParameterToUpdate(
    value: JsValue,
    explanation: Option[String] = None,
    platforms: Option[Seq[Platform]] = None,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime])

