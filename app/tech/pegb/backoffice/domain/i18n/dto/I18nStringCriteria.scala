package tech.pegb.backoffice.domain.i18n.dto

import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform}
import tech.pegb.backoffice.util.HasPartialMatch

case class I18nStringCriteria(
    id: Option[Int] = None,
    key: Option[I18nKey] = None,
    locale: Option[I18nLocale] = None,
    platform: Option[I18nPlatform] = None,
    `type`: Option[String] = None,
    explanation: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch
