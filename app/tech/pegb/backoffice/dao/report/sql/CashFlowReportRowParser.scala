package tech.pegb.backoffice.dao.report.sql

import java.time.LocalDate

import anorm.Row
import tech.pegb.backoffice.dao.report.dto.{CashFlowReportRow, CashFlowTotals}

import scala.util.Try

trait CashFlowReportRowParser extends CashFlowReportMetadata {
  def parseCashFlowReportRow(row: Row): Try[CashFlowReportRow] = Try {
    CashFlowReportRow(
      date = row[LocalDate](this.cDate),
      provider = row[String](this.cProvider),
      accountNum = row[Option[String]](this.cAccNum).getOrElse("unknown account"),
      currency = row[String](this.cCurrency),
      openingBalance = row[Option[BigDecimal]](this.cOpeningBal).getOrElse(BigDecimal(0)),
      closingBalance = row[Option[BigDecimal]](this.cClosingBal).getOrElse(BigDecimal(0)),
      bankTransfer = row[Option[BigDecimal]](this.cBankTrans).getOrElse(BigDecimal(0)),
      cashins = row[Option[BigDecimal]](this.cCashins).getOrElse(BigDecimal(0)),
      cashouts = row[Option[BigDecimal]](this.cCashouts).getOrElse(BigDecimal(0)),
      transactions = row[Option[BigDecimal]](this.cOtherTxns).getOrElse(BigDecimal(0)))
  }

  def parseCashFlowTotalsRow(row: Row): Try[CashFlowTotals] = Try {
    CashFlowTotals(
      totalBankTransfers = row[Option[BigDecimal]](this.cBankTrans).getOrElse(BigDecimal(0)),
      totalCashins = row[Option[BigDecimal]](this.cCashins).getOrElse(BigDecimal(0)),
      totalCashouts = row[Option[BigDecimal]](this.cCashouts).getOrElse(BigDecimal(0)),
      totalTransactions = row[Option[BigDecimal]](this.cOtherTxns).getOrElse(BigDecimal(0)))
  }

}
