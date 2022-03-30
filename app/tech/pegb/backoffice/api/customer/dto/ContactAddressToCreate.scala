package tech.pegb.backoffice.api.customer.dto

case class ContactAddressToCreate(
    addressType: String,
    country: String,
    city: String,
    postalCode: Option[String],
    address: Option[String],
    coordinateX: Option[BigDecimal],
    coordinateY: Option[BigDecimal])
