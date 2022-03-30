package tech.pegb.backoffice.dao.transaction.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class TransactionCriteria(
    id: Option[CriteriaField[String]] = None,
    primaryAccountUsersUsername: Option[CriteriaField[String]] = None,
    primaryAccountIndividualUsersName: Option[CriteriaField[String]] = None,
    primaryAccountIndividualUsersFullname: Option[CriteriaField[String]] = None,
    primaryAccountBusinessUsersBusinessName: Option[CriteriaField[String]] = None,
    primaryAccountBusinessUsersBrandName: Option[CriteriaField[String]] = None,
    customerId: Option[CriteriaField[String]] = None,
    accountId: Option[CriteriaField[String]] = None,
    accountNumbers: Option[CriteriaField[Seq[String]]] = None,
    createdAt: Option[CriteriaField[_]] = None,
    transactionType: Option[CriteriaField[String]] = None,
    channel: Option[CriteriaField[String]] = None,
    status: Option[CriteriaField[String]] = None,
    currencyCode: Option[CriteriaField[String]] = None,
    direction: Option[CriteriaField[String]] = None,
    accountType: Option[CriteriaField[String]] = None,
    provider: Option[CriteriaField[String]] = None)
