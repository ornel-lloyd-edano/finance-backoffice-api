package tech.pegb.backoffice.api.transaction.dto

import com.fasterxml.jackson.annotation.JsonProperty

trait ManualTransactionToCreateT {
  val transactionReason: String
  val fxDetails: Option[ManualTransactionFxDetailsT]
  val manualTxnLines: Seq[ManualTransactionLinesToCreateT]
}

trait ManualTransactionFxDetailsT {
  val fxProvider: String
  val fromCurrency: String
  val toCurrency: String
  val fxRate: BigDecimal
}

trait ManualTransactionLinesToCreateT {
  val primaryAccountNumber: String
  val primaryCurrency: String
  val secondaryAccountNumber: String
  val secondaryCurrency: String
  val primaryDirection: String
  val amount: BigDecimal
  val primaryExplanation: String
  val secondaryExplanation: String
  val secondaryAmount: Option[BigDecimal]
}

case class ManualTxnToCreate(
    @JsonProperty(required = true) transactionReason: String,
    @JsonProperty(required = true) fxDetails: Option[ManualTransactionFxDetails],
    @JsonProperty(required = true) manualTxnLines: Seq[ManualTxnLinesToCreate]) extends ManualTransactionToCreateT

case class ManualTxnLinesToCreate(
    @JsonProperty(required = true) primaryAccountNumber: String,
    @JsonProperty(required = true) primaryCurrency: String,
    @JsonProperty(required = true) secondaryAccountNumber: String,
    @JsonProperty(required = true) secondaryCurrency: String,
    @JsonProperty(required = true) primaryDirection: String,
    @JsonProperty(required = true) amount: BigDecimal,
    @JsonProperty(required = true) primaryExplanation: String,
    @JsonProperty(required = true) secondaryExplanation: String,
    @JsonProperty(required = true) secondaryAmount: Option[BigDecimal]) extends ManualTransactionLinesToCreateT

case class ManualTransactionFxDetails(
    @JsonProperty(required = true) fxProvider: String,
    @JsonProperty(required = true) fromCurrency: String,
    @JsonProperty(required = true) toCurrency: String,
    @JsonProperty(required = true) fxRate: BigDecimal) extends ManualTransactionFxDetailsT
