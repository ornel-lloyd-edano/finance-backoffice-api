package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class User(
    id: Int,
    uuid: String,
    userName: String,
    password: Option[String],
    `type`: Option[String],
    tier: Option[String],
    segment: Option[String],
    subscription: Option[String],
    email: Option[String],
    status: Option[String],
    activatedAt: Option[LocalDateTime],
    passwordUpdatedAt: Option[LocalDateTime],
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

object User {
  implicit val f = Json.format[User]

  def getEmpty = new User(id = -1, uuid = "", userName = "", password = None,
    `type` = None, tier = None, segment = None, subscription = None,
    email = None, status = None, activatedAt = None, passwordUpdatedAt = None,
    createdAt = LocalDateTime.now, createdBy = "", updatedAt = None, updatedBy = None)
}
