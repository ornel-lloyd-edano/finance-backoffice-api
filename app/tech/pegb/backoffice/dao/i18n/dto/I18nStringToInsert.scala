package tech.pegb.backoffice.dao.i18n.dto

import java.time.LocalDateTime

case class I18nStringToInsert(
    key: String,
    text: String,
    locale: String,
    platform: String,
    `type`: Option[String],
    explanation: Option[String],
    createdAt: LocalDateTime)

