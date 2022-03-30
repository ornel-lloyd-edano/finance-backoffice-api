package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class Occupation(
    id: Int,
    name: String,
    description: Option[String],
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],
    isActive: Boolean)

object Occupation {
  implicit val f = Json.format[Occupation]
}
