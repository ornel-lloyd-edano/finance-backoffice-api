package tech.pegb.backoffice.dao.businessuserapplication.entity

import java.time.LocalDateTime

case class BUApplicPrimaryContact(
    id: Int,
    uuid: String,
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
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

}
