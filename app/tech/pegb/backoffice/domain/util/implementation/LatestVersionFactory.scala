package tech.pegb.backoffice.domain.util.implementation

import java.time.ZoneOffset

import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.application.sql.WalletApplicationSqlDao
import tech.pegb.backoffice.dao.auth.sql.{BackOfficeUserSqlDao, PermissionSqlDao, RoleSqlDao, ScopeSqlDao}
import tech.pegb.backoffice.dao.commission.sql.CommissionProfileSqlDao
import tech.pegb.backoffice.dao.currencyexchange.sql.{CurrencyExchangeSqlDao, SpreadsSqlDao}
import tech.pegb.backoffice.dao.customer.sql.{IndividualUserSqlDao, UserSqlDao}
import tech.pegb.backoffice.dao.document.sql.DocumentSqlDao
import tech.pegb.backoffice.dao.fee.sql.FeeProfileSqlDao
import tech.pegb.backoffice.dao.i18n.sql.I18nStringSqlDao
import tech.pegb.backoffice.dao.limit.sql.LimitProfileSqlDao
import tech.pegb.backoffice.dao.makerchecker.dto
import tech.pegb.backoffice.dao.makerchecker.sql.TasksSqlDao
import tech.pegb.backoffice.dao.notification.sql.{NotificationSqlDao, NotificationTemplateSqlDao}
import tech.pegb.backoffice.dao.report.sql.ReportDefinitionSqlDao
import tech.pegb.backoffice.dao.transaction.sql.{SettlementSqlDao, TransactionSqlDao}
import tech.pegb.backoffice.domain.account.dto.AccountCriteria
import tech.pegb.backoffice.domain.application.dto.WalletApplicationCriteria
import tech.pegb.backoffice.domain.auth.dto.{BackOfficeUserCriteria, PermissionCriteria, RoleCriteria, ScopeCriteria}
import tech.pegb.backoffice.domain.commission.dto.CommissionProfileCriteria
import tech.pegb.backoffice.domain.currencyexchange.dto.{CurrencyExchangeCriteria, SpreadCriteria}
import tech.pegb.backoffice.domain.customer.dto.{GenericUserCriteria, IndividualUserCriteria}
import tech.pegb.backoffice.domain.document.dto.DocumentCriteria
import tech.pegb.backoffice.domain.fee.dto.FeeProfileCriteria
import tech.pegb.backoffice.domain.i18n.dto.I18nStringCriteria
import tech.pegb.backoffice.domain.limit.dto.LimitProfileCriteria
import tech.pegb.backoffice.domain.notification.dto.{NotificationCriteria, NotificationTemplateCriteria}
import tech.pegb.backoffice.domain.report.dto.ReportDefinitionCriteria
import tech.pegb.backoffice.domain.transaction.dto.{ManualTxnCriteria, TransactionCriteria}
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.domain.{BaseService, ServiceError}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.application.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.permission.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.role.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.scope.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.commission.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.currencyexchange.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.document.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.fee.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.i18n.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.limit.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.notification.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.report.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, Logging, WithExecutionContexts}

import scala.concurrent.Future

@Singleton
class LatestVersionFactory @Inject() (
    executionContexts: WithExecutionContexts,
    config: AppConfig,
    txnDao: TransactionSqlDao,
    accountDao: AccountSqlDao,
    documentDao: DocumentSqlDao,
    walletApplicationDao: WalletApplicationSqlDao,
    currencyExchangeDao: CurrencyExchangeSqlDao,
    spreadDao: SpreadsSqlDao,
    feeProfileDao: FeeProfileSqlDao,
    limitProfileDao: LimitProfileSqlDao,
    individualUsrDao: IndividualUserSqlDao,
    notificationTemplateDao: NotificationTemplateSqlDao,
    notificationDao: NotificationSqlDao,
    tasksDao: TasksSqlDao,
    i18nStringDao: I18nStringSqlDao,
    roleDao: RoleSqlDao,
    userDao: UserSqlDao,
    scopeDao: ScopeSqlDao,
    reportDefinitionDao: ReportDefinitionSqlDao,
    permissionDao: PermissionSqlDao,
    settlementsDao: SettlementSqlDao,
    backOfficeUserDao: BackOfficeUserSqlDao,
    commissionProfileDao: CommissionProfileSqlDao) extends BaseService with LatestVersionService with Logging {

  implicit val ec = executionContexts.blockingIoOperations

  def getLatestVersion(anyCriteria: AnyRef): Future[Either[ServiceError, Option[String]]] = {

    anyCriteria match {
      case criteria: BackOfficeUserCriteria ⇒
        Future(backOfficeUserDao.getMostRecentUpdatedAt(criteria.asDao().toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: ManualTxnCriteria ⇒
        //settlements never get updated
        Future(settlementsDao.getSettlementsByCriteria(criteria.asDao, None, None, None)
          .map(_.sortBy(_.createdAt.toEpochSecond(ZoneOffset.UTC)).reverse.headOption.map(_.createdAt.toString))
          .leftMap(_.asDomainError))

      case criteria: DocumentCriteria ⇒
        Future(documentDao.getMostRecentUpdatedAt(criteria.asDao().toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: AccountCriteria ⇒
        Future(accountDao.getMostRecentUpdatedAt(criteria.asDao.toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: TransactionCriteria ⇒
        Future(txnDao.getMostRecentUpdatedAt(criteria.asDao().toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: WalletApplicationCriteria ⇒
        Future(walletApplicationDao.getMostRecentUpdatedAt(criteria.asDao(config.InactiveStatuses).toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: IndividualUserCriteria ⇒
        Future(individualUsrDao.getMostRecentUpdatedAt(criteria.asDao.toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: CurrencyExchangeCriteria ⇒
        Future(currencyExchangeDao.getMostRecentUpdatedAt(criteria.asDao.toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: SpreadCriteria ⇒
        Future(spreadDao.getMostRecentUpdatedAt(criteria.asDao.toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: FeeProfileCriteria ⇒
        Future(feeProfileDao.getMostRecentUpdatedAt(criteria.asDao.toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: LimitProfileCriteria ⇒
        Future(limitProfileDao.getMostRecentUpdatedAt(criteria.asDao.toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: NotificationTemplateCriteria ⇒
        Future(notificationTemplateDao.getMostRecentUpdatedAt(criteria.asDao.toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: NotificationCriteria ⇒
        Future(notificationDao.getMostRecentUpdatedAt(criteria.asDao().toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: dto.MakerCheckerCriteria ⇒
        Future(tasksDao.getMostRecentUpdatedAt(criteria.some)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: I18nStringCriteria ⇒
        Future(i18nStringDao.getMostRecentUpdatedAt(criteria.asDao.toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: ReportDefinitionCriteria ⇒
        Future(reportDefinitionDao.getMostRecentUpdatedAt(criteria.asDao.some)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: RoleCriteria ⇒
        Future(roleDao.getMostRecentUpdatedAt(criteria.asDao().toOption)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .left.map(_.asDomainError))

      case criteria: GenericUserCriteria ⇒
        Future(userDao.getMostRecentUpdatedAt(criteria.asDao.some)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: ScopeCriteria ⇒
        Future(scopeDao.getMostRecentUpdatedAt(criteria.asDao().some)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: PermissionCriteria ⇒
        Future(permissionDao.getMostRecentUpdatedAt(criteria.asDao().some)
          .map(_.flatMap(_.updatedAt.map(_.toString)))
          .leftMap(_.asDomainError))

      case criteria: CommissionProfileCriteria ⇒
        Future(commissionProfileDao.getMostRecentUpdatedAt(criteria.asDao.toOption)
          .map(_.map(_.updatedAt.toString))
          .leftMap(_.asDomainError))

      case _ ⇒
        logger.warn(s"Unexpected criteria to use for getLatestVersion")
        Right(None).toFuture
    }
  }

}
