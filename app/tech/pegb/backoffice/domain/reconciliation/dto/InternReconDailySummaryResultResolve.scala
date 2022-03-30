package tech.pegb.backoffice.domain.reconciliation.dto

import java.time.LocalDateTime

case class InternReconDailySummaryResultResolve(
    comments: String,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime])
