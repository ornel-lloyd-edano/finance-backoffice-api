package tech.pegb.backoffice.domain.report.dto

import java.time.LocalDate

import tech.pegb.backoffice.util.time.{DateRangeValidatable}

case class CashFlowReportCriteria(
    startDate: Option[LocalDate] = None,
    endDate: Option[LocalDate] = None,
    onlyForTheseCurrencies: Seq[String] = Nil,
    onlyForTheseProviders: Seq[String] = Nil,
    userType: Option[String] = None,
    txnOtherParty: Option[String] = None,
    notThisPrimaryAccNumber: Option[String] = None) extends DateRangeValidatable {

}
