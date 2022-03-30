package tech.pegb.backoffice.dao.reconciliation.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class InternalReconSummaryCriteria(
    maybeId: Option[CriteriaField[String]] = None,
    maybeDateRange: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    maybeAccountNumber: Option[CriteriaField[String]] = None,
    maybeAccountType: Option[CriteriaField[String]] = None,
    maybeUserUuid: Option[CriteriaField[String]] = None,
    maybeAnyCustomerName: Option[CriteriaField[String]] = None,
    maybeStatus: Option[CriteriaField[String]] = None)

