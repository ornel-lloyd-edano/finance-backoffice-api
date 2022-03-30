package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty

case class I18nStringToUpdate(
    @ApiModelProperty(name = "key", required = true, example = "hello") key: Option[String],
    @ApiModelProperty(name = "text", required = true, example = "hello") text: Option[String],
    @ApiModelProperty(name = "locale", required = true, example = "en-US") locale: Option[String],
    @ApiModelProperty(name = "platform", required = true, example = "web") platform: Option[String],
    @ApiModelProperty(name = "type", required = true, example = "chat_message") `type`: Option[String],
    @ApiModelProperty(name = "explanation", required = true, example = "text for hello") explanation: Option[String],
    @ApiModelProperty(name = "updated_at", required = false) lastUpdatedAt: Option[ZonedDateTime])
