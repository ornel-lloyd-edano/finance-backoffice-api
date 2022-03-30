package tech.pegb.backoffice.domain.aggregations.implementation

import java.time.LocalDate
import java.time.temporal.{ChronoUnit, WeekFields}
import java.util.Locale

import cats.data.EitherT
import cats.implicits._
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.api.aggregations.controllers.Constants
import tech.pegb.backoffice.dao.DbConstants._
import tech.pegb.backoffice.dao.account.sql.{AccountSqlDao, AccountTypesSqlDao}
import tech.pegb.backoffice.dao.aggregations.abstraction.{AggFunctions, GenericAggregationDao}
import tech.pegb.backoffice.dao.aggregations.dto.{AggregationInput, Entity}
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.aggregations.TxnAggServiceResponse
import tech.pegb.backoffice.domain.aggregations.abstraction.{BalanceCalculator, ThirdPartyFeesCalculationExpressionCreator, TransactionAggregationFactory, TransactionAggregationService ⇒ TransactionAggregationServiceTrait}
import tech.pegb.backoffice.domain.aggregations.dto.{TransactionAggregationResult, TransactionGrouping, TxnAggregationsCriteria}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.DomainModelMappingException
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.aggregation.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.aggregation.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransactionAggregationService @Inject() (
    executionContexts: WithExecutionContexts,
    appConfig: AppConfig,
    tpfExpressionCreator: ThirdPartyFeesCalculationExpressionCreator,
    balanceCalculator: BalanceCalculator,
    @Named("MySQLAggregationDao") mysqlAggDao: GenericAggregationDao,
    @Named("GreenPlumAggregationDao") gpAggDao: GenericAggregationDao) extends TransactionAggregationServiceTrait
  with TransactionAggregationFactory with BaseService {

  import TransactionAggregationService._

  implicit val ec: ExecutionContext = executionContexts.blockingIoOperations

  implicit val dateRangeConfig = appConfig.DateTimeRangeLimits.dateTimeRangeConfig.some

  private def getAggFunction(isAggFromGP: Boolean) = {
    if (isAggFromGP) gpAggDao.aggregate(_, _, _, _, _, _, _) else mysqlAggDao.aggregate(_, _, _, _, _, _, _)
  }

  private def shouldAggFromGP(criteria: TxnAggregationsCriteria, numDaysWhenToSwitchDataSource: Option[Int]) = {
    val threshold = numDaysWhenToSwitchDataSource.getOrElse(appConfig.Aggregations.defaultNumDaysWhenToSwitchDataSource)
    val isBeyondThreshold = (from: LocalDate, to: LocalDate) ⇒ ChronoUnit.DAYS.between(from, to) > threshold

    (criteria.startDate, criteria.endDate) match {
      case (Some(dateFrom), Some(dateTo)) ⇒
        isBeyondThreshold(dateFrom.toLocalDate, dateTo.toLocalDate)
      case (Some(dateFrom), _) ⇒
        isBeyondThreshold(dateFrom.toLocalDate, LocalDate.now())

      case _ ⇒
        true
    }
  }

  def getGrossRevenue(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean] = None,
    numDaysWhenToSwitchDataSource: Option[Int] = None): TxnAggServiceResponse = Future {
    val onTheFly = isOnTheFly.getOrElse(appConfig.Aggregations.isOnTheFly)
    val isAggFromGP = shouldAggFromGP(criteria, numDaysWhenToSwitchDataSource)
    val entities = Seq(
      (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
      (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

      (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,

      (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)
    val expressionToAgg = if (onTheFly) {

      val rawExpression =
        s"""
           |CASE
           |  WHEN $txnType = 'currency_exchange' AND $txnDirection = 'debit' THEN
           |    $txnAmt * ($txnCost - $txnEffective)
           |  WHEN $txnType = 'fee' AND $txnDirection = 'debit' THEN
           |    $txnAmt
           |  ELSE
           |    0.0
           |END
       """.stripMargin

      (rawExpression, AggFunctions.Sum, grossRevenueAlias.toOption)
    } else {
      (txnDashboardRevenue, AggFunctions.Sum, grossRevenueAlias.toOption)
    }
    val aggFunction = getAggFunction(isAggFromGP)

    for {
      _ ← criteria.validateDateRange.leftMap(err ⇒ validationError(err.getMessage))
      aggResult ← aggFunction(
        entities,
        Seq(expressionToAgg.asDao),
        criteria.asGenericDao(grouping),
        grouping.map(_.asDao).getOrElse(Nil),
        orderBy.asDao,
        None,
        None).fold(
          _.asDomainError.toLeft,
          _.map(result ⇒ result.asDomain(grossRevenueAlias)
            .fold(
              err ⇒ throw new DomainModelMappingException(result, "Unable to convert AggregationResult to TransactionAggregationResult", err),
              identity))
            .toRight)

    } yield {
      aggResult
    }

  }.recover {
    case err: DomainModelMappingException ⇒
      logger.error(err.getMessage, err)
      Left(dtoMappingError(err.getMessage))
    case err: Exception ⇒
      logger.error("Unexpected error in getGrossRevenue", err)
      Left(unknownError("Unexpected error in getGrossRevenue"))
  }

  def getTurnOver(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean] = None,
    numDaysWhenToSwitchDataSource: Option[Int] = None): TxnAggServiceResponse = Future {

    val isAggFromGP = shouldAggFromGP(criteria, numDaysWhenToSwitchDataSource)

    logger.debug("Get Turnover Criteria :  " + criteria.toString)

    val entities = Seq(
      (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
      (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

      (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,
      (UserSqlDao.TableName, Some(UserSqlDao.TableAlias), Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountUserId}" -> s"${UserSqlDao.TableAlias}.${UserSqlDao.id}")).asDao,

      (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

    val expressionToAgg = (txnAmt, AggFunctions.Sum, turnoverAlias.toOption).asDao
    val aggFunction = getAggFunction(isAggFromGP)

    for {
      _ ← criteria.validateDateRange.leftMap(err ⇒ validationError(err.getMessage))
      aggResult ← aggFunction.apply(
        entities,
        Seq(expressionToAgg),
        criteria.asGenericDao(grouping),
        grouping.map(_.asDao).getOrElse(Nil),
        orderBy.asDao,
        None,
        None).fold(
          _.asDomainError.toLeft,
          _.map(result ⇒
            result.asDomain(turnoverAlias)
              .fold(
                err ⇒ throw new DomainModelMappingException(result, "Unable to convert AggregationResult to TransactionAggregationResult", err),
                _.fixDoubleTotalDueToDebitCredit)).toRight)

    } yield {
      aggResult
    }

  }.recover {
    case err: DomainModelMappingException ⇒
      logger.error(err.getMessage, err)
      Left(dtoMappingError(err.getMessage))
    case err: Exception ⇒
      logger.error("Unexpected error in getTurnOver", err)
      Left(unknownError("Unexpected error in getTurnOver"))
  }

  def getProviderTurnover(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean] = None,
    numDaysWhenToSwitchDataSource: Option[Int] = None): TxnAggServiceResponse = Future {

    val isAggFromGP = shouldAggFromGP(criteria, numDaysWhenToSwitchDataSource)

    logger.debug("Get Turnover Criteria :  " + criteria.toString)

    val entities = Seq(
      (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
      (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

      (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,
      (UserSqlDao.TableName, Some(UserSqlDao.TableAlias), Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountUserId}" -> s"${UserSqlDao.TableAlias}.${UserSqlDao.id}")).asDao)

    val expressionToAgg = (txnAmt, AggFunctions.Sum, providerTurnoverAlias.toOption).asDao
    val aggFunction = getAggFunction(isAggFromGP)
    for {
      _ ← criteria.validateDateRange.leftMap(err ⇒ validationError(err.getMessage))
      aggResult ← aggFunction.apply(
        entities,
        Seq(expressionToAgg),
        criteria.asGenericDao(grouping),
        grouping.map(_.asDao).getOrElse(Nil),
        orderBy.asDao,
        None,
        None).fold(
          _.asDomainError.toLeft,
          _.map(result ⇒
            result.asDomain(providerTurnoverAlias)
              .fold(
                err ⇒ throw new DomainModelMappingException(result, "Unable to convert AggregationResult to TransactionAggregationResult", err),
                identity)).toRight)

    } yield {
      aggResult
    }

  }.recover {
    case err: DomainModelMappingException ⇒
      logger.error(err.getMessage, err)
      Left(dtoMappingError(err.getMessage))
    case err: Exception ⇒
      logger.error("Unexpected error in getProviderTurnOver", err)
      Left(unknownError("Unexpected error in getProviderTurnOver"))
  }

  def getThirdPartyFees(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean] = None,
    numDaysWhenToSwitchDataSource: Option[Int] = None): TxnAggServiceResponse = {

    val entity = (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao
    val entities = Seq(
      entity,
      (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

      (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,

      (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

    val aggAlias = TransactionAggregationService.thirdPartyFees
    (for {
      expression ← {
        EitherT(tpfExpressionCreator.getCompleteThirdPartyFeesCalculationNestedExpression(
          entity, TransactionSqlDao.cAmount, None))
      }

      _ ← EitherT.fromEither[Future](criteria.validateDateRange.leftMap(err ⇒ validationError(err.getMessage)))

      result ← EitherT.fromEither[Future]({
        val isAggFromGP = shouldAggFromGP(criteria, numDaysWhenToSwitchDataSource)
        val daoCriteria = criteria.asGenericDao(grouping)
        val expressionToAgg = (expression, AggFunctions.Sum, aggAlias.toOption).asDao
        val aggFunction = getAggFunction(isAggFromGP)

        aggFunction.apply(
          entities,
          Seq(expressionToAgg),
          daoCriteria,
          grouping.map(_.asDao).getOrElse(Nil),
          orderBy.asDao,
          None,
          None).fold(
            _.asDomainError.toLeft,
            _.map(result ⇒
              result.asDomain(aggAlias)
                .fold(
                  err ⇒ throw new DomainModelMappingException(result, "Unable to convert AggregationResult to TransactionAggregationResult", err),
                  _.fixDoubleTotalDueToDebitCredit)).toRight)
      })
    } yield {
      result
    }).value
  }.recover {
    case err: DomainModelMappingException ⇒
      logger.error(err.getMessage, err)
      Left(dtoMappingError(err.getMessage))
    case err: Exception ⇒
      logger.error("Unexpected error in getThirdPartyFees", err)
      Left(unknownError("Unexpected error in getThirdPartyFees"))
  }

  def getTotalAccountBalances(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean] = None,
    numDaysWhenToSwitchDataSource: Option[Int] = None): TxnAggServiceResponse = Future {

    val isAggFromGP = shouldAggFromGP(criteria, numDaysWhenToSwitchDataSource)

    val daoCriteria = criteria.asGenericDaoWithoutTransaction

    val (entity, expressionToAgg) = createEntityAndExpressionFromAccounts

    val aggFunction = getAggFunction(isAggFromGP)

    aggFunction.apply(
      entity,
      expressionToAgg,
      daoCriteria,
      grouping.map(_.asDao).getOrElse(Nil),
      orderBy.asDao,
      None,
      None).fold(
        _.asDomainError.toLeft,
        _.map(result ⇒
          result.asDomain(balanceAlias)
            .fold(
              err ⇒ throw new DomainModelMappingException(result, "Unable to convert AggregationResult to TransactionAggregationResult", err), identity)).toRight)
  }.recover {
    case err: DomainModelMappingException ⇒
      logger.error(err.getMessage, err)
      Left(dtoMappingError(err.getMessage))
    case err: Exception ⇒
      logger.error("Unexpected error in getTotalAccountBalances", err)
      Left(unknownError("Unexpected error in getTotalAccountBalances"))
  }

  def getTotalBalance(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean] = None,
    numDaysWhenToSwitchDataSource: Option[Int] = None): TxnAggServiceResponse = Future {

    val isAggFromGP = shouldAggFromGP(criteria, numDaysWhenToSwitchDataSource)

    val daoCriteria = grouping match {
      case Some(groupBy) if groupBy.daily || groupBy.weekly || groupBy.monthly ⇒ criteria.copy(startDate = None).asGenericDao(grouping)
      case _ ⇒ criteria.asGenericDao(grouping)
    }

    val (entity, expressionToAgg) = createEntityAndExpressionFromTxns

    val aggFunction = getAggFunction(isAggFromGP)

    for {
      _ ← criteria.validateDateRange.leftMap(err ⇒ validationError(err.getMessage))
      aggResult ← aggFunction.apply(
        entity,
        expressionToAgg,
        daoCriteria,
        grouping.map(_.asDao).getOrElse(Nil),
        orderBy.asDao,
        None,
        None).fold(
          _.asDomainError.toLeft,
          _.map(result ⇒
            result.asDomain(balanceAlias)
              .fold(
                err ⇒ throw new DomainModelMappingException(result, "Unable to convert AggregationResult to TransactionAggregationResult", err), identity)).toRight)

    } yield {
      balanceCalculator.computeBalancePerTimePeriod(aggResult, grouping, criteria.startDate, criteria.endDate)
    }

  }.recover {
    case err: DomainModelMappingException ⇒
      logger.error(err.getMessage, err)
      Left(dtoMappingError(err.getMessage))
    case err: Exception ⇒
      logger.error("Unexpected error in getTotalBalance", err)
      Left(unknownError("Unexpected error in getTotalBalance"))
  }

  def getTotalAmount(
    criteria: TxnAggregationsCriteria,
    grouping: Option[TransactionGrouping],
    orderBy: Seq[Ordering],
    isOnTheFly: Option[Boolean] = None,
    numDaysWhenToSwitchDataSource: Option[Int] = None): TxnAggServiceResponse = Future {

    val isAggFromGP = shouldAggFromGP(criteria, numDaysWhenToSwitchDataSource)

    val (entity, expressionToAgg) = createEntityAndExpressionFromTxns

    val aggFunction = getAggFunction(isAggFromGP)

    for {
      _ ← criteria.validateDateRange.leftMap(err ⇒ validationError(err.getMessage))
      aggResult ← aggFunction.apply(
        entity,
        expressionToAgg,
        criteria.asGenericDao(grouping),
        grouping.map(_.asDao).getOrElse(Nil),
        orderBy.asDao,
        None,
        None).fold(
          _.asDomainError.toLeft,
          _.map(result ⇒
            result.asDomain(balanceAlias)
              .fold(
                err ⇒ throw new DomainModelMappingException(result, "Unable to convert AggregationResult to TransactionAggregationResult", err), identity)).toRight)

    } yield {
      aggResult
    }
  }.recover {
    case err: DomainModelMappingException ⇒
      logger.error(err.getMessage, err)
      Left(dtoMappingError(err.getMessage))
    case err: Exception ⇒
      logger.error("Unexpected error in getTotalAmount", err)
      Left(unknownError("Unexpected error in getTotalAmount"))
  }

  // Cashflow report is similar to turnover, but it is only for providers

  private def createEntityAndExpressionFromAccounts: (Seq[Entity], Seq[AggregationInput]) = {
    val entity = Seq(
      (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias), Nil).asDao,
      (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias), Seq((accountCurrencyId, currencyId))).asDao,
      (AccountTypesSqlDao.TableName, Some(AccountTypesSqlDao.TableAlias), Seq((accountsTypeIdFKeyRef, accountsTypeId))).asDao)
    val expressionToAgg = Seq((accountBalance, AggFunctions.Sum, balanceAlias.toOption).asDao)

    (entity, expressionToAgg)
  }

  private def createEntityAndExpressionFromTxns: (Seq[Entity], Seq[AggregationInput]) = {

    val entity = Seq(
      (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
      (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias), Seq((txnCurrencyId, currencyId))).asDao,
      (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias), Seq((txnPrimaryAccountId, accountId))).asDao,
      (AccountTypesSqlDao.TableName, Some(AccountTypesSqlDao.TableAlias), Seq((accountsTypeIdFKeyRef, accountsTypeId))).asDao,
      (UserSqlDao.TableName, Some(UserSqlDao.TableAlias), Seq((txnPrimaryAccountUserId, userId))).asDao,
      (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
        Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

    val rawExpression =
      s"""
         |CASE
         |     WHEN $txnDirection = 'debit'  AND $accountMainType = 'asset' THEN $txnAmt
         |     WHEN $txnDirection = 'credit' AND $accountMainType = 'asset' THEN -$txnAmt
         |     WHEN $txnDirection = 'credit' AND $accountMainType = 'liability' THEN $txnAmt
         |     WHEN $txnDirection = 'debit'  AND $accountMainType = 'liability' THEN -$txnAmt
         |  ELSE 0
         |END
       """.stripMargin
    val expressionToAgg = Seq((rawExpression, AggFunctions.Sum, balanceAlias.toOption).asDao)

    (entity, expressionToAgg)

  }

  private val map = Map[String, (TxnAggregationsCriteria, Option[TransactionGrouping], Seq[Ordering], Option[Boolean], Option[Int]) ⇒ TxnAggServiceResponse](
    Constants.Aggregations.GrossRevenue → getGrossRevenue,
    Constants.Aggregations.TurnOver → getTurnOver,
    Constants.Aggregations.ThirdPartyFees → getThirdPartyFees,
    Constants.Aggregations.AccountBalance → getTotalAccountBalances,
    Constants.Aggregations.Balance → getTotalBalance,
    Constants.Aggregations.Amount → getTotalAmount,
    Constants.Aggregations.ProviderTurnover -> getProviderTurnover)

  def getAggregationFunction(name: String): Option[(TxnAggregationsCriteria, Option[TransactionGrouping], Seq[Ordering], Option[Boolean], Option[Int]) ⇒ TxnAggServiceResponse] = {
    map.get(name)
  }

}

object TransactionAggregationService {

  val turnoverAlias = Constants.Aggregations.TurnOver
  val balanceAlias = "balance"
  val grossRevenueAlias = "total_gross_revenue"
  val thirdPartyFees = "third_party_fees"
  val providerTurnoverAlias = Constants.Aggregations.ProviderTurnover
  // add cashflow aggregations
  //  val bankTransferAlias = "bank_transfer"
  //  val

  implicit class RichAggregationResult(val that: TransactionAggregationResult) extends AnyVal {
    def fixDoubleTotalDueToDebitCredit: TransactionAggregationResult = {
      val divisor = BigDecimal(2)
      that.copy(sumAmount = that.sumAmount.map(aggValue ⇒ (aggValue / divisor).setScale(2, BigDecimal.RoundingMode.HALF_EVEN)))
    }
  }

  val weekFields = WeekFields.of(Locale.getDefault)

}
