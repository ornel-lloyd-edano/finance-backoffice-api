package tech.pegb.backoffice.mapping.api.domain.account

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.{Currency, UUID}

import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.customer.dto.{CustomerExternalAccountToCreate, ExternalAccountToCreate, ExternalAccountToUpdate}
import tech.pegb.backoffice.domain.account.dto.AccountCriteria
import tech.pegb.backoffice.domain.account.model.AccountAttributes._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{MsisdnLike, NameAttribute}
import tech.pegb.backoffice.util.{Constants, UUIDLike}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.account.{dto ⇒ domain}

import scala.util.Try

object Implicits {

  implicit class AccountToCreateMapper(val arg: api.customer.dto.AccountToCreate) extends AnyVal {
    def asDomain(createdBy: String, createdAt: LocalDateTime) = Try(domain.AccountToCreate(
      customerId = arg.customerId,
      accountNumber = None,
      accountName = None,
      accountType = AccountType(arg.`type`),
      isMainAccount = false,
      currency = Currency.getInstance(arg.currency),
      initialBalance = None,
      accountStatus = None,
      mainType = AccountMainType(Constants.Liability),
      createdBy = createdBy,
      createdAt = createdAt))
  }

  implicit class AccountCriteriaApiAdapter(val arg: (UUID, Option[Boolean], Option[String], Option[String], Option[String], Option[String])) extends AnyVal {
    def asDomain = AccountCriteria(
      customerId = arg._1.toUUIDLike.toOption,
      isMainAccount = arg._2,
      currency = arg._3.map(_.sanitize.toUpperCase),
      status = arg._4.map(_.sanitize.toUpperCase),
      accountType = arg._5.map(_.sanitize.toUpperCase),
      accountNumber = arg._6.map(_.sanitize))
  }

  implicit class AccountCriteriaByCustomerIdApiAdapter(val arg: UUID) extends AnyVal {
    def toAccountCriteria: AccountCriteria = AccountCriteria(customerId = arg.toUUIDLike.toOption)
  }

  private type CustomerId = Option[UUIDLike]
  private type CustomerFullName = Option[String]
  private type Msisdn = Option[String]
  private type IsMainAccount = Option[Boolean]
  private type Currency = Option[String]
  private type Status = Option[String]
  private type AccountType = Option[String]
  private type AccountNumber = Option[String]
  private type PartialMatchFields = Set[String]
  private type AnyCustomerName = Option[String]
  implicit class QueryParamsToAccountCriteriaAdapter(val arg: (CustomerId, CustomerFullName, AnyCustomerName, Msisdn, IsMainAccount, Currency, Status, AccountType, AccountNumber, PartialMatchFields)) extends AnyVal {
    def asDomain = Try(AccountCriteria(
      customerId = arg._1,
      customerFullName = arg._2.map(_.sanitize),
      anyCustomerName = arg._3.map(name ⇒ NameAttribute(name.sanitize)),
      msisdn = arg._4.map(MsisdnLike(_)),
      isMainAccount = arg._5,
      currency = arg._6.map(_.sanitize),
      status = arg._7.map(_.sanitize),
      accountType = arg._8.map(_.sanitize),
      accountNumber = arg._9.map(_.sanitize),
      partialMatchFields = arg._10))
  }

  implicit class CustomerExternalAccountToCreateDomainAdapter(val arg: CustomerExternalAccountToCreate) extends AnyVal {
    def asDomain(requestId: UUID, customerId: UUID, doneBy: String, doneAt: ZonedDateTime): domain.ExternalAccountToCreate = {
      domain.ExternalAccountToCreate(
        id = requestId,
        customerId = customerId,
        externalProvider = arg.provider.sanitize,
        externalAccountNumber = arg.accountNumber.sanitize,
        externalAccountHolder = arg.accountHolder.sanitize,
        currency = arg.currency.sanitize,
        createdBy = doneBy,
        createdAt = doneAt.toLocalDateTimeUTC)
    }
  }

  implicit class ExternalAccountToCreateDomainAdapter(val arg: ExternalAccountToCreate) extends AnyVal {
    def asDomain(requestId: UUID, doneBy: String, doneAt: ZonedDateTime): domain.ExternalAccountToCreate = {
      domain.ExternalAccountToCreate(
        id = requestId,
        customerId = arg.customerId,
        externalProvider = arg.provider.sanitize,
        externalAccountNumber = arg.accountNumber.sanitize,
        externalAccountHolder = arg.accountHolder.sanitize,
        currency = arg.currency.sanitize,
        createdBy = doneBy,
        createdAt = doneAt.toLocalDateTimeUTC)
    }
  }

  implicit class ExternalAccountToUpdateDomainAdapter(val arg: ExternalAccountToUpdate) extends AnyVal {
    def asDomain(doneBy: String, doneAt: ZonedDateTime): domain.ExternalAccountToUpdate = {
      domain.ExternalAccountToUpdate(
        externalProvider = arg.provider.map(_.sanitize),
        externalAccountHolder = arg.accountHolder.map(_.sanitize),
        externalAccountNumber = arg.accountNumber.map(_.sanitize),
        currency = arg.currency.map(_.sanitize),
        updatedBy = doneBy,
        updatedAt = doneAt.toLocalDateTimeUTC,
        lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
    }
  }

  private type ExternalAccountId = Option[UUIDLike]
  private type Provider = Option[String]
  private type AccountHolder = Option[String]
  implicit class ExternalAccountQueryParamsToDomainAdapter(val arg: (ExternalAccountId, CustomerId, Provider, AccountHolder, AccountNumber, Currency, PartialMatchFields)) extends AnyVal {
    def asDomain: domain.ExternalAccountCriteria = {
      domain.ExternalAccountCriteria(
        id = arg._1,
        customerId = arg._2,
        externalProvider = arg._3.map(_.sanitize),
        externalAccountHolder = arg._4.map(_.sanitize),
        externalAccountNumber = arg._5.map(_.sanitize),
        currency = arg._6.map(_.sanitize),
        partialMatchFields = arg._7)
    }
  }

  implicit class ExternalCustomerAccountQueryParamsToDomainAdapter(val arg: (ExternalAccountId, CustomerId)) extends AnyVal {
    def asDomain: domain.ExternalAccountCriteria = {
      domain.ExternalAccountCriteria(id = arg._1, customerId = arg._2)
    }
  }

}
