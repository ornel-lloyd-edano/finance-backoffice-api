package tech.pegb.backoffice.dao.businessuserapplication.entity

import java.time.LocalDateTime

case class BUApplicPrimaryAddress(
    id: Int,
    uuid: String,
    applicationId: Int,
    addressType: String,
    countryId: Int,
    city: String,
    postalCode: Option[String],
    address: Option[String],
    coordinateX: Option[Double],
    coordinateY: Option[Double],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

}

