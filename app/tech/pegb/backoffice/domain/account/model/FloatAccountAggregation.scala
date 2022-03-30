package tech.pegb.backoffice.domain.account.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountMainType, AccountNumber, AccountType}

case class FloatAccountAggregation(
    userUuid: UUID,
    userName: String,
    accountUuid: UUID,
    accountNumber: AccountNumber,
    accountType: AccountType,
    accountMainType: AccountMainType,
    currency: Currency,
    internalBalance: BigDecimal,
    externalBalance: Option[BigDecimal],
    inflow: BigDecimal,
    outflow: BigDecimal,
    net: BigDecimal,
    createdAt: LocalDateTime,
    updatedAt: Option[LocalDateTime])
