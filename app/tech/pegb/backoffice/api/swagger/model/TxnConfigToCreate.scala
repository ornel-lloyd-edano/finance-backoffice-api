package tech.pegb.backoffice.api.swagger.model

import java.util.UUID

import io.swagger.annotations.{ApiModel, ApiModelProperty}
import tech.pegb.backoffice.api.customer.dto.{CustomerTxnConfigToCreateT, TxnConfigToCreateT}

@ApiModel("TxnConfigToCreate")
case class TxnConfigToCreate(
    @ApiModelProperty(name = "customer_id", required = true, example = "da2a3868-4bbe-4bdc-adde-8e8e61bef2df") customerId: UUID,
    @ApiModelProperty(name = "transaction_type", required = true, example = "cashout") transactionType: String,
    @ApiModelProperty(name = "currency", required = true, example = "KES") currency: String) extends TxnConfigToCreateT {

}

@ApiModel("CustomerTxnConfigToCreate")
case class CustomerTxnConfigToCreate(
    @ApiModelProperty(name = "transaction_type", required = true, example = "cashout") transactionType: String,
    @ApiModelProperty(name = "currency", required = true, example = "KES") currency: String) extends CustomerTxnConfigToCreateT {

}
