package tech.pegb.backoffice.api.document.dto

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

case class DocumentToCreate(
    @JsonProperty(required = true) customerId: UUID,
    @JsonProperty(required = true) applicationId: UUID,
    @JsonProperty(required = false) fileName: Option[String],
    @JsonProperty(required = true) documentType: String,
    @JsonProperty(required = true) documentIdentifier: Option[String],
    @JsonProperty(required = true) purpose: String)
