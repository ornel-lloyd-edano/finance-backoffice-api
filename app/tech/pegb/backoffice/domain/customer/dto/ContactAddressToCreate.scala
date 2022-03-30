package tech.pegb.backoffice.domain.customer.dto

import java.time.LocalDateTime
import java.util.UUID

case class ContactAddressToCreate(
    uuid: UUID,
    userUuid: UUID,
    addressType: String,
    country: String,
    city: String,
    postalCode: Option[String],
    address: Option[String],
    coordinateX: Option[BigDecimal],
    coordinateY: Option[BigDecimal],
    createdBy: String,
    createdAt: LocalDateTime,
    isActive: Boolean)
