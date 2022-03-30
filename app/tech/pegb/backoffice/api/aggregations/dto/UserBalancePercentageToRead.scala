package tech.pegb.backoffice.api.aggregations.dto

import io.swagger.annotations.ApiModelProperty

case class UserBalancePercentageToRead(
    institution: String,
    @ApiModelProperty(name = "user_balance") userBalance: BigDecimal,
    percentage: BigDecimal)
