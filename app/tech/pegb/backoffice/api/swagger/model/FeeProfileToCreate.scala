package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty

case class FeeProfileToCreate(
    @ApiModelProperty(name = "fee_type", required = true, example = "transaction_based") feeType: String,
    @ApiModelProperty(name = "user_type", required = true, example = "business") userType: String,
    @ApiModelProperty(name = "tier", required = true, example = "small") tier: String,
    @ApiModelProperty(name = "subscription_type", required = true, example = "platinum") subscriptionType: String,
    @ApiModelProperty(name = "transaction_type", required = true, example = "international_remittance") transactionType: String,
    @ApiModelProperty(name = "channel", required = true, example = "eft") channel: String,
    @ApiModelProperty(name = "other_party", required = false, example = "Pambazuka") otherParty: Option[String],
    @ApiModelProperty(name = "instrument", required = false) instrument: Option[String],
    @ApiModelProperty(name = "calculation_method", required = true, example = "staircase_flat_percentage") calculationMethod: String,
    @ApiModelProperty(name = "currency_code", required = true, example = "AED") currencyCode: String,
    @ApiModelProperty(name = "fee_method", required = true, example = "add") feeMethod: String,
    @ApiModelProperty(name = "tax_included", required = false, example = "true", value = "null means tax not applicable") taxIncluded: Option[Boolean],
    @ApiModelProperty(name = "max_fee", required = false, value = "required if calculation method is flat percentage") maxFee: Option[BigDecimal],
    @ApiModelProperty(name = "min_fee", required = false, value = "required if calculation method is flat percentage") minFee: Option[BigDecimal],
    @ApiModelProperty(name = "fee_amount", required = false, value = "required if calculation method is flat amount") feeAmount: Option[BigDecimal],
    @ApiModelProperty(name = "fee_ratio", required = false, value = "required if calculation method is flat percentage") feeRatio: Option[BigDecimal],
    @ApiModelProperty(name = "ranges", required = false, value = "required if calculation method is flat/percentage staircase") ranges: Option[Array[FeeProfileRangeToCreate]])
