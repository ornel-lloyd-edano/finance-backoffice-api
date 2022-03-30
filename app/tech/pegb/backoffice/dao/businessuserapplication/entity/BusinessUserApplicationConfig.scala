package tech.pegb.backoffice.dao.businessuserapplication.entity

import java.time.LocalDateTime

trait BusinessUserApplicationConfig

case class AccountConfig(
    id: Int,
    uuid: String,
    applicationId: Int,
    accountType: String,
    accountName: String,
    currencyId: Int,
    currencyCode: String,
    isDefault: Boolean,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String]) extends BusinessUserApplicationConfig

case class TransactionConfig(
    id: Int,
    uuid: String,
    applicationId: Int,
    transactionType: String,
    currencyId: Int,
    currencyCode: String,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String]) extends BusinessUserApplicationConfig

case class ExternalAccount(
    id: Int,
    uuid: String,
    applicationId: Int,
    provider: String,
    accountNumber: String,
    accountHolder: String,
    currencyId: Int,
    currencyCode: String,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String]) extends BusinessUserApplicationConfig

