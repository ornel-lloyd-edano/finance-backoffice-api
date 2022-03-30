package tech.pegb.backoffice.domain.settings.dto

case class SystemSettingsCriteria(
    id: Option[Int] = None,
    key: Option[String] = None,
    explanation: Option[String] = None,
    forAndroid: Option[Boolean] = None,
    forIOS: Option[Boolean] = None,
    forBackoffice: Option[Boolean] = None)
