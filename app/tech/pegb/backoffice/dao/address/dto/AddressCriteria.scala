package tech.pegb.backoffice.dao.address.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class AddressCriteria(
    uuid: Option[CriteriaField[String]] = None,
    buApplicationUuid: Option[CriteriaField[String]] = None,
    userUuid: Option[CriteriaField[String]] = None,
    addressType: Option[CriteriaField[String]] = None,
    countryName: Option[CriteriaField[String]] = None,
    city: Option[CriteriaField[String]] = None,
    postalCode: Option[CriteriaField[String]] = None,
    address: Option[CriteriaField[String]] = None,
    coordinateX: Option[CriteriaField[BigDecimal]] = None,
    coordinateY: Option[CriteriaField[BigDecimal]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[LocalDateTime]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[LocalDateTime]] = None,
    isActive: Option[CriteriaField[Boolean]] = None) {

}
