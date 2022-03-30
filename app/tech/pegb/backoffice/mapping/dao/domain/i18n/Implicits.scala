package tech.pegb.backoffice.mapping.dao.domain.i18n

import tech.pegb.backoffice.dao.i18n.entity.{I18nPair, I18nString}
import tech.pegb.backoffice.domain.i18n.model
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform, I18nText}
object Implicits {

  implicit class I18nStringAdapter(val arg: I18nString) extends AnyVal {
    def asDomain = {
      model.I18nString(
        id = arg.id,
        key = I18nKey(arg.key),
        text = I18nText(arg.text),
        locale = I18nLocale(arg.locale),
        platform = I18nPlatform(arg.platform),
        `type` = arg.`type`,
        explanation = arg.explanation,
        createdAt = arg.createdAt,
        updatedAt = arg.updatedAt)
    }

    def asCompoundKey = {
      model.I18nCompoundKey(
        key = I18nKey(arg.key),
        locale = I18nLocale(arg.locale),
        platform = I18nPlatform(arg.platform))
    }
  }

  implicit class I18nPairAdapter(val arg: I18nPair) extends AnyVal {
    def asDomain = {
      model.I18nPair(
        key = I18nKey(arg.key),
        text = I18nText(arg.text))
    }
  }
}
