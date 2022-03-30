package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty

case class FeeProfileRangeToUpdate(
    @ApiModelProperty(required = true, example = "100.00", value = "minimum is 0 and decimal places not greater than 2") max: BigDecimal,
    @ApiModelProperty(required = true, example = "1.00", value = "minimum is 0 and decimal places not greater than 2") min: BigDecimal,
    @ApiModelProperty(name = "fee_amount", required = true, value = "minimum is 0 and decimal places not greater than 2") feeAmount: Option[BigDecimal],
    @ApiModelProperty(name = "fee_ratio", required = true, example = "0.2500", value = "minimum is 0 and decimal places not greater than 4") feeRatio: Option[BigDecimal])
