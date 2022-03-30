package tech.pegb.backoffice.dao.businessuserapplication.entity

import java.time.{LocalDate, LocalDateTime}

case class BusinessUserApplication(
    id: Int,
    uuid: String,
    businessName: String,
    brandName: String,
    businessCategory: String,
    stage: String,
    status: String,
    userTier: String,
    businessType: String,
    registrationNumber: String,
    taxNumber: Option[String],
    registrationDate: Option[LocalDate],
    explanation: Option[String],
    userId: Option[Int],
    submittedBy: Option[String],
    submittedAt: Option[LocalDateTime],
    checkedBy: Option[String],
    checkedAt: Option[LocalDateTime],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime])
