package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.transaction.dto.{ManualTransactionFxDetailsT, ManualTransactionLinesToCreateT, ManualTransactionToCreateT}

case class ManualTransactionToCreate(
    @ApiModelProperty(name = "transaction_reason", example = "account top up by request of finance department", required = true) transactionReason: String,
    @ApiModelProperty(name = "fx_details", required = true) fxDetails: Option[ManualTransactionFxDetails],
    @ApiModelProperty(name = "manual_txn_lines", example = "", required = true) manualTxnLines: Seq[ManualTransactionLinesToCreate]) extends ManualTransactionToCreateT

case class ManualTransactionLinesToCreate(
    @ApiModelProperty(name = "primary_account_number", example = "1250.01", required = true) primaryAccountNumber: String,
    @ApiModelProperty(name = "primary_currency", example = "KES", required = true) primaryCurrency: String,
    @ApiModelProperty(name = "secondary_account_number", example = "3210.01", required = true) secondaryAccountNumber: String,
    @ApiModelProperty(name = "secondary_currency", example = "USD", required = true) secondaryCurrency: String,
    @ApiModelProperty(name = "primary_direction", example = "debit", required = true) primaryDirection: String,
    @ApiModelProperty(name = "amount", example = "500.00", required = true) amount: BigDecimal,
    @ApiModelProperty(name = "primary_explanation", example = "500.00KES from 1250.01 distribution account", required = true) primaryExplanation: String,
    @ApiModelProperty(name = "secondary_explanation", example = "500.00KES to 3210.01 collection account", required = true) secondaryExplanation: String,
    @ApiModelProperty(name = "secondary_amount", example = "500.00", required = true) secondaryAmount: Option[BigDecimal]) extends ManualTransactionLinesToCreateT

case class ManualTransactionFxDetails(
    @ApiModelProperty(name = "fx_provider", example = "Central Bank of Kenya: CBK", required = true) fxProvider: String,
    @ApiModelProperty(name = "from_currency", example = "KES", required = true) fromCurrency: String,
    @ApiModelProperty(name = "to_currency", example = "USD", required = true) toCurrency: String,
    @ApiModelProperty(name = "fx_rate", example = "0.00983", required = true) fxRate: BigDecimal) extends ManualTransactionFxDetailsT
