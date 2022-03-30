package tech.pegb.backoffice.api.notification.dto

import java.time.ZonedDateTime
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

case class NotificationTemplateToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "name", required = true) name: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "title_resource", required = true) titleResource: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "default_title", required = true) defaultTitle: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "content_resource", required = true) contentResource: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "default_content", required = true) defaultContent: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "description", required = false) description: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "channels", required = true) channels: Seq[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = false) updatedAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "is_active", required = true) isActive: Boolean) {

}
