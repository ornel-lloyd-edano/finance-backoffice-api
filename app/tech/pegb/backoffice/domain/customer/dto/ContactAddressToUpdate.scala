package tech.pegb.backoffice.domain.customer.dto

import java.time.LocalDateTime

case class ContactAddressToUpdate(
    addressType: Option[String] = None,
    country: Option[String] = None,
    city: Option[String] = None,
    postalCode: Option[String] = None,
    address: Option[String] = None,
    coordinateX: Option[BigDecimal] = None,
    coordinateY: Option[BigDecimal] = None,
    isActive: Option[Boolean] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime])
