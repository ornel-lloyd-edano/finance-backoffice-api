package tech.pegb.backoffice.domain.customer.dto

import java.time.LocalDateTime
import java.util.UUID

case class SavingOptionCriteria(
    id: Option[Int] = None,
    uuid: Option[UUID] = None,
    userId: Option[Int] = None,
    userUuid: Option[UUID] = None,
    accountId: Option[Int] = None,
    accountUuid: Option[UUID] = None,
    currency: Option[String] = None,
    isActive: Option[Boolean] = None,
    currentAmount: Option[BigDecimal] = None,
    statusUpdatedAt: Option[LocalDateTime] = None,
    createdAt: Option[LocalDateTime] = None)
