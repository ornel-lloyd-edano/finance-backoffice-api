package tech.pegb.backoffice.api.swagger.model

import java.util.UUID

import io.swagger.annotations.{ApiModel, ApiModelProperty}
import tech.pegb.backoffice.api.customer.dto.{CustomerExternalAccountToCreateT, ExternalAccountToCreateT}

@ApiModel("CustomerExternalAccountToCreate")
case class CustomerExternalAccountToCreate(
    @ApiModelProperty(name = "provider", required = true, example = "Kenya Central Bank") provider: String,
    @ApiModelProperty(name = "account_number", required = true, example = "955100") accountNumber: String,
    @ApiModelProperty(name = "account_holder", required = true, example = "George Ogalo") accountHolder: String,
    @ApiModelProperty(name = "currency", required = true, example = "KES") currency: String) extends CustomerExternalAccountToCreateT {

}

@ApiModel("ExternalAccountToCreate")
case class ExternalAccountToCreate(
    @ApiModelProperty(name = "customer_id", required = true, example = "94ca5847-fa95-447b-9a74-4c04cb248755") customerId: UUID,
    @ApiModelProperty(name = "provider", required = true, example = "Kenya Central Bank") provider: String,
    @ApiModelProperty(name = "account_number", required = true, example = "955100") accountNumber: String,
    @ApiModelProperty(name = "account_holder", required = true, example = "George Ogalo") accountHolder: String,
    @ApiModelProperty(name = "currency", required = true, example = "KES") currency: String) extends ExternalAccountToCreateT {

}
