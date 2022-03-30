package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty

case class InternReconDailySummaryResultResolve(
    @ApiModelProperty(name = "comments", required = true, example = "Manual transaction created. resolving...") comments: String,
    @ApiModelProperty(name = "updated_at", required = true) lastUpdatedAt: Option[ZonedDateTime])

