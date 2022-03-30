package tech.pegb.backoffice.mapping.domain.dao.i18n

import cats.implicits._
import tech.pegb.backoffice.dao.i18n.dto
import tech.pegb.backoffice.dao.i18n.dto.I18nStringToInsert
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.i18n.dto.{I18nStringCriteria, I18nStringToCreate, I18nStringToUpdate}
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nLocale, I18nPlatform}

object Implicits {

  implicit class I18nStringToCreateAdapter(val arg: I18nStringToCreate) extends AnyVal {
    def asDao = I18nStringToInsert(
      key = arg.key.underlying,
      text = arg.text.underlying,
      locale = arg.locale.underlying,
      platform = arg.platform.underlying,
      `type` = arg.`type`,
      explanation = arg.explanation,
      createdAt = arg.createdAt)
  }

  implicit class I18nStringCriteriaAdapter(val arg: I18nStringCriteria) extends AnyVal {
    def asDao = dto.I18nStringCriteria(
      id = arg.id.map(CriteriaField("", _, MatchTypes.Exact)),
      key = arg.key.map(k ⇒ CriteriaField("key", k.underlying,
        if (arg.partialMatchFields.contains("key")) MatchTypes.Partial else MatchTypes.Exact)),
      explanation = arg.explanation.map(explanation ⇒ CriteriaField("explanation", explanation,
        if (arg.partialMatchFields.contains("explanation")) MatchTypes.Partial else MatchTypes.Exact)),
      `type` = arg.`type`.map(t ⇒ CriteriaField("type", t,
        if (arg.partialMatchFields.contains("type")) MatchTypes.Partial else MatchTypes.Exact)),
      locale = arg.locale.map(l ⇒ CriteriaField("", l.underlying)),
      platform = arg.platform.map(p ⇒ CriteriaField("", p.underlying)))
  }

  implicit class I18nStringLocaleAndPlatformCriteriaAdapter(val arg: (I18nLocale, I18nPlatform)) extends AnyVal {
    def asDao = dto.I18nStringCriteria(
      locale = CriteriaField("", arg._1.underlying).some,
      platform = CriteriaField("", arg._2.underlying).some)
  }

  implicit class I18nUpdateDtoAdapter(val arg: I18nStringToUpdate) extends AnyVal {
    def asDao = dto.I18nStringToUpdate(
      key = arg.key.map(_.underlying),
      text = arg.text.map(_.underlying),
      locale = arg.locale.map(_.underlying),
      platform = arg.platform.map(_.underlying),
      `type` = arg.`type`,
      explanation = arg.explanation,
      updatedAt = arg.updatedAt,
      lastUpdatedAt = arg.lastUpdatedAt)
  }

}
