package tech.pegb.backoffice.domain.aggregations.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.util.time.DateTimeRangeValidatable

case class TxnAggregationsCriteria(
    currencyCode: Option[String] = None,
    institution: Option[String] = None,
    transactionType: Option[String] = None,
    userType: Option[String] = None,
    isAnyOfTheseTxnTypes: Seq[String] = Nil,
    exceptTheseTxnTypes: Seq[String] = Nil,
    accountType: Option[String] = None,
    isAnyTheseAccountTypes: Option[Set[String]] = None,
    startDate: Option[LocalDateTime] = None,
    endDate: Option[LocalDateTime] = None,
    otherParty: Option[String] = None,
    notLikeThisAccountNumber: Option[String] = None) extends DateTimeRangeValidatable {

}
