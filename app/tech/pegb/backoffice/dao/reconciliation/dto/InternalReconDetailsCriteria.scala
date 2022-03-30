package tech.pegb.backoffice.dao.reconciliation.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class InternalReconDetailsCriteria(
    maybeReconSummaryId: Option[CriteriaField[String]] = None,
    mayBeDateRange: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    maybeAccountNumber: Option[CriteriaField[String]] = None,
    maybeCurrency: Option[CriteriaField[String]] = None)
