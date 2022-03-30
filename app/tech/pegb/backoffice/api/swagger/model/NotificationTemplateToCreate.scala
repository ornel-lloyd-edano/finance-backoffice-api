package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.notification.dto.NotificationTemplateToCreateI

case class NotificationTemplateToCreate(
    @ApiModelProperty(name = "name", required = true) name: String,
    @ApiModelProperty(name = "default_title", required = true) defaultTitle: String,
    @ApiModelProperty(name = "default_content", required = true) defaultContent: String,
    @ApiModelProperty(name = "channels", required = true) channels: Seq[String],
    @ApiModelProperty(name = "description", required = false) description: Option[String]) extends NotificationTemplateToCreateI {

}
