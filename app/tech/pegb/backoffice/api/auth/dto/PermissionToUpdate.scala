package tech.pegb.backoffice.api.auth.dto

import java.time.ZonedDateTime
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

case class PermissionToUpdate(
    permissionKey: Option[PermissionKey],
    scopeId: Option[UUID],
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
