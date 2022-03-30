package tech.pegb.backoffice.dao.settings.dto

import java.time.LocalDateTime

case class SystemSettingToInsert(
    key: String,
    value: String,
    `type`: String,
    explanation: Option[String],
    forAndroid: Boolean,
    forIOS: Boolean,
    forBackoffice: Boolean,
    createdAt: LocalDateTime,
    createdBy: String)
