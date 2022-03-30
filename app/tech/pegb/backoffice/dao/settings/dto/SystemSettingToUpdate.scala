package tech.pegb.backoffice.dao.settings.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.settings.sql.SystemSettingsSqlDao._

case class SystemSettingToUpdate(
    key: Option[String] = None,
    value: Option[String] = None,
    `type`: Option[String] = None,
    explanation: Option[String] = None,
    forAndroid: Option[Boolean] = None,
    forIOS: Option[Boolean] = None,
    forBackoffice: Option[Boolean] = None,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)
  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)

  key.foreach(x ⇒ append(cKey → x))
  value.foreach(x ⇒ append(cValue → x))
  `type`.foreach(x ⇒ append(cType → x))
  explanation.foreach(x ⇒ append(cExplanation → x))
  forAndroid.foreach(x ⇒ append(cForAndroid → x))
  forIOS.foreach(x ⇒ append(cForIos → x))
  forBackoffice.foreach(x ⇒ append(cForBackOffice → x))

}
