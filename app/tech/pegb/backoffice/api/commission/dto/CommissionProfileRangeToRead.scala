package tech.pegb.backoffice.api.commission.dto

import io.swagger.annotations.ApiModelProperty

case class CommissionProfileRangeToRead(
    @ApiModelProperty(name = "min", required = true, example = "1") min: BigDecimal,
    @ApiModelProperty(name = "max", required = true, example = "100") max: Option[BigDecimal],
    @ApiModelProperty(name = "commission_amount", required = true, example = "50") commissionAmount: Option[BigDecimal],
    @ApiModelProperty(name = "commission_ratio", required = true, example = "0.05") commissionRatio: Option[BigDecimal])
