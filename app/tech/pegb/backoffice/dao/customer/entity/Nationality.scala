package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class Nationality(
    id: Int,
    name: String,
    description: Option[String],
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],
    isActive: Boolean)

object Nationality {
  implicit val f = Json.format[Nationality]
}
