package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.businessuserapplication.dto.{AccountConfigT, BusinessUserApplicationConfigToCreateT, ExternalAccountT, TransactionConfigT}

case class BusinessUserApplicationConfigToCreateSwagger(
    @ApiModelProperty(name = "transaction_config", required = true) transactionConfig: Seq[TransactionConfig],
    @ApiModelProperty(name = "account_config", required = true) accountConfig: Seq[AccountConfig],
    @ApiModelProperty(name = "external_accounts", required = true) externalAccounts: Seq[ExternalAccount]) extends BusinessUserApplicationConfigToCreateT

case class TransactionConfig(
    @ApiModelProperty(name = "transaction_type", required = true, example = "merchant_payment") transactionType: String,
    @ApiModelProperty(name = "currency_code", required = true, example = "KES") currencyCode: String) extends TransactionConfigT

case class AccountConfig(
    @ApiModelProperty(name = "account_type", required = true, example = "collection") accountType: String,
    @ApiModelProperty(name = "account_name", required = true, example = "Default Collection") accountName: String,
    @ApiModelProperty(name = "currency_code", required = true, example = "KES") currencyCode: String,
    @ApiModelProperty(name = "is_default", required = true, example = "true") isDefault: Boolean) extends AccountConfigT

case class ExternalAccount(
    @ApiModelProperty(name = "provider", required = true, example = "mPesa") provider: String,
    @ApiModelProperty(name = "account_number", required = true, example = "955100") accountNumber: String,
    @ApiModelProperty(name = "account_holder", required = true, example = "Costa Coffee FZE") accountHolder: String,
    @ApiModelProperty(name = "currency_code", required = true, example = "KES") currencyCode: String) extends ExternalAccountT
