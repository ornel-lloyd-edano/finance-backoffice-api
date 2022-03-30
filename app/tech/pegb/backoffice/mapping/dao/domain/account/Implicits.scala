package tech.pegb.backoffice.mapping.dao.domain.account

import java.util.{Currency, UUID}

import tech.pegb.backoffice.dao
import tech.pegb.backoffice.domain.account
import tech.pegb.backoffice.domain.account.model.{Account, ExternalAccount}
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountMainType, AccountNumber, AccountStatus, AccountType}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{Msisdn, NameAttribute}
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

object Implicits {

  implicit class AccountAdapter(arg: dao.account.entity.Account) {
    def asDomain: Try[Account] = Try {
      account.model.Account(
        id = UUID.fromString(arg.uuid),
        customerId = UUID.fromString(arg.userUuid),
        anyCustomerName = getCustomerName(arg.anyCustomerName, arg.brandName),
        userName = arg.userName,
        //TODO just make a pattern match on (individual fullname, individual name, business name) case _=> arg.userName
        customerName = if (arg.userType === "individual") {
          arg.individualUserFullName.orElse(arg.individualUserName)
        } else arg.userName,
        msisdn = arg.msisdn.map(Msisdn(_)),
        accountNumber = AccountNumber(arg.accountNumber),
        accountName = NameAttribute(arg.accountName),
        accountType = AccountType(arg.accountType),
        isMainAccount = arg.isMainAccount.getOrElse(false),
        currency = Currency.getInstance(arg.currency),
        balance = arg.balance.getOrElse(BigDecimal(0)),
        blockedBalance = arg.blockedBalance.getOrElse(BigDecimal(0)),
        dailyTotalTransactionAmount = None,
        lastDayBalance = None,
        accountStatus = AccountStatus(arg.status.getOrElse("unknown")),
        lastTransactionAt = arg.lastTransactionAt,
        mainType = AccountMainType(arg.mainType),
        createdAt = arg.createdAt,
        createdBy = Some(arg.createdBy),
        updatedAt = arg.updatedAt,
        updatedBy = arg.updatedBy)
    }

    private def getCustomerName(maybeName: Option[String], maybeBrandName: Option[String]): Option[String] = {
      (maybeName, maybeBrandName) match {
        case (Some(name), Some(brandName)) if name == brandName ⇒ Some(name)
        case (Some(name), Some(brandName)) ⇒ Some(s"$name ($brandName)")
        case (None, Some(brandName)) ⇒ Some(brandName)
        case (Some(name), None) ⇒ Some(name)
        case _ ⇒ None
      }
    }
  }

  implicit class ExternalAccountEntityToDomainAdapter(val arg: dao.account.entity.ExternalAccount) extends AnyVal {
    def asDomain = ExternalAccount(
      id = arg.uuid,
      customerId = arg.userUuid,
      externalProvider = arg.provider,
      externalAccountNumber = arg.accountNumber,
      externalAccountHolder = arg.accountHolder,
      currency = arg.currencyName,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt)
  }
}
