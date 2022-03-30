package tech.pegb.backoffice.dao.currencyexchange.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class SpreadCriteria(
    id: Option[CriteriaField[String]] = None,
    currencyExchangeId: Option[CriteriaField[String]] = None,
    currencyCode: Option[String] = None,
    transactionType: Option[String] = None,
    channel: Option[String] = None,
    recipientInstitution: Option[String] = None,
    isDeletedAtNotNull: Option[Boolean] = None)
