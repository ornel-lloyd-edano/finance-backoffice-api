package tech.pegb.backoffice.api.swagger.model

import java.time.LocalDateTime

import io.swagger.annotations.ApiModelProperty

case class SpreadToUpdate(
    @ApiModelProperty(name = "spread", required = true) spread: BigDecimal,
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[LocalDateTime])
