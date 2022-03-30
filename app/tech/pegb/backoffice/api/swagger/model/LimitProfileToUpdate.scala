package tech.pegb.backoffice.api.swagger.model

import java.time.LocalDateTime

import io.swagger.annotations.ApiModelProperty

case class LimitProfileToUpdate(
    @ApiModelProperty(name = "max_amount_per_interval", required = true) maxAmountPerInterval: Option[BigDecimal],
    @ApiModelProperty(name = "min_amount_per_txn", required = true) minAmountPerTxn: Option[BigDecimal],
    @ApiModelProperty(name = "max_amount_per_txn", required = true) maxAmountPerTxn: Option[BigDecimal],
    @ApiModelProperty(name = "max_count_per_interval", required = true) maxCountPerInterval: Option[Int],
    @ApiModelProperty(name = "max_balance_amount", required = true) maxBalanceAmount: Option[BigDecimal],
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[LocalDateTime])

