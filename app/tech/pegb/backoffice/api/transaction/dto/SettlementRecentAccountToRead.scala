package tech.pegb.backoffice.api.transaction.dto

import io.swagger.annotations.ApiModelProperty

case class SettlementRecentAccountToRead(
    @ApiModelProperty(name = "id", example = "1", required = true) id: Int,
    @ApiModelProperty(name = "customer_name", example = "John Doe", required = true) customerName: String,
    @ApiModelProperty(name = "account_number", example = "1201.1", required = true) accountNumber: String,
    @ApiModelProperty(name = "balance", example = "150000", required = true) balance: BigDecimal,
    @ApiModelProperty(name = "currency", example = "KES", required = true) currency: String)
