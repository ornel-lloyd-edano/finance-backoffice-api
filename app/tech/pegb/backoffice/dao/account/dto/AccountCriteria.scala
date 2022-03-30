package tech.pegb.backoffice.dao.account.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class AccountCriteria(
    userId: Option[CriteriaField[String]] = None,
    individualUserFullName: Option[CriteriaField[String]] = None,
    anyCustomerName: Option[CriteriaField[String]] = None,
    msisdn: Option[CriteriaField[String]] = None,
    isMainAccount: Option[CriteriaField[Boolean]] = None,
    currency: Option[CriteriaField[String]] = None,
    status: Option[CriteriaField[String]] = None,
    accountType: Option[CriteriaField[String]] = None,
    accountNumber: Option[CriteriaField[String]] = None,
    accountNumbers: Option[CriteriaField[Set[String]]] = None,
    accountIds: Option[CriteriaField[Set[Int]]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdDateRange: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedDateRange: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None)
