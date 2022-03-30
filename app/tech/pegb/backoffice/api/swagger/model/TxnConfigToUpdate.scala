package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.{ApiModel, ApiModelProperty}
import tech.pegb.backoffice.api.customer.dto.TxnConfigToUpdateT

@ApiModel("TxnConfigToUpdate")
case class TxnConfigToUpdate(
    @ApiModelProperty(name = "transaction_type", required = false, example = "cashout") transactionType: Option[String],
    @ApiModelProperty(name = "currency", required = false, example = "KES") currency: Option[String],
    @ApiModelProperty(name = "updated_at", required = false, example = "2019-01-01T00:00:00Z") lastUpdatedAt: Option[ZonedDateTime]) extends TxnConfigToUpdateT {

}
