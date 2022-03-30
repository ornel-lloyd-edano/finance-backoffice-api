package tech.pegb.backoffice.dao.businessuserapplication.dto

import java.time.LocalDateTime

case class BUApplicPrimaryAddressToInsert(
    applicationId: Int,
    addressType: String,
    countryId: Int,
    city: String,
    postalCode: Option[String],
    address: Option[String],
    coordinateX: Option[Double],
    coordinateY: Option[Double],
    createdBy: String,
    createdAt: LocalDateTime) {

}
