package tech.pegb.backoffice.api.i18n.dto

import com.fasterxml.jackson.annotation.JsonProperty

case class I18nStringBulkCreate(
    @JsonProperty(required = true) locale: String,
    @JsonProperty(required = true) strings: Seq[I18nStringToCreate])
