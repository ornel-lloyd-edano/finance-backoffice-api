package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class Company(
    id: Int,
    companyName: String,
    companyFullName: Option[String],
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],
    isActive: Boolean)

object Company {
  implicit val f = Json.format[Company]
}
