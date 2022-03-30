package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty

case class I18nStringToCreate(
    @ApiModelProperty(name = "key", required = true, example = "hello") key: String,
    @ApiModelProperty(name = "text", required = true, example = "hello") text: String,
    @ApiModelProperty(name = "locale", required = true, example = "en-US") locale: String,
    @ApiModelProperty(name = "platform", required = true, example = "web") platform: String,
    @ApiModelProperty(name = "type", required = true, example = "chat_message") `type`: Option[String],
    @ApiModelProperty(name = "explanation", required = true, example = "text for hello") explanation: Option[String])
