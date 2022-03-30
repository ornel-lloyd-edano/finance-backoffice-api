package tech.pegb.backoffice.dao.address.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.address.entity.Address._

case class AddressToUpdate(
    addressType: Option[String] = None,
    countryId: Option[Int] = None,
    city: Option[String] = None,
    postalCode: Option[String] = None,
    address: Option[String] = None,
    coordinateX: Option[BigDecimal] = None,
    coordinateY: Option[BigDecimal] = None,
    isActive: Option[Boolean] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  addressType.foreach(v ⇒ append(cAddressType → v))
  countryId.foreach(v ⇒ append(cCountryId → v))
  city.foreach(v ⇒ append(cCity → v))
  postalCode.foreach(v ⇒ append(cPostalCode → v))
  address.foreach(v ⇒ append(cAddress → v))
  coordinateX.foreach(v ⇒ append(cCoordX → v))
  coordinateY.foreach(v ⇒ append(cCoordY → v))
  isActive.foreach(v ⇒ append(cIsActive → v))
  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)
  lastUpdatedAt.foreach(paramsBuilder += cLastUpdatedAt → _)

}
