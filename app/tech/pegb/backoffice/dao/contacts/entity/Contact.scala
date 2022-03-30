package tech.pegb.backoffice.dao.contacts.entity

import java.time.LocalDateTime

case class Contact(
    id: Int,
    uuid: String,
    buApplicationId: Option[Int],
    buApplicationUUID: Option[String],
    userId: Option[Int],
    userUUID: Option[String],
    contactType: String,
    name: String,
    middleName: Option[String],
    surname: String,
    phoneNumber: String,
    email: String,
    idType: String,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime],
    vpUserId: Option[Int],
    vpUserUUID: Option[String],
    isActive: Boolean) {

}

object Contact {
  val cId = "id"
  val cUuid = "uuid"
  val cBuApplicId = "bu_application_id"
  val cBuApplicUUID = "bu_application_uuid"
  val cUsrId = "user_id"
  val cUsrUUID = "user_uuid"
  val cContactType = "contact_type"
  val cName = "name"
  val cMidName = "middle_name"
  val cSurName = "surname"
  val cPhoneNum = "phone_number"
  val cEmail = "email"
  val cIdType = "id_type"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cUpdatedBy = "updated_by"
  val cUpdatedAt = "updated_at"
  val cVpUserId = "vp_user_id"
  val cVpUserUUID = "vp_user_uuid"
  val cIsActive = "is_active"
}
