package tech.pegb.backoffice.dao.customer.entity

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import ai.x.play.json.Jsonx

case class IndividualUser(
    id: Int,
    uuid: String,
    username: Option[String],
    password: Option[String],
    tier: Option[String],
    segment: Option[String],
    subscription: Option[String],
    email: Option[String],
    status: String,
    msisdn: String,
    `type`: Option[String],
    name: Option[String],
    fullName: Option[String],
    gender: Option[String],
    personId: Option[String],
    documentNumber: Option[String],
    documentType: Option[String],
    documentModel: Option[String] = None, //quickly fix test compile errors
    company: Option[String],
    birthDate: Option[LocalDate],
    birthPlace: Option[String],
    nationality: Option[String],
    occupation: Option[String],
    employer: Option[String],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime],
    activatedAt: Option[LocalDateTime] = None)

object IndividualUser {
  implicit val f = Jsonx.formatCaseClass[IndividualUser]

  def getEmpty = new IndividualUser(
    id = 0,
    uuid = UUID.randomUUID().toString,
    username = None,
    password = None,
    tier = None,
    segment = None,
    subscription = None,
    email = None,
    status = "active",
    msisdn = "111111111111",
    `type` = None,
    name = None,
    fullName = None,
    gender = None,
    personId = None,
    documentNumber = None,
    documentType = None,
    documentModel = None,
    company = None,
    birthDate = None,
    birthPlace = None,
    nationality = None,
    occupation = None,
    employer = None,
    createdBy = "",
    createdAt = LocalDateTime.now,
    updatedBy = None,
    updatedAt = None,
    activatedAt = None)
}
