package tech.pegb.backoffice.api.businessuserapplication.dto

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

case class BusinessUserApplicationSummaryToRead(
    id: UUID,
    businessName: String,
    brandName: String,
    businessCategory: String,
    stage: String,
    status: String,
    userTier: String,
    businessType: String,
    registrationNumber: String,
    taxNumber: String,
    registrationDate: LocalDate,
    explanation: Option[String],
    submittedBy: Option[String],
    submittedAt: Option[ZonedDateTime],
    submittedRole: Option[Int],
    submittedBusinessUnit: Option[String],
    checkedBy: Option[String],
    checkedAt: Option[ZonedDateTime],
    createdBy: String,
    createdAt: ZonedDateTime,
    updatedBy: Option[String],
    updatedAt: Option[ZonedDateTime],
    readOnly: Boolean)
