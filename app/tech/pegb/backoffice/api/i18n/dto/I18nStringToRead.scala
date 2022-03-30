package tech.pegb.backoffice.api.i18n.dto

import java.time.{ZonedDateTime}

import io.swagger.annotations.ApiModelProperty

case class I18nStringToRead(
    @ApiModelProperty(name = "id", example = "1", required = true) id: Int,
    @ApiModelProperty(name = "key", example = "welcome", required = true) key: String,
    @ApiModelProperty(name = "text", example = "welcome", required = true) text: String,
    @ApiModelProperty(name = "locale", example = "en-US", required = true) locale: String,
    @ApiModelProperty(name = "platform", example = "web", required = true) platform: String,
    @ApiModelProperty(name = "type", example = "chat_message", required = true) `type`: Option[String],
    @ApiModelProperty(name = "explanation", example = "resource for welcome", required = true) explanation: Option[String],
    @ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime])
