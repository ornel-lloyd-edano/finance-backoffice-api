package tech.pegb.backoffice.api.swagger.model

import java.util.UUID

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.auth.dto.ScopeToCreateT

case class ScopeToCreate(
    @ApiModelProperty(name = "name", required = true, example = "dashboards_create") name: String,
    @ApiModelProperty(name = "parent_id", required = true, example = "abc015f3-b9eb-43a8-8b1a-4f20f00ecd88") parentId: Option[UUID],
    @ApiModelProperty(name = "description", required = true, example = "Access to dashboard creation") description: Option[String]) extends ScopeToCreateT
