package tech.pegb.backoffice.api.businessuserapplication.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class BusinessUserApplicationConfigToRead(
    @ApiModelProperty(name = "id", required = true, example = "da2a3868-4bbe-4bdc-adde-8e8e61bef2df") id: UUID,
    @ApiModelProperty(name = "status", required = true, example = "ongoing") status: String,
    @ApiModelProperty(name = "transaction_config", required = true) transactionConfig: Seq[TransactionConfigToRead],
    @ApiModelProperty(name = "account_config", required = true) accountConfig: Seq[AccountConfigToRead],
    @ApiModelProperty(name = "external_accounts", required = true) externalAccounts: Seq[ExternalAccountToRead],
    @ApiModelProperty(name = "created_by", required = true, example = "alice") createdBy: String,
    @ApiModelProperty(name = "created_at", required = true, example = "2020-01-20") createdAt: ZonedDateTime,
    @ApiModelProperty(name = "updated_by", required = false, example = "pegbuser") updatedBy: Option[String],
    @ApiModelProperty(name = "updated_at", required = false, example = "2020-01-21") updatedAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "submitted_by", required = false) submittedBy: Option[String])

case class TransactionConfigToRead(
    @ApiModelProperty(name = "transaction_type", required = true, example = "merchant_payment") transactionType: String,
    @ApiModelProperty(name = "currency_code", required = true, example = "KES") currencyCode: String)

case class AccountConfigToRead(
    @ApiModelProperty(name = "account_type", required = true, example = "collection") accountType: String,
    @ApiModelProperty(name = "account_name", required = true, example = "Default Collection") accountName: String,
    @ApiModelProperty(name = "currency_code", required = true, example = "KES") currencyCode: String,
    @ApiModelProperty(name = "is_default", required = true, example = "true") isDefault: Boolean)

case class ExternalAccountToRead(
    @ApiModelProperty(name = "provider", required = true, example = "mPesa") provider: String,
    @ApiModelProperty(name = "account_number", required = true, example = "955100") accountNumber: String,
    @ApiModelProperty(name = "account_holder", required = true, example = "Costa Coffee FZE") accountHolder: String,
    @ApiModelProperty(name = "currency_code", required = true, example = "KES") currencyCode: String)
