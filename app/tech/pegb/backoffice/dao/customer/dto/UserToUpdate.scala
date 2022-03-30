package tech.pegb.backoffice.dao.customer.dto

import java.time.LocalDateTime

case class UserToUpdate(
    userName: Option[String],
    password: Option[String],
    `type`: Option[String],
    tier: Option[String],
    segment: Option[String],
    subscription: Option[String],
    email: Option[String],
    status: Option[String],
    activatedAt: Option[LocalDateTime],
    passwordUpdatedAt: Option[LocalDateTime],
    updatedAt: LocalDateTime,
    updatedBy: String)

object UserToUpdate {
  def getEmpty = UserToUpdate(userName = None, password = None, `type` = None, tier = None, segment = None,
    subscription = None, email = None, status = None, activatedAt = None, passwordUpdatedAt = None,
    updatedAt = LocalDateTime.now, updatedBy = "")
}
