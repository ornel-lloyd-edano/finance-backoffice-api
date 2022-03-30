package tech.pegb.backoffice.dao.transaction.dto

import java.time.LocalDateTime

case class SettlementFxHistoryCriteria(
    fxProvider: Option[String] = None,
    fromCurrency: Option[String] = None,
    toCurrency: Option[String] = None,
    createdAtFrom: Option[LocalDateTime] = None,
    createdAtTo: Option[LocalDateTime] = None)
