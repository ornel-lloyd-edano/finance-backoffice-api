package tech.pegb.backoffice.domain.reconciliation.dto

import java.time.{LocalDate, LocalDateTime}

case class InternReconSummaryDto(
    id: String,
    accountId: String,
    accountNumber: String,
    accountType: String,
    accountMainType: String,
    currency: String,

    userUuid: String,
    userFullName: Option[String],
    anyCustomerName: Option[String],
    reconDate: LocalDate,

    endOfDayBalance: BigDecimal,
    valueChange: BigDecimal, //refers to value change from last reconciliation
    difference: BigDecimal, //refers to EOD-TTA
    transactionTotalAmount: BigDecimal,
    transactionTotalCount: Int,

    problematicInternReconResultsCount: Int,

    reconStatus: String,
    comments: Option[String],
    updatedAt: Option[LocalDateTime])
