package tech.pegb.backoffice.dao.customer.dto

import java.time.LocalDateTime

case class CardApplicationToUpdate(
    operationType: Option[String] = None,
    cardType: Option[String] = None,
    status: Option[String] = None,
    updatedAt: LocalDateTime,
    updatedBy: String)
