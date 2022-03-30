package tech.pegb.backoffice.api.transaction.dto

import com.fasterxml.jackson.annotation.JsonProperty

import java.time.ZonedDateTime
import java.util.UUID

//NOTE: since we separate actual api model and swagger model because of some jackson and swagger incompatibility
//we need to create a trait to define the fields that should be present in these two duplicate models

trait ManualTransactionToReadT {
  val id: UUID
  val manualTxnLines: Seq[ManualTransactionLinesToReadT]
  val status: String
  val transactionReason: String
  val createdBy: String
  val createdAt: ZonedDateTime
}

case class ManualTransactionToRead(
    @JsonProperty(required = true) id: UUID,
    @JsonProperty(required = true) manualTxnLines: Seq[ManualTransactionLinesToRead],
    @JsonProperty(required = true) status: String,
    @JsonProperty(required = true) transactionReason: String,
    @JsonProperty(required = true) createdBy: String,
    @JsonProperty(required = true) createdAt: ZonedDateTime) extends ManualTransactionToReadT

trait ManualTransactionLinesToReadT {
  val lineId: Int
  val manualTxnId: UUID
  val account: String
  val currency: Option[String]
  val direction: String
  val amount: BigDecimal
  val explanation: String
}

case class ManualTransactionLinesToRead(
    @JsonProperty(required = true) lineId: Int,
    @JsonProperty(required = true) manualTxnId: UUID,
    @JsonProperty(required = true) account: String,
    @JsonProperty(required = true) currency: Option[String],
    @JsonProperty(required = true) direction: String,
    @JsonProperty(required = true) amount: BigDecimal,
    @JsonProperty(required = true) explanation: String) extends ManualTransactionLinesToReadT

