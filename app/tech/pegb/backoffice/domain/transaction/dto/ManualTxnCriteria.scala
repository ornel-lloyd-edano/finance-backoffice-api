package tech.pegb.backoffice.domain.transaction.dto

import java.time.LocalDate
import java.util.Currency

import tech.pegb.backoffice.domain.transaction.model.DirectionType
import tech.pegb.backoffice.util.UUIDLike

case class ManualTxnCriteria(
    id: Option[UUIDLike] = None,
    startCreatedAt: Option[LocalDate] = None,
    endCreatedAt: Option[LocalDate] = None,
    accountNumber: Option[String] = None,
    direction: Option[DirectionType] = None,
    currency: Option[Currency] = None)
