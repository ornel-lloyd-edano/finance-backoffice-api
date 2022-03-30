package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.notification.dto.NotificationTemplateToUpdateI

case class NotificationTemplateToUpdate(
    @ApiModelProperty(name = "channels", required = false) channels: Option[Seq[String]] = None,
    @ApiModelProperty(name = "is_active", required = false) isActive: Option[Boolean] = None,
    @ApiModelProperty(name = "name", required = false) name: Option[String] = None,
    @ApiModelProperty(name = "title_resource", required = false) titleResource: Option[String] = None,
    @ApiModelProperty(name = "default_title", required = false) defaultTitle: Option[String] = None,
    @ApiModelProperty(name = "content_resource", required = false) contentResource: Option[String] = None,
    @ApiModelProperty(name = "default_content", required = false) defaultContent: Option[String] = None,
    @ApiModelProperty(name = "description", required = false) description: Option[String] = None,
    @ApiModelProperty(name = "updated_at", required = false) lastUpdatedAt: Option[ZonedDateTime] = None) extends NotificationTemplateToUpdateI
