package tech.pegb.backoffice.dao.reconciliation.model

import java.time.{LocalDate, LocalDateTime}

case class InternReconDailySummaryResult(
    id: String,
    reconDate: LocalDate,
    accountId: String,
    accountNumber: String,
    accountType: String,
    accountMainType: String,
    userId: Int,
    userUuid: String,
    userFullName: Option[String] = None,
    anyCustomerName: Option[String] = None,
    currency: String,
    endOfDayBalance: BigDecimal,
    valueChange: BigDecimal,
    transactionTotalAmount: BigDecimal,
    transactionTotalCount: Int,
    problematicTxnCount: Int,
    status: String,
    comments: Option[String] = None,
    updatedAt: Option[LocalDateTime] = None,
    updatedBy: Option[String] = None)
