package tech.pegb.backoffice.api.aggregations.dto

import io.swagger.annotations.ApiModelProperty

case class UserBalancePercentageToUpdate(
    @ApiModelProperty(name = "user_balance") userBalance: BigDecimal,
    percentage: BigDecimal)
