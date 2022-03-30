package tech.pegb.backoffice.api.notification.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

trait NotificationTemplateToUpdateI {
  val channels: Option[Seq[String]]
  val isActive: Option[Boolean]
  val name: Option[String]
  val titleResource: Option[String]
  val defaultTitle: Option[String]
  val contentResource: Option[String]
  val defaultContent: Option[String]
  val description: Option[String]
  val lastUpdatedAt: Option[ZonedDateTime]
}

case class NotificationTemplateToUpdate(
    channels: Option[Seq[String]] = None,
    isActive: Option[Boolean] = None,
    name: Option[String] = None,
    titleResource: Option[String] = None,
    defaultTitle: Option[String] = None,
    contentResource: Option[String] = None,
    defaultContent: Option[String] = None,
    description: Option[String] = None,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime] = None) extends NotificationTemplateToUpdateI {

}
