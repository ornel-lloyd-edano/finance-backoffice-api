package tech.pegb.backoffice.mapping.domain.dao.report.cashflow

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.domain.report.dto.{CashFlowReportCriteria, CashFlowTotalsCriteria}
import tech.pegb.backoffice.dao.report.dto.{CashFlowReportCriteria ⇒ DaoCashFlowReportCriteria, CashFlowTotalsCriteria ⇒ DaoCashFlowTotalsCriteria}
import tech.pegb.backoffice.dao.report.sql.CashFlowReportMetadata
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.util.Implicits._

object Implicits extends CashFlowReportMetadata {
  implicit class CashFlowReportCriteriaAdapter(val arg: CashFlowReportCriteria) extends AnyVal {
    def asDao =
      DaoCashFlowReportCriteria(
        currencies = if (arg.onlyForTheseCurrencies.nonEmpty)
          CriteriaField[Seq[String]](CurrencySqlDao.cName, arg.onlyForTheseCurrencies, MatchTypes.In).toOption
        else None,
        providers = if (arg.onlyForTheseProviders.nonEmpty)
          CriteriaField[Seq[String]](Provider.cName, arg.onlyForTheseProviders, MatchTypes.In).toOption
        else None,
        createdAtFrom = arg.startDate.map(d ⇒ CriteriaField[LocalDateTime](TransactionSqlDao.cCreatedAt, d.atStartOfDay(), MatchTypes.GreaterOrEqual)),
        createdAtTo = arg.endDate.map(d ⇒ CriteriaField[LocalDateTime](TransactionSqlDao.cCreatedAt, d.atEndOfDay, MatchTypes.LesserOrEqual)),
        userType = if (arg.userType.nonEmpty) CriteriaField[String](UserSqlDao.typeName, arg.userType.get, MatchTypes.Exact).toOption else None,
        provider = arg.txnOtherParty.map(value ⇒ CriteriaField[String](Provider.cName, value, MatchTypes.IsNotNull)),
        primaryAccNumber = if (arg.notThisPrimaryAccNumber.nonEmpty) CriteriaField[String](TransactionSqlDao.cPrimaryAccountNumber, arg.notThisPrimaryAccNumber.get, MatchTypes.NotPartial).toOption else None)
  }

  implicit class CashFlowTotalsCriteriaAdapter(val arg: CashFlowTotalsCriteria) extends AnyVal {
    def asDao =
      DaoCashFlowTotalsCriteria(
        currency = CriteriaField[String](CurrencySqlDao.cName, arg.currency).toOption,
        providers = if (arg.onlyForTheseProviders.nonEmpty)
          CriteriaField[Seq[String]](txProvider, arg.onlyForTheseProviders, MatchTypes.In).toOption
        else None,
        createdAtFrom = arg.dateFrom.map(d ⇒ CriteriaField[LocalDateTime](TransactionSqlDao.cCreatedAt, d.atStartOfDay(), MatchTypes.GreaterOrEqual)),
        createdAtTo = arg.dateTo.map(d ⇒ CriteriaField[LocalDateTime](TransactionSqlDao.cCreatedAt, d.atEndOfDay, MatchTypes.LesserOrEqual)))
  }
}
