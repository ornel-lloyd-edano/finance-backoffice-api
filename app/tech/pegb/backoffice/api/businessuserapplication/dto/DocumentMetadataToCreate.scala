package tech.pegb.backoffice.api.businessuserapplication.dto

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

trait DocumentMetadataToCreateT {
  val customerId: Option[UUID]
  val applicationId: Option[UUID]
  val filename: String
  val documentType: String
}

case class DocumentMetadataToCreate(
    @JsonProperty(value = "customer_id", required = false) customerId: Option[UUID],
    @JsonProperty(value = "application_id", required = false) applicationId: Option[UUID],
    @JsonProperty(value = "filename", required = true) filename: String,
    @JsonProperty(value = "document_type", required = true) documentType: String) extends DocumentMetadataToCreateT {

}
