package tech.pegb.backoffice.api.fee.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class FeeProfileToRead(
    @ApiModelProperty(name = "id", example = "e37015f3-b9eb-43a8-8b1a-4f20f00ecd88 ", required = true) id: UUID,
    @ApiModelProperty(name = "fee_type", example = "transaction_based", required = true) feeType: String,
    @ApiModelProperty(name = "user_type", example = "business", required = true) userType: String,
    @ApiModelProperty(name = "tier", example = "small", required = true) tier: String,
    @ApiModelProperty(name = "subscription_type", example = "platinum", required = true) subscriptionType: String,
    @ApiModelProperty(name = "transaction_type", example = "international_remittance", required = true) transactionType: String,
    @ApiModelProperty(name = "channel", example = "eft", required = true) channel: Option[String],
    @ApiModelProperty(name = "other_party", example = "Pambazuka", required = true) otherParty: Option[String],
    @ApiModelProperty(name = "instrument", example = "credit_card", required = true) instrument: Option[String],
    @ApiModelProperty(name = "calculation_method", example = "staircase_flat_percentage", required = true) calculationMethod: String,
    @ApiModelProperty(name = "fee_method", example = "add", required = true) feeMethod: String,
    @ApiModelProperty(name = "tax_included", example = "true", required = true) taxIncluded: Option[Boolean],
    @ApiModelProperty(name = "currency_code", example = "AED", required = true) currencyCode: String,
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime]) {

}
