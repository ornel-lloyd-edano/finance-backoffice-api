package tech.pegb.backoffice.dao.businessuserapplication.dto

import java.time.LocalDateTime

case class CountryToUpsert(
    name: String,
    officialName: Option[String] = None,
    abbrev: Option[String] = None,
    label: Option[String] = None,
    icon: Option[String] = None,
    currencyId: Option[Int] = None,
    isActive: Option[Boolean] = None,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String] = None,
    updatedAt: Option[LocalDateTime] = None) {

}
