package tech.pegb.backoffice.domain.i18n.dto

import java.time.LocalDateTime

import cats.implicits._
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform, I18nText}
import tech.pegb.backoffice.domain.i18n.model.I18nCompoundKey

case class I18nStringToCreate(
    key: I18nKey,
    text: I18nText,
    locale: I18nLocale,
    platform: I18nPlatform,
    `type`: Option[String],
    explanation: Option[String],
    createdAt: LocalDateTime) {

  def getCompoundKey: I18nCompoundKey = {
    I18nCompoundKey(
      key = key,
      locale = locale,
      platform = platform)
  }

  def toUpdateWithIdDto(id: Int): I18nStringToUpdateWithId = {
    I18nStringToUpdateWithId(
      id = id,
      dto = I18nStringToUpdate(
        key = key.some,
        text = text.some,
        locale = locale.some,
        platform = platform.some,
        `type` = `type`,
        explanation = explanation,
        updatedAt = createdAt,
        lastUpdatedAt = none))
  }
}
