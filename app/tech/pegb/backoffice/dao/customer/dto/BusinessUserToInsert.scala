package tech.pegb.backoffice.dao.customer.dto

import java.time.{LocalDate, LocalDateTime}

case class BusinessUserToInsert(
    userId: Int,

    businessName: String,
    brandName: String,
    businessCategory: String,
    businessType: String,

    registrationNumber: String,
    taxNumber: String,
    registrationDate: LocalDate,
    currencyId: Int,
    collectionAccountId: Option[Int],
    distributionAccountId: Option[Int],
    defaultContactId: Option[Int],

    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

object BusinessUserToInsert {
  def getEmpty = BusinessUserToInsert(
    userId = 1,
    businessName = "",
    brandName = "",
    businessCategory = "",
    businessType = "",

    registrationNumber = "",
    taxNumber = "",
    registrationDate = LocalDate.of(1970, 1, 1),
    currencyId = 1,
    collectionAccountId = None,
    distributionAccountId = None,
    defaultContactId = None,

    createdAt = LocalDateTime.of(1970, 1, 1, 0, 0, 0),
    createdBy = "",
    updatedAt = None,
    updatedBy = None)
}
