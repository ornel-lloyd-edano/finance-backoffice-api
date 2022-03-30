package tech.pegb.backoffice.domain.settings.model

import java.time.LocalDateTime

case class SystemSetting(
    id: Int,
    key: String,
    value: String,
    `type`: String,
    explanation: Option[String],
    forAndroid: Boolean,
    forIOS: Boolean,
    forBackoffice: Boolean,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])
