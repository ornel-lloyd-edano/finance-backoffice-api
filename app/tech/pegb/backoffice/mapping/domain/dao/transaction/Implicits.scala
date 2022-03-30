package tech.pegb.backoffice.mapping.domain.dao.transaction

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.MatchTypes.{In, Partial}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.transaction.dto
import tech.pegb.backoffice.dao.transaction.dto.{SettlementCriteria ⇒ DaoSettlementCriteria, SettlementLinesToInsert ⇒ DaoSettlementLinesToInsert, SettlementToInsert ⇒ DaoSettlementToInsert, TransactionCriteria ⇒ DaoTransactionCriteria}
import tech.pegb.backoffice.dao.transaction.entity.TxnConfig
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.domain.transaction.dto._
import tech.pegb.backoffice.domain.transaction.model.ManualTransactionLines
import tech.pegb.backoffice.util.Implicits._

object Implicits {
  implicit class TransactionCriteriaConverterNew(val arg: TransactionCriteria) extends AnyVal {
    def asDao(isOtherPartyNotNull: Option[Boolean] = None) = DaoTransactionCriteria(
      id = arg.id.map(i ⇒ CriteriaField(TransactionSqlDao.cId, i)),
      primaryAccountUsersUsername = arg.anyCustomerName.map { name ⇒
        CriteriaField(TransactionSqlDao.cPrimaryUserUsername, name, Partial)
      },
      primaryAccountIndividualUsersName = arg.anyCustomerName.map { name ⇒
        CriteriaField(TransactionSqlDao.cPrimaryIndividualUserName, name, Partial)
      },
      primaryAccountIndividualUsersFullname = arg.anyCustomerName.map { name ⇒
        CriteriaField(TransactionSqlDao.cPrimaryIndividualUserFullname, name, Partial)
      },
      primaryAccountBusinessUsersBusinessName = arg.anyCustomerName.map { name ⇒
        CriteriaField(TransactionSqlDao.cBusinessName, name, Partial)
      },
      primaryAccountBusinessUsersBrandName = arg.anyCustomerName.map { name ⇒
        CriteriaField(TransactionSqlDao.cBrandName, name, Partial)
      },
      customerId = arg.customerId.map(c ⇒ CriteriaField(TransactionSqlDao.cPrimaryUserUuid, c.toString, Partial)),
      accountId = arg.accountId.map(a ⇒ CriteriaField(TransactionSqlDao.cPrimaryAccountId, a.toString, Partial)),
      accountNumbers = {
        if (arg.accountNumbers.isEmpty) None
        else Some(CriteriaField(TransactionSqlDao.cPrimaryAccountNumber, arg.accountNumbers, In))
      },
      createdAt = (arg.startDate, arg.endDate) match {
        case (Some(createdAtFrom), Some(createdAtTo)) ⇒
          CriteriaField[(LocalDateTime, LocalDateTime)](TransactionSqlDao.cCreatedAt, (createdAtFrom, createdAtTo), MatchTypes.InclusiveBetween).toOption
        case (Some(createdAtFrom), None) ⇒
          CriteriaField[LocalDateTime](TransactionSqlDao.cCreatedAt, createdAtFrom, MatchTypes.GreaterOrEqual).toOption
        case (None, Some(createdAtTo)) ⇒
          CriteriaField[LocalDateTime](TransactionSqlDao.cCreatedAt, createdAtTo, MatchTypes.LesserOrEqual).toOption
        case _ ⇒ None
      },
      transactionType = arg.transactionType.map(t ⇒ CriteriaField(TransactionSqlDao.cType, t.toString)),
      channel = arg.channel.map(c ⇒ CriteriaField(TransactionSqlDao.cChannel, c.toString)),
      status = arg.status.map(s ⇒ CriteriaField(TransactionSqlDao.cChannel, s.underlying)),
      currencyCode = arg.currencyCode.map(c ⇒ CriteriaField(TransactionSqlDao.caCurrency, c.toString)),
      accountType = arg.accountType.map(c ⇒ CriteriaField(TransactionSqlDao.cPrimaryAccountType, c)),
      direction = arg.direction.map(c ⇒ CriteriaField(TransactionSqlDao.cDirection, c)),
      provider = isOtherPartyNotNull match {
        case Some(true) ⇒ CriteriaField(s"${ProviderSqlDao.TableAlias}.${Provider.cName}", "", MatchTypes.IsNotNull).toOption
        case Some(false) ⇒ CriteriaField(s"${ProviderSqlDao.TableAlias}.${Provider.cName}", "", MatchTypes.IsNull).toOption
        case _ ⇒ None
      })
  }

  implicit class ManualTxnCriteriaDomainAdapter(val arg: ManualTxnCriteria) extends AnyVal {
    def asDao = DaoSettlementCriteria(
      uuid = arg.id.map(_.toString),
      createdAtFrom = arg.startCreatedAt.map(_.atStartOfDay()),
      createdAtTo = arg.endCreatedAt.map(_.atEndOfDay),
      accountNumber = arg.accountNumber,
      direction = arg.direction.map(_.toString()),
      currency = arg.currency.map(_.getCurrencyCode))
  }

  implicit class ManualTxnLinesToCreateAdapter(val arg: ManualTransactionLines) extends AnyVal {
    def asDao(accountId: Int, currencyId: Int) = DaoSettlementLinesToInsert(
      accountId = accountId,
      direction = arg.direction.toString(),
      currencyId = currencyId,
      amount = arg.amount,
      explanation = arg.explanation)
  }

  implicit class ManualTxnToCreateAdapter(val arg: ManualTxnToCreate) extends AnyVal {
    def asDao(currencyLookUp: Map[String, Int], settlementLines: Seq[DaoSettlementLinesToInsert]) = DaoSettlementToInsert(
      uuid = arg.uuid.toString,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      checkedBy = None,
      checkedAt = None,
      status = "approved",
      reason = arg.reason,
      fxProvider = arg.manualTxnFxDetails.map(_.fxProvider),
      fromCurrencyId = arg.manualTxnFxDetails.flatMap(x ⇒ currencyLookUp.get(x.fromCurrency)),
      toCurrencyId = arg.manualTxnFxDetails.flatMap(x ⇒ currencyLookUp.get(x.toCurrency)),
      fxRate = arg.manualTxnFxDetails.map(_.fxRate),
      settlementLines = settlementLines)
  }

  implicit class SettlementFxHistoryCriteriaAdapter(val arg: SettlementFxHistoryCriteria) extends AnyVal {
    def asDao = dto.SettlementFxHistoryCriteria(
      fxProvider = arg.fxProvider,
      fromCurrency = arg.fromCurrency,
      toCurrency = arg.toCurrency,
      createdAtFrom = arg.createdAtFrom,
      createdAtTo = arg.createdAtTo)
  }

  implicit class SettlementRecentAccountCriteriaAdapter(val arg: SettlementRecentAccountCriteria) extends AnyVal {
    def asDao = dto.SettlementRecentAccountCriteria(
      currency = arg.currency)
  }

  implicit class TxnConfigToCreateDaoAdaptet(val arg: TxnConfigToCreate) extends AnyVal {
    def asDao(customerId: Int, currencyId: Int) = dto.TxnConfigToCreate(
      uuid = arg.id,
      userId = customerId,
      transactionType = arg.transactionType,
      currencyId = currencyId,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      updatedBy = Some(arg.createdBy),
      updatedAt = Some(arg.createdAt))
  }

  implicit class TxnConfigToUpdateDaoAdapter(val arg: TxnConfigToUpdate) extends AnyVal {
    def asDao(currencyId: Option[Int]) = dto.TxnConfigToUpdate(
      transactionType = arg.transactionType,
      currencyId = arg.currency.flatMap(_ ⇒ currencyId),
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt,
      lastUpdatedAt = arg.lastUpdatedAt)
  }

  implicit class TxnConfigCriteriaDaoAdapter(val arg: TxnConfigCriteria) extends AnyVal {
    def asDao(anyUuids: Option[Set[UUID]] = None) = dto.TxnConfigCriteria(
      uuid = arg.id.map(v ⇒ CriteriaField(TxnConfig.cUuid, v.toString)),
      anyUuid = anyUuids.map(v ⇒ CriteriaField(UserSqlDao.uuid, v.map(_.toString), MatchTypes.In)),
      userUuid = arg.customerId.map(v ⇒ CriteriaField(UserSqlDao.uuid, v.toString)),
      transactionType = arg.transactionType.map(v ⇒ CriteriaField(TxnConfig.cTxnType, v)),
      currencyName = arg.currency.map(v ⇒ CriteriaField(CurrencySqlDao.cName, v)))
  }
}
