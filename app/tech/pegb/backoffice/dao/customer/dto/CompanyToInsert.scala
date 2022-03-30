package tech.pegb.backoffice.dao.customer.dto

import java.time.LocalDateTime

case class CompanyToInsert(
    name: String,
    fullName: Option[String],
    createdAt: Option[LocalDateTime],
    createdBy: Option[String],
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],
    isActive: Boolean)
