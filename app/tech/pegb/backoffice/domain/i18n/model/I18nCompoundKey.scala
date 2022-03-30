package tech.pegb.backoffice.domain.i18n.model

import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform}

case class I18nCompoundKey(
    key: I18nKey,
    locale: I18nLocale,
    platform: I18nPlatform) {
  override def toString: String = {
    s"{$key, $locale, $platform}"
  }
}
