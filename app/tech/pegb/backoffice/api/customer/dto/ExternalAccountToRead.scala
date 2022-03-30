package tech.pegb.backoffice.api.customer.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "ExternalAccount")
case class ExternalAccountToRead(
    @ApiModelProperty(name = "id", required = true, example = "da2a3868-4bbe-4bdc-adde-8e8e61bef2df") id: UUID,
    @ApiModelProperty(name = "customer_id", required = true, example = "da2a3868-4bbe-4bdc-adde-8e8e61bef2df") customerId: UUID,
    @ApiModelProperty(name = "provider", required = true, example = "Kenya Central Bank") provider: String,
    @ApiModelProperty(name = "account_number", required = true, example = "955100") accountNumber: String,
    @ApiModelProperty(name = "account_holder", required = true, example = "George Ogalo") accountHolder: String,
    @ApiModelProperty(name = "currency", required = true, example = "KES") currency: String,
    @ApiModelProperty(name = "updated_at", required = false, example = "2019-01-01T00:00:00Z") updatedAt: Option[ZonedDateTime]) {

}
