package tech.pegb.backoffice.dao.customer.entity

import java.time.{LocalDate, LocalDateTime}

case class BusinessUser(
    id: Int,
    uuid: String,
    userId: Int,
    userUUID: String,

    businessName: String,
    brandName: String,
    businessCategory: String,
    businessType: String,

    registrationNumber: String,
    taxNumber: Option[String],
    registrationDate: Option[LocalDate],
    currencyId: Int,
    collectionAccountId: Option[Int],
    collectionAccountUUID: Option[String],
    distributionAccountId: Option[Int],
    distributionAccountUUID: Option[String],

    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])
