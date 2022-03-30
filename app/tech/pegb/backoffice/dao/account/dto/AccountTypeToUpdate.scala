package tech.pegb.backoffice.dao.account.dto

import java.time.LocalDateTime

//TODO add lastUpdatedAt: Option[LocalDateTime]
case class AccountTypeToUpdate(
    accountTypeName: Option[String],
    description: Option[String],
    isActive: Option[Boolean],
    updatedAt: LocalDateTime,
    updatedBy: String)
