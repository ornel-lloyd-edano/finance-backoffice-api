package tech.pegb.backoffice.dao.i18n.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class I18nStringCriteria(
    id: Option[CriteriaField[Int]] = None,
    key: Option[CriteriaField[String]] = None,
    explanation: Option[CriteriaField[String]] = None,
    `type`: Option[CriteriaField[String]] = None,
    locale: Option[CriteriaField[String]] = None,
    platform: Option[CriteriaField[String]] = None)
