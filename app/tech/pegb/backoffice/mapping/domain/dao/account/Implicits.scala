package tech.pegb.backoffice.mapping.domain.dao.account

import tech.pegb.backoffice.dao.account.dto.AccountCriteria
import tech.pegb.backoffice.dao.account.entity.ExternalAccount
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao._
import tech.pegb.backoffice.dao.account.sql.{AccountSqlDao, AccountTypesSqlDao}
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.IndividualUserSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.account.{dto ⇒ dao}
import tech.pegb.backoffice.domain.account.{dto ⇒ domain}
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.util.Constants

import scala.util.Try

object Implicits {

  implicit class AccountCriteriaConverter(val arg: domain.AccountCriteria) extends AnyVal {
    def asDao: AccountCriteria = {

      val partialFields = arg.partialMatchFields

      dao.AccountCriteria(
        userId = arg.customerId.map(id ⇒ CriteriaField(cUserId, id.underlying, partialFields.withMatchType(Constants.customerId))),
        individualUserFullName = arg.customerFullName.map(fullName ⇒ CriteriaField("", fullName, partialFields.withMatchType(Constants.customerFullName))),
        //TODO it is not clear to which actual columns you are mapping anyCustomerName, better map it to the actual ones
        anyCustomerName = arg.anyCustomerName.map(x ⇒ CriteriaField("", x.underlying, if (arg.partialMatchFields.contains("any_customer_name")) MatchTypes.Partial else MatchTypes.Exact)),
        msisdn = arg.msisdn.map(m ⇒ CriteriaField(IndividualUserSqlDao.msisdn, m.underlying, partialFields.withMatchType(Constants.msisdn))),
        isMainAccount = arg.isMainAccount.map(CriteriaField(AccountSqlDao.cIsMainAccount, _)),
        currency = arg.currency.map(CriteriaField(CurrencySqlDao.cName, _, MatchTypes.Exact)),
        status = arg.status.map(CriteriaField(AccountSqlDao.cStatus, _)),
        accountType = arg.accountType.map(CriteriaField(AccountTypesSqlDao.cAccountType, _)),
        accountNumber = arg.accountNumber.map(accNum ⇒ CriteriaField(AccountSqlDao.cNumber, accNum, partialFields.withMatchType(Constants.accountNumber))),
        accountNumbers = arg.accountNumbers.map(numbers ⇒ CriteriaField(AccountSqlDao.cNumber, numbers, MatchTypes.In)),
        createdBy = None,
        createdDateRange = None,
        updatedBy = None,
        updatedDateRange = None)
    }
  }

  implicit class AccountToCreateConverter(val arg: domain.AccountToCreate) extends AnyVal {

    def asDao = Try(dao.AccountToInsert(
      accountNumber = arg.accountNumber.map(_.underlying).get,
      userId = arg.customerId.toString,
      accountName = arg.accountName.map(_.underlying).get,
      accountType = arg.accountType.underlying.toLowerCase,
      isMainAccount = arg.isMainAccount,
      currency = arg.currency.getCurrencyCode,
      balance = arg.initialBalance.getOrElse(BigDecimal(0)),
      blockedBalance = BigDecimal(0),
      status = arg.accountStatus.map(_.underlying).get.toLowerCase,
      mainType = arg.mainType.underlying,
      createdAt = arg.createdAt,
      createdBy = arg.createdBy))
  }

  implicit class AccountNumberListToAccountCriteriaAdapter(val arg: Seq[String]) extends AnyVal {

    def asDao = dao.AccountCriteria(accountNumbers = Some(CriteriaField("", arg.toSet, MatchTypes.In)))
  }

  implicit class ExternalAccountToCreateDaoAdapter(val arg: domain.ExternalAccountToCreate) extends AnyVal {
    def asDao(userId: Int, currencyId: Int) = dao.ExternalAccountToCreate(
      uuid = arg.id,
      userId = userId,
      provider = arg.externalProvider,
      accountNumber = arg.externalAccountNumber,
      accountHolder = arg.externalAccountHolder,
      currencyId = currencyId,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt)
  }

  implicit class ExternalAccountToUpdateDaoAdapter(val arg: domain.ExternalAccountToUpdate) extends AnyVal {
    def asDao(currencyId: Option[Int]) = dao.ExternalAccountToUpdate(
      provider = arg.externalProvider,
      accountNumber = arg.externalAccountNumber,
      accountHolder = arg.externalAccountHolder,
      currencyId = currencyId,
      updatedBy = Some(arg.updatedBy),
      updatedAt = Some(arg.updatedAt),
      lastUpdatedAt = arg.lastUpdatedAt)
  }

  implicit class ExternalAccountCriteriaDaoAdapter(val arg: domain.ExternalAccountCriteria) extends AnyVal {
    def asDao() = dao.ExternalAccountCriteria(
      uuid = arg.id.map(v ⇒ CriteriaField(ExternalAccount.cUuid, v.toString, arg.partialMatchFields)),
      anyUuid = arg.anyIds.map(v ⇒ CriteriaField(ExternalAccount.cUuid, v.map(_.toString))),
      userUuid = arg.customerId.map(v ⇒ CriteriaField(ExternalAccount.cUserUUid, v.toString, arg.partialMatchFields)),
      provider = arg.externalProvider.map(v ⇒ CriteriaField(ExternalAccount.cProvider, v.toString, arg.partialMatchFields)),
      accountHolder = arg.externalAccountHolder.map(v ⇒ CriteriaField(ExternalAccount.cAccountHolder, v.toString, arg.partialMatchFields)),
      accountNumber = arg.externalAccountNumber.map(v ⇒ CriteriaField(ExternalAccount.cAccountNum, v.toString, arg.partialMatchFields)),
      currencyName = arg.currency.map(v ⇒ CriteriaField(ExternalAccount.cCurrencyName, v)))
  }

}
