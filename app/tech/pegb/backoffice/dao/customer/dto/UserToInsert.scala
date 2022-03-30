package tech.pegb.backoffice.dao.customer.dto

import java.time.LocalDateTime

case class UserToInsert(
    userName: String,
    password: Option[String],
    `type`: Option[String],
    tier: Option[String],
    segment: Option[String],
    subscription: Option[String],
    email: Option[String],
    status: String,
    createdAt: LocalDateTime,
    createdBy: String)

object UserToInsert {
  def getEmpty = UserToInsert(userName = "", password = None, `type` = None, tier = None, segment = None,
    subscription = None, email = None, status = "", createdAt = LocalDateTime.now, createdBy = "")
}
