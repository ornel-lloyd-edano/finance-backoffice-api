package tech.pegb.backoffice.dao.customer.dto

import java.time.LocalDateTime

case class NationalityToInsert(
    name: String,
    description: String,
    createdAt: Option[LocalDateTime],
    createdBy: Option[String],
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],
    isActive: Boolean)
