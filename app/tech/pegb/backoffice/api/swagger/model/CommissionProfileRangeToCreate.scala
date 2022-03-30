package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.commission.dto.CommissionProfileRangeToCreateTrait

case class CommissionProfileRangeToCreate(
    @ApiModelProperty(name = "min", required = true, example = "1") min: BigDecimal,
    @ApiModelProperty(name = "max", required = true, example = "50") max: Option[BigDecimal],
    @ApiModelProperty(name = "commission_amount", required = true, example = "10") commissionAmount: Option[BigDecimal],
    @ApiModelProperty(name = "commission_ratio", required = true, example = "0.05") commissionRatio: Option[BigDecimal]) extends CommissionProfileRangeToCreateTrait
