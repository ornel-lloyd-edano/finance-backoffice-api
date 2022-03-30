package tech.pegb.backoffice.api.recon.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class InternReconDailySummaryResultResolve(
    comments: String,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
