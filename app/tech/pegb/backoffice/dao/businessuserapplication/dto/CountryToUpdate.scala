package tech.pegb.backoffice.dao.businessuserapplication.dto

import java.time.LocalDateTime

case class CountryToUpdate(
    icon: Option[String],
    label: Option[String],
    isActive: Option[Boolean],
    officialName: Option[String],
    abbrev: Option[String],
    updatedAt: LocalDateTime,
    updatedBy: String) {

}
