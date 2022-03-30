package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.transaction.dto.{ManualTransactionLinesToReadT, ManualTransactionToReadT}

case class ManualTransactionToRead(
    @ApiModelProperty(name = "id", example = "a37c9454-8067-4dc1-9319-3034ddc9919c", required = true) id: UUID,
    @ApiModelProperty(name = "manual_txn_lines", example = "", required = true) manualTxnLines: Seq[ManualTransactionLinesToRead],
    @ApiModelProperty(name = "status", example = "pending", required = true) status: String,
    @ApiModelProperty(name = "transaction_reason", example = "account top up by request of finance department", required = true) transactionReason: String,
    @ApiModelProperty(name = "created_by", example = "Pegb User", required = true) createdBy: String,
    @ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime) extends ManualTransactionToReadT

case class ManualTransactionLinesToRead(
    @ApiModelProperty(name = "line_id", example = "1", required = true) lineId: Int,
    @ApiModelProperty(name = "manual_txn_id", example = "a37c9454-8067-4dc1-9319-3034ddc9919c", required = true) manualTxnId: UUID,
    @ApiModelProperty(name = "account", example = "1250.01", required = true) account: String,
    @ApiModelProperty(name = "currency", example = "KES", required = true) currency: Option[String],
    @ApiModelProperty(name = "direction", example = "debit", required = true) direction: String,
    @ApiModelProperty(name = "amount", example = "500.00", required = true) amount: BigDecimal,
    @ApiModelProperty(name = "explanation", example = "500.00KES from 1250.01 distribution account", required = true) explanation: String,

    //fields below are redundant fields of ManualTransactionToRead
    //this was done just in case front end wants to read manual txn api in denormalized flat structure
    @ApiModelProperty(name = "status", example = "pending", required = true) status: Option[String],
    @ApiModelProperty(name = "transaction_reason", example = "account top up by request of finance department", required = true) transactionReason: Option[String],
    @ApiModelProperty(name = "created_by", example = "Pegb User", required = true) createdBy: Option[String],
    @ApiModelProperty(name = "created_at", required = true) createdAt: Option[ZonedDateTime]) extends ManualTransactionLinesToReadT
