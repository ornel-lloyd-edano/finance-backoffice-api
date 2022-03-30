package tech.pegb.backoffice.dao.i18n.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class I18nString(
    id: Int,
    key: String,
    text: String,
    locale: String,
    platform: String,
    `type`: Option[String],
    explanation: Option[String],
    createdAt: LocalDateTime,
    updatedAt: Option[LocalDateTime])

object I18nString {
  implicit val f = Json.format[I18nString]
}
