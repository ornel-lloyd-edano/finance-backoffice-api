package tech.pegb.backoffice.api.swagger.model

import java.util.UUID

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.businessuserapplication.dto.DocumentMetadataToCreateT

case class DocumentMetadataToCreate(
    @ApiModelProperty(name = "customer_id", required = false) customerId: Option[UUID],
    @ApiModelProperty(name = "application_id", required = false) applicationId: Option[UUID],
    @ApiModelProperty(name = "filename", required = true) filename: String,
    @ApiModelProperty(name = "document_type", required = true) documentType: String) extends DocumentMetadataToCreateT {

}
