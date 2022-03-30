package tech.pegb.backoffice.api.businessuserapplication.dto

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class BusinessUserApplicationToRead(
    @ApiModelProperty(name = "id", required = true, example = "da2a3868-4bbe-4bdc-adde-8e8e61bef2df") id: UUID,
    @ApiModelProperty(name = "business_name", required = true, example = "Universal Catering Co") businessName: String,
    @ApiModelProperty(name = "brand_name", required = true, example = "Costa Coffee DSO") brandName: String,
    @ApiModelProperty(name = "business_category", required = true, example = "Restaurants - 5812") businessCategory: String,
    @ApiModelProperty(name = "stage", required = true, example = "identity_info") stage: String,
    @ApiModelProperty(name = "status", required = true, example = "ongoing") status: String,
    @ApiModelProperty(name = "user_tier", required = true, example = "Basic") userTier: String,
    @ApiModelProperty(name = "business_type", required = true, example = "Merchant") businessType: String,
    @ApiModelProperty(name = "registration_number", required = true, example = "213/564654EE") registrationNumber: String,
    @ApiModelProperty(name = "tax_number", required = true, example = "A213546468977M") taxNumber: Option[String],
    @ApiModelProperty(name = "registration_date", required = true, example = "1996-01-20") registrationDate: Option[LocalDate],
    @ApiModelProperty(name = "explanation", required = true, example = "some notes on business user application creation") explanation: Option[String],
    @ApiModelProperty(name = "submitted_by", required = true, example = "alice") submittedBy: Option[String],
    @ApiModelProperty(name = "submitted_at", required = true, example = "2020-01-20") submittedAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "checked_by", required = true, example = "pegbuser") checkedBy: Option[String],
    @ApiModelProperty(name = "checked_at", required = true, example = "2020-01-21") checkedAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "valid_transaction_config", required = true) validTransactionConfig: Seq[TransactionConfigToRead],
    @ApiModelProperty(name = "created_by", required = true, example = "alice") createdBy: String,
    @ApiModelProperty(name = "created_at", required = true, example = "2020-01-20") createdAt: ZonedDateTime,
    @ApiModelProperty(name = "updated_by", required = true, example = "pegbuser") updatedBy: Option[String],
    @ApiModelProperty(name = "updated_at", required = true, example = "2020-01-21") updatedAt: Option[ZonedDateTime])
