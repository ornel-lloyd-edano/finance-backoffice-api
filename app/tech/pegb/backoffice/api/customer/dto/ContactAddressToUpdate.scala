package tech.pegb.backoffice.api.customer.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class ContactAddressToUpdate(
    addressType: Option[String] = None,
    country: Option[String] = None,
    city: Option[String] = None,
    postalCode: Option[String] = None,
    address: Option[String] = None,
    coordinateX: Option[BigDecimal] = None,
    coordinateY: Option[BigDecimal] = None,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
