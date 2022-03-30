package tech.pegb.backoffice.api.document.dto
import java.time.ZonedDateTime
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "DocumentToRead")
case class DocumentToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "customer_id", required = false) customerId: Option[UUID],
    @JsonProperty(required = true)@ApiModelProperty(name = "application_id", required = true) applicationId: Option[UUID],
    @JsonProperty(required = true)@ApiModelProperty(name = "document_type", required = true) documentType: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "document_identifier", required = false) documentIdentifier: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "purpose", required = true) purpose: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "created_by", required = true) createdBy: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "status", required = true) status: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "reason_if_rejected", required = false) rejectionReason: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "checked_at", required = false) checkedAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "checked_by", required = false) checkedBy: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "uploaded_at", required = false) uploadedAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "uploaded_by", required = false) uploadedBy: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = false) updatedAt: Option[ZonedDateTime])

object DocumentToRead {
  val empty = new DocumentToRead(id = UUID.randomUUID(), customerId = Some(UUID.randomUUID()), applicationId = Some(UUID.randomUUID()),
    documentType = "", documentIdentifier = None, purpose = "", createdAt = ZonedDateTime.now(), createdBy = "",
    status = "", rejectionReason = None, checkedAt = None, checkedBy = None,
    uploadedAt = None, uploadedBy = None, updatedAt = None)
}
