package tech.pegb.backoffice.api.swagger.model

import java.time.LocalDateTime

import io.swagger.annotations.ApiModelProperty

case class FeeProfileToUpdate(
    @ApiModelProperty(name = "calculation_method", required = true, example = "staircase_flat_percentage") calculationMethod: String,
    @ApiModelProperty(name = "fee_method", required = true, example = "add") feeMethod: String,
    @ApiModelProperty(name = "tax_included", required = false, example = "true", value = "null means tax not applicable") taxIncluded: Option[Boolean] = None,
    @ApiModelProperty(name = "max_fee", required = false, value = "required if calculation method is flat percentage") maxFee: Option[BigDecimal],
    @ApiModelProperty(name = "min_fee", required = false, value = "required if calculation method is flat percentage") minFee: Option[BigDecimal],
    @ApiModelProperty(name = "fee_amount", required = false, value = "required if calculation method is flat amount") feeAmount: Option[BigDecimal],
    @ApiModelProperty(name = "fee_ratio", required = false, value = "required if calculation method is flat percentage") feeRatio: Option[BigDecimal],
    @ApiModelProperty(name = "ranges", required = false, value = "required if calculation method is flat/percentage staircase") ranges: Option[Array[FeeProfileRangeToCreate]],
    @ApiModelProperty(name = "updated_at", required = false) updatedAt: Option[LocalDateTime])
