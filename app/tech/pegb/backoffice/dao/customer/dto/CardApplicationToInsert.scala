package tech.pegb.backoffice.dao.customer.dto

import java.time.LocalDateTime

case class CardApplicationToInsert(
    userId: String,
    operationType: String,
    cardType: String,
    nameOnCard: String,
    cardPin: String,
    deliveryAddress: String,
    status: String,
    createdAt: LocalDateTime,
    createdBy: String)
