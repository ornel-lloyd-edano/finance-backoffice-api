package tech.pegb.backoffice.api.swagger.model

import java.util.UUID

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "DocumentToCreate")
case class DocumentToCreate(
    @ApiModelProperty(name = "customer_id", example = "732e7dfb-f024-4f8a-a21b-3ff6ac1107a8", required = true) customerId: UUID,
    @ApiModelProperty(name = "application_id", example = "c8ad880d-8b4a-4219-be48-2bd5ebc1d245", required = true) applicationId: UUID,
    @ApiModelProperty(name = "document_type", example = "national_id", required = true) documentType: String,
    @ApiModelProperty(name = "document_identifier", example = "748-1985-2592054-9", required = false) documentIdentifier: Option[String],
    @ApiModelProperty(name = "purpose", example = "wallet application requirement", required = true) purpose: String)
