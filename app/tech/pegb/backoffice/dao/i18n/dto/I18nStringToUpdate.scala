package tech.pegb.backoffice.dao.i18n.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.i18n.sql.I18nStringSqlDao._

case class I18nStringToUpdate(
    key: Option[String] = None,
    text: Option[String] = None,
    locale: Option[String] = None,
    platform: Option[String] = None,
    `type`: Option[String] = None,
    explanation: Option[String] = None,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  append(cUpdatedAt → updatedAt)
  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)

  key.foreach(x ⇒ append(cKey → x))
  text.foreach(x ⇒ append(cText → x))
  locale.foreach(x ⇒ append(cLocale → x))
  platform.foreach(x ⇒ append(cPlatform → x))
  `type`.foreach(x ⇒ append(cType → x))
  explanation.foreach(x ⇒ append(cExplanation → x))
}

