package tech.pegb.backoffice.dao.businessuserapplication.dto

import java.time.LocalDateTime

case class BUApplicPrimaryContactToInsert(
    applicationId: Int,
    contactType: String,
    name: String,
    middleName: Option[String],
    surname: String,
    phoneNumber: String,
    email: String,
    idType: String,
    isVelocityUser: Boolean,
    velocityLevel: Option[String],
    isDefaultContact: Option[Boolean],
    createdBy: String,
    createdAt: LocalDateTime) {

}
