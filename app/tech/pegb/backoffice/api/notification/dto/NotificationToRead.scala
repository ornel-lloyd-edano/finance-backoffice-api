package tech.pegb.backoffice.api.notification.dto

import java.time.ZonedDateTime
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

case class NotificationToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "template_id", required = true) templateId: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "operation_id", required = true) operationId: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "channel", required = true) channel: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "title", required = true) title: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "content", required = true) content: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "address", required = true) address: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "user_id", required = false) userId: Option[UUID],
    @JsonProperty(required = true)@ApiModelProperty(name = "status", required = true) status: String,

    @JsonProperty(required = true)@ApiModelProperty(name = "sent_at", required = false) sentAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "error_msg", required = false) errorMsg: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "retries", required = false) retries: Option[Int],
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = false) updatedAt: Option[ZonedDateTime]) {

}
