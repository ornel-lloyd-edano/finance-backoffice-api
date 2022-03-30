package tech.pegb.backoffice.dao.address.entity

import java.time.LocalDateTime

case class Address(
    id: Int,
    uuid: String,
    buApplicationId: Option[Int],
    buApplicationUuid: Option[String],
    userId: Option[Int],
    userUuid: Option[String],
    addressType: String,
    countryId: Int,
    countryName: String,
    city: String,
    postalCode: Option[String],
    address: Option[String],
    coordinateX: Option[BigDecimal],
    coordinateY: Option[BigDecimal],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime],
    isActive: Boolean) {

}

object Address {
  val cId = "id"
  val cUuid = "uuid"
  val cBuApplicId = "bu_application_id"
  val cBuApplicUuid = "bu_application_uuid"
  val cUsrId = "user_id"
  val cUsrUuid = "user_uuid"
  val cAddressType = "address_type"
  val cCountryId = "country_id"
  val cCountry = "country_name"
  val cCity = "city"
  val cPostalCode = "postal_code"
  val cAddress = "address"
  val cCoordX = "coordinate_x"
  val cCoordY = "coordinate_y"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cUpdatedBy = "updated_by"
  val cUpdatedAt = "updated_at"
  val cIsActive = "is_active"
}
