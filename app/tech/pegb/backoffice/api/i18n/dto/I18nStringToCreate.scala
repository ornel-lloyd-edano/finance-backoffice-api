package tech.pegb.backoffice.api.i18n.dto

import com.fasterxml.jackson.annotation.JsonProperty

case class I18nStringToCreate(
    @JsonProperty(required = true) key: String,
    @JsonProperty(required = true) text: String,
    @JsonProperty(required = true) locale: String,
    @JsonProperty(required = true) platform: String,
    @JsonProperty(required = false) `type`: Option[String],
    @JsonProperty(required = false) explanation: Option[String])
