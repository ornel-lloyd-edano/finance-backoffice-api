package tech.pegb.backoffice.api.customer.dto

package CustomerAttributes {

  import java.time.ZonedDateTime

  import com.fasterxml.jackson.annotation.JsonProperty
  import io.swagger.annotations.{ApiModel, ApiModelProperty}

  @ApiModel(value = "Address")
  case class Address(
      @JsonProperty(required = true)@ApiModelProperty(name = "underlying", required = true) underlying: String)

  @ApiModel(value = "CustomerStatus")
  case class CustomerStatus(status: String)

  @ApiModel(value = "DeactivatePayload")
  case class DeactivatePayload(
      @JsonProperty(required = true)@ApiModelProperty(name = "reason", required = true) reason: String)

  @ApiModel(value = "ActivationDocumentType")
  case class ActivationDocumentType(
      @JsonProperty(required = true)@ApiModelProperty(name = "type", required = true) `type`: String)

  @ApiModel(value = "ActivationRequirement")
  case class ActivationRequirement(
      @JsonProperty(required = true)@ApiModelProperty(name = "identifier", required = true) identifier: String,
      @JsonProperty(required = true)@ApiModelProperty(name = "document_type", required = true) documentType: ActivationDocumentType,
      @JsonProperty(required = true)@ApiModelProperty(name = "verified_by", required = true) verifiedBy: String,
      @JsonProperty(required = true)@ApiModelProperty(name = "verified_at", required = true) verifiedAt: ZonedDateTime)

}

