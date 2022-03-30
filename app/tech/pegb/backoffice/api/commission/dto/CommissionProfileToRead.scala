package tech.pegb.backoffice.api.commission.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class CommissionProfileToRead(
    @ApiModelProperty(name = "id", example = "e37015f3-b9eb-43a8-8b1a-4f20f00ecd88", required = true) id: UUID,
    @ApiModelProperty(name = "business_type", required = true, example = "merchant") businessType: String,
    @ApiModelProperty(name = "tier", required = true, example = "small") tier: String,
    @ApiModelProperty(name = "subscription_type", required = true, example = "standard") subscriptionType: String,
    @ApiModelProperty(name = "transaction_type", required = true, example = "cashin") transactionType: String,
    @ApiModelProperty(name = "currency_code", required = true, example = "KES") currencyCode: String,
    @ApiModelProperty(name = "channel", required = true, example = "Android") channel: Option[String],
    @ApiModelProperty(name = "instrument", required = true, example = "debit_card") instrument: Option[String],
    @ApiModelProperty(name = "calculation_method", required = true, example = "flat_percentage") calculationMethod: String,
    @ApiModelProperty(name = "min_commission", required = true, example = "1") minCommission: Option[BigDecimal],
    @ApiModelProperty(name = "max_commission", required = true, example = "50") maxCommission: Option[BigDecimal],
    @ApiModelProperty(name = "commission_amount", required = true, example = "10") commissionAmount: Option[BigDecimal],
    @ApiModelProperty(name = "commission_ratio", required = true, example = "0.05") commissionRatio: Option[BigDecimal],
    @ApiModelProperty(name = "created_by", required = true, example = "pegbuser") createdBy: String,
    @ApiModelProperty(name = "updated_by", required = true, example = "pegbuser") updatedBy: String,
    @ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: ZonedDateTime)
