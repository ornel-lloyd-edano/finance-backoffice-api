package tech.pegb.backoffice.dao.settings.entity

import java.time.LocalDateTime

import org.coursera.autoschema.annotations.Term

case class SystemSetting(
    id: Int,
    key: String,
    value: String,
    `type`: String,
    explanation: Option[String],
    forAndroid: Boolean,
    forIOS: Boolean,
    forBackoffice: Boolean,
    @Term.Hide createdAt: LocalDateTime,
    @Term.Hide createdBy: String,
    @Term.Hide updatedAt: Option[LocalDateTime],
    @Term.Hide updatedBy: Option[String])
