package tech.pegb.backoffice.dao.customer.entity

import java.time.{LocalDate, LocalDateTime}

case class UserAndBusinessUserJoin(
    uuid: String,
    username: Option[String],
    password: Option[String],
    tier: Option[String],
    segment: Option[String],
    subscription: String,
    email: Option[String],
    status: String,
    businessUserName: String,
    activatedAt: Option[LocalDateTime],
    passwordUpdatedAt: Option[LocalDateTime],
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

object UserAndBusinessUserJoin {
  def getEmpty = UserAndBusinessUserJoin(
    uuid = "",
    username = None,
    password = None,
    tier = None,
    segment = None,
    subscription = "",
    email = None,
    status = "",
    businessUserName = "",
    activatedAt = None,
    passwordUpdatedAt = None,
    createdAt = LocalDateTime.now,
    createdBy = "",
    updatedAt = None,
    updatedBy = None)
}

case class UserAndBusinessUserJoinGetCriteria(
    tier: Option[String] = None,
    subscription: Option[String] = None,
    segment: Option[String] = None,
    businessUserName: Option[String] = None,
    status: Option[String] = None,
    createdBy: Option[String] = None,
    createdDateFrom: Option[LocalDate] = None,
    createdDateTo: Option[LocalDate] = None,
    updatedBy: Option[String] = None,
    updatedDateFrom: Option[LocalDate] = None,
    updatedDateTo: Option[LocalDate] = None)
