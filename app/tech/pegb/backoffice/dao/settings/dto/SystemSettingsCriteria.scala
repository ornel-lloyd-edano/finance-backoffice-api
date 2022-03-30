package tech.pegb.backoffice.dao.settings.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class SystemSettingsCriteria(
    id: Option[CriteriaField[Int]] = None,
    key: Option[CriteriaField[String]] = None,
    explanation: Option[CriteriaField[String]] = None,
    forAndroid: Option[CriteriaField[Boolean]] = None,
    forIOS: Option[CriteriaField[Boolean]] = None,
    forBackoffice: Option[CriteriaField[Boolean]] = None)
