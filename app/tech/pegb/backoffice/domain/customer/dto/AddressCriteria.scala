package tech.pegb.backoffice.domain.customer.dto

import java.time.LocalDateTime
import java.util.UUID

case class AddressCriteria(
    uuid: Option[UUID] = None,
    buApplicationUuid: Option[UUID] = None,
    userUuid: Option[UUID] = None,
    addressType: Option[String] = None,
    countryName: Option[String] = None,
    city: Option[String] = None,
    postalCode: Option[String] = None,
    address: Option[String] = None,
    coordinateX: Option[BigDecimal] = None,
    coordinateY: Option[BigDecimal] = None,
    createdBy: Option[String] = None,
    createdAt: Option[LocalDateTime] = None,
    updatedBy: Option[String] = None,
    updatedAt: Option[LocalDateTime] = None,
    isActive: Option[Boolean] = None)
