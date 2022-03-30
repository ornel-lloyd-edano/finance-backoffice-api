package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

case class UserAndBusinessUserJoinWithAccountJoin(
    id: String,
    password: Option[String],
    tier: Option[String],
    segment: Option[String],
    subscription: Option[String],
    email: Option[String],
    status: Option[String],

    msisdn: Option[String],
    `type`: Option[String],
    company: Option[String],

    activatedAt: Option[LocalDateTime],
    passwordUpdatedAt: Option[LocalDateTime],
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],

    accountNumber: String,
    accountName: Option[String],
    isMainAccount: Boolean,
    currency: String,
    balance: BigDecimal,
    blockedBalance: BigDecimal,
    accountStatus: String,
    closedAt: Option[LocalDateTime],
    lastTransactionAt: Option[LocalDateTime],
    accountCreatedAt: LocalDateTime,
    accountCreatedBy: String,
    accountUpdatedAt: Option[LocalDateTime],
    accountUpdatedBy: Option[String])
