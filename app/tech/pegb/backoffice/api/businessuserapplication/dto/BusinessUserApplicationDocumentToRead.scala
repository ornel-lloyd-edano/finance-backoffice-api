package tech.pegb.backoffice.api.businessuserapplication.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class BusinessUserApplicationDocumentToRead(
    @ApiModelProperty(name = "id", required = true) id: UUID,
    @ApiModelProperty(name = "status", required = true) status: String,
    @ApiModelProperty(name = "documents", required = true) documents: Seq[SimpleDocumentToRead],
    @ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "created_by", required = true) createdBy: String,
    @ApiModelProperty(name = "updated_at", required = false) updatedAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "updated_by", required = false) updatedBy: Option[String],
    @ApiModelProperty(name = "submitted_by", required = false) submittedBy: Option[String]) {

}

case class SimpleDocumentToRead(
    @ApiModelProperty(name = "id", required = true) id: UUID,
    @ApiModelProperty(name = "application_id", required = false) applicationId: Option[UUID],
    @ApiModelProperty(name = "filename", required = true) filename: String,
    @ApiModelProperty(name = "document_type", required = true) documentType: String)
