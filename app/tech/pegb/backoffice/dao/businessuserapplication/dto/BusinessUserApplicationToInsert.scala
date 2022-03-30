package tech.pegb.backoffice.dao.businessuserapplication.dto

import java.time.{LocalDate, LocalDateTime}

case class BusinessUserApplicationToInsert(
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
    createdBy: String,
    createdAt: LocalDateTime)
