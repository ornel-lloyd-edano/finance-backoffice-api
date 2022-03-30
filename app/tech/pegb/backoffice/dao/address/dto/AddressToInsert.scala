package tech.pegb.backoffice.dao.address.dto

import java.time.LocalDateTime

case class AddressToInsert(
    uuid: String,
    userUuid: String,
    addressType: String,
    country: String,
    city: String,
    postalCode: Option[String],
    address: Option[String],
    coordinateX: Option[BigDecimal],
    coordinateY: Option[BigDecimal],
    createdBy: String,
    createdAt: LocalDateTime,
    isActive: Boolean) {

}
