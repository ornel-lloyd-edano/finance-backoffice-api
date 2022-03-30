package tech.pegb.backoffice.api.i18n.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class I18nStringToUpdate(
    key: Option[String] = None,
    text: Option[String] = None,
    locale: Option[String] = None,
    platform: Option[String] = None,
    `type`: Option[String] = None,
    explanation: Option[String] = None,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
