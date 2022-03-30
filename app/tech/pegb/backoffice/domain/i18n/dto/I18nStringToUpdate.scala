package tech.pegb.backoffice.domain.i18n.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform, I18nText}

case class I18nStringToUpdate(
    key: Option[I18nKey] = None,
    text: Option[I18nText] = None,
    locale: Option[I18nLocale] = None,
    platform: Option[I18nPlatform] = None,
    `type`: Option[String] = None,
    explanation: Option[String] = None,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime])

