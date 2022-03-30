package tech.pegb.backoffice.domain.i18n.model

import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nText}

case class I18nPair(
    key: I18nKey,
    text: I18nText)
