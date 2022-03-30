package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

case class VelocityPortalUser(
    id: Int,
    uuid: String,
    userId: Int,
    userUUID: String,
    name: String,
    middleName: Option[String],
    surname: String,
    msisdn: String,
    email: String,
    username: String,
    role: String,
    status: String,
    lastLoginAt: Option[LocalDateTime],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime])

object VelocityPortalUser {
  val cId = "id"
  val cUuid = "uuid"
  val cUserId = "user_id"
  val cName = "name"
  val cMiddleName = "middle_name"
  val cSurname = "surname"
  val cMsisdn = "msisdn"
  val cEmail = "email"
  val cUsername = "username"
  val cRole = "role"
  val cStatus = "status"
  val cLastLoginAt = "last_login_at"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cUpdatedBy = "updated_by"
  val cUpdatedAt = "updated_at"
}
