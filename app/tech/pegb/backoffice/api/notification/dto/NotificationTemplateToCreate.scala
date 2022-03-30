package tech.pegb.backoffice.api.notification.dto

import com.fasterxml.jackson.annotation.JsonProperty

trait NotificationTemplateToCreateI {
  val name: String
  val defaultTitle: String
  val defaultContent: String
  val channels: Seq[String]
  val description: Option[String]
}

case class NotificationTemplateToCreate(
    @JsonProperty(required = true) name: String,
    @JsonProperty(required = true) defaultTitle: String,
    @JsonProperty(required = true) defaultContent: String,
    @JsonProperty(required = true) channels: Seq[String],
    @JsonProperty(required = true) description: Option[String]) extends NotificationTemplateToCreateI {

}

