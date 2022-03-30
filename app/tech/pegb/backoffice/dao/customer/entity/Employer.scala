package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class Employer(
    id: Int,
    employerName: String,
    description: Option[String],
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],
    isActive: Boolean)

object Employer {
  implicit val f = Json.format[Employer]
}
