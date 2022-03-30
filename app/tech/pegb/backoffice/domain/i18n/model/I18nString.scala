package tech.pegb.backoffice.domain.i18n.model

import java.time.LocalDateTime

import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform, I18nText}

case class I18nString(
    id: Int,
    key: I18nKey,
    text: I18nText,
    locale: I18nLocale,
    platform: I18nPlatform,
    `type`: Option[String],
    explanation: Option[String],
    createdAt: LocalDateTime,
    updatedAt: Option[LocalDateTime])
