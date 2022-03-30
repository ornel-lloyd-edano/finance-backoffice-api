package tech.pegb.backoffice.dao.settings.dto

case class SystemSettingParameter(
    key: Option[String],
    value: Option[String],
    `type`: Option[String],
    explanation: Option[String],
    forAndroid: Option[Boolean],
    forIOS: Option[Boolean],
    forBackoffice: Option[Boolean])
