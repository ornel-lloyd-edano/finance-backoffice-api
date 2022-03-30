package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime
import java.util.UUID

import com.fasterxml.jackson.databind.JsonNode
import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.parameter.dto.ParameterToReadI

case class ParameterToRead(
    @ApiModelProperty(name = "id", example = "82e58b1f-c22b-4cec-9188-adca2ad27574", required = true) id: UUID,
    @ApiModelProperty(name = "key", example = "saving_goal_reasons", required = true) key: String,
    @ApiModelProperty(name = "value", example = """["vacation", "new_car"]""", required = true) value: JsonNode,
    @ApiModelProperty(name = "metadata_id", example = "system_settings", required = true) metadataId: String,
    @ApiModelProperty(name = "key", example = "[android, iOS]", required = true) platforms: Seq[String],
    @ApiModelProperty(name = "explanation", example = "number of retries allowed for sending one time password", required = false) explanation: Option[String],
    @ApiModelProperty(name = "created_at", example = "2019-01-01T00:00:00Z", required = true) createdAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "created_by", example = "Backoffice User", required = true) createdBy: Option[String],
    @ApiModelProperty(name = "updated_at", example = "Backoffice User", required = false) updatedAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "updated_at", example = "2019-01-02T00:00:00Z", required = false) updatedBy: Option[String]) extends ParameterToReadI
