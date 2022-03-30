package tech.pegb.backoffice.api.makerchecker.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.databind.JsonNode

case class TaskDetailToRead(
    id: String,
    module: String,
    action: String,
    status: String,
    reason: Option[String],
    createdAt: ZonedDateTime,
    createdBy: String,
    checkedAt: Option[ZonedDateTime],
    checkedBy: Option[String],
    updatedAt: Option[ZonedDateTime],
    change: Option[JsonNode],
    originalValue: Option[JsonNode],
    isReadOnly: Boolean,
    stale: Option[Boolean])
