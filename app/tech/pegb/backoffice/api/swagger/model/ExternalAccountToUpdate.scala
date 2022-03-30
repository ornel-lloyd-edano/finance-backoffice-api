package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.{ApiModel, ApiModelProperty}
import tech.pegb.backoffice.api.customer.dto.ExternalAccountToUpdateT

@ApiModel("ExternalAccountToUpdate")
case class ExternalAccountToUpdate(
    @ApiModelProperty(name = "provider", required = false, example = "Kenya Central Bank") provider: Option[String],
    @ApiModelProperty(name = "account_number", required = false, example = "955100") accountNumber: Option[String],
    @ApiModelProperty(name = "account_holder", required = false, example = "George Ogalo") accountHolder: Option[String],
    @ApiModelProperty(name = "currency", required = false, example = "KES") currency: Option[String],
    @ApiModelProperty(name = "updated_at", required = false, example = "2019-01-01T00:00:00Z") lastUpdatedAt: Option[ZonedDateTime]) extends ExternalAccountToUpdateT {

}
