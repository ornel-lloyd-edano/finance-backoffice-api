package tech.pegb.backoffice.domain.aggregations

import java.time.{LocalDate, LocalDateTime}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.test.Injecting
import tech.pegb.backoffice.dao.DbConstants._
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.aggregations.abstraction.AggFunctions.Sum
import tech.pegb.backoffice.dao.aggregations.abstraction.{AggFunctions, GenericAggregationDao}
import tech.pegb.backoffice.dao.aggregations.dto.{AggregatedValue, AggregationInput, AggregationResult, Entity, _}
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, OrderingSet}
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.domain.aggregations.abstraction.ThirdPartyFeesCalculationExpressionCreator
import tech.pegb.backoffice.domain.aggregations.dto.{TransactionAggregationResult, TransactionGrouping, TxnAggregationsCriteria}
import tech.pegb.backoffice.domain.aggregations.implementation.TransactionAggregationService
import tech.pegb.backoffice.mapping.domain.dao.aggregation.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class TransactionAggregationServiceSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  lazy val mysqlAggDao = stub[GenericAggregationDao]
  lazy val gpAggDao = stub[GenericAggregationDao]
  lazy val thirdPartyFeeExpressionCreator = stub[ThirdPartyFeesCalculationExpressionCreator]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[GenericAggregationDao].qualifiedWith("MySQLAggregationDao").to(mysqlAggDao),
      bind[GenericAggregationDao].qualifiedWith("GreenPlumAggregationDao").to(gpAggDao),
      bind[ThirdPartyFeesCalculationExpressionCreator].to(thirdPartyFeeExpressionCreator),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val txnAggService = inject[TransactionAggregationService]

  private val txnType = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cType}"
  private val txnDirection = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cDirection}"
  private val txnAmt = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cAmount}"
  private val txnCost = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCostRate}"
  private val txnEffective = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cEffectiveRate}"
  private val accountMainType = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cMainType}"
  private val accountBalance = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cBalance}"

  "TransactionAggregationService" should {

    "getGrossRevenue from dashboard_revenue in greenplum" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.now.toOption,
        endDate = LocalDateTime.now.plusDays(30).toOption)

      val result = txnAggService.getGrossRevenue(criteria, None, Nil, isOnTheFly = Some(false), Some(10))

      val expectedEntity = Seq(
        (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
        (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

        (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,

        (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

      val expectedAggregations = Seq(AggregationInput(
        columnOrExpression = "tx.dashboard_revenue",
        function = AggFunctions.Sum,
        alias = "total_gross_revenue".toOption))
      val expectedCriteria = Seq(
        CriteriaField(currencyName, criteria.currencyCode.get),
        CriteriaField(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}", "", MatchTypes.IsNotNull),
        CriteriaField(txnCreatedAt, (criteria.startDate.get, criteria.endDate.get), MatchTypes.InclusiveBetween))

      val mockResult = AggregationResult(
        aggregations = Seq(AggregatedValue("total_gross_revenue", "999999.00")),
        grouping = Nil)

      (gpAggDao.aggregate _)
        .when(expectedEntity, expectedAggregations, expectedCriteria, Nil, None, None, None)
        .returns(Right(Seq(mockResult)))

      val expectedResult = Seq(TransactionAggregationResult(sumAmount = BigDecimal("999999.00").toOption))

      whenReady(result) { result ⇒
        result mustBe Right(expectedResult)
      }
    }

    "getGrossRevenue compute on the fly in mysql (amount of days between dateFrom and dateTo is less than config)" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.now.toOption,
        endDate = LocalDateTime.now.plusDays(5).toOption)

      val expectedEntity = Seq(
        (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
        (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

        (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,

        (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

      val expectedAggregations = Seq(AggregationInput(
        columnOrExpression = s"""
                                |CASE
                                |  WHEN tx.type = 'currency_exchange' AND tx.direction = 'debit' THEN
                                |    tx.amount * (tx.cost_rate - tx.effective_rate)
                                |  WHEN tx.type = 'fee' AND tx.direction = 'debit' THEN
                                |    tx.amount
                                |  ELSE
                                |    0.0
                                |END
       """.stripMargin,
        function = AggFunctions.Sum,
        alias = "total_gross_revenue".toOption))
      val expectedCriteria = Seq(
        CriteriaField(currencyName, criteria.currencyCode.get),
        CriteriaField(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}", "", MatchTypes.IsNotNull),
        CriteriaField(txnCreatedAt, (criteria.startDate.get, criteria.endDate.get), MatchTypes.InclusiveBetween))

      val mockResult = AggregationResult(
        aggregations = Seq(AggregatedValue("total_gross_revenue", "1000.00")),
        grouping = Nil)

      (mysqlAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int]))
        .when(expectedEntity, expectedAggregations, expectedCriteria, Nil, None, None, None)
        .returns(Right(Seq(mockResult)))

      val expectedResult = Seq(TransactionAggregationResult(sumAmount = BigDecimal("1000.00").toOption))

      val result = txnAggService.getGrossRevenue(criteria, None, Nil, isOnTheFly = Some(true), Some(10))

      whenReady(result) { result ⇒
        result mustBe Right(expectedResult)
      }
    }

    "getGrossRevenue compute on the fly in greenplum (amount of days between dateFrom and dateTo is more than config)" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.now.toOption,
        endDate = LocalDateTime.now.plusDays(30).toOption)

      val result = txnAggService.getGrossRevenue(criteria, None, Nil, isOnTheFly = Some(true), Some(10))

      val expectedEntity = Seq(
        (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
        (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

        (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,

        (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

      val expectedAggregations = Seq(AggregationInput(
        columnOrExpression = s"""
                                |CASE
                                |  WHEN tx.type = 'currency_exchange' AND tx.direction = 'debit' THEN
                                |    tx.amount * (tx.cost_rate - tx.effective_rate)
                                |  WHEN tx.type = 'fee' AND tx.direction = 'debit' THEN
                                |    tx.amount
                                |  ELSE
                                |    0.0
                                |END
       """.stripMargin,
        function = AggFunctions.Sum,
        alias = "total_gross_revenue".toOption))
      val expectedCriteria = Seq(
        CriteriaField(currencyName, criteria.currencyCode.get),
        CriteriaField(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}", "", MatchTypes.IsNotNull),
        CriteriaField(txnCreatedAt, (criteria.startDate.get, criteria.endDate.get), MatchTypes.InclusiveBetween))

      val mockResult = AggregationResult(
        aggregations = Seq(AggregatedValue("total_gross_revenue", "999999.00")),
        grouping = Nil)

      (gpAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int]))
        .when(expectedEntity, expectedAggregations, expectedCriteria, Nil, None, None, None)
        .returns(Right(Seq(mockResult)))

      val expectedResult = Seq(TransactionAggregationResult(sumAmount = BigDecimal("999999.00").toOption))

      whenReady(result) { result ⇒
        result mustBe Right(expectedResult)
      }
    }

    "get turnover from mysql when onFly is enabled" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.of(2019, 12, 1, 0, 0, 0).toOption,
        endDate = LocalDateTime.of(2019, 12, 6, 0, 0, 0).toOption)

      val grouping = Grouping(column = "c.currency_name", value = "KES")
      val aggregatedValue = AggregatedValue(columnOrAlias = "turnover", value = "5000000.00")
      val daoAggregationResult = AggregationResult(aggregations = Seq(aggregatedValue), grouping = Seq(grouping))

      val expectedResult = TransactionAggregationResult(Some(2500000.00), None, None, None, Some("KES"), None, None, None, None, None)

      //dao inputs:

      val entity = Seq(
        (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
        (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

        (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,
        (UserSqlDao.TableName, Some(UserSqlDao.TableAlias), Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountUserId}" → s"${UserSqlDao.TableAlias}.${UserSqlDao.id}")).asDao,

        (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

      val expressionsToAgg = AggregationInput(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cAmount}", Sum, Some("turnover"))
      val daoCriteria = criteria.asGenericDao(None)
      val groupBy = Nil
      val orderBy = None

      (mysqlAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, groupBy, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      val result = txnAggService.getTurnOver(criteria, None, Nil, isOnTheFly = Some(true), Some(10))

      whenReady(result) { result ⇒

        result.isRight mustBe true
        result.right.get mustBe Seq(expectedResult)
      }
    }

    "get turnover from green plum when date range exceeds the configured no of days" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.now.minusDays(30).toOption,
        endDate = LocalDateTime.now.plusDays(100).toOption)

      val grouping = Grouping(column = "c.currency_name", value = "KES")
      val aggregatedValue = AggregatedValue(columnOrAlias = "turnover", value = "5000000.00")
      val daoAggregationResult = AggregationResult(aggregations = Seq(aggregatedValue), grouping = Seq(grouping))
      val expectedResult = TransactionAggregationResult(Some(2500000.00), None, None, None, Some("KES"), None, None, None, None, None)

      //dao inputs:

      val entity = Seq(
        (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
        (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

        (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,
        (UserSqlDao.TableName, Some(UserSqlDao.TableAlias), Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountUserId}" -> s"${UserSqlDao.TableAlias}.${UserSqlDao.id}")).asDao,

        (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

      val expressionsToAgg = AggregationInput(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cAmount}", Sum, Some("turnover"))
      val daoCriteria = criteria.asGenericDao(None)
      val groupBy = Nil
      val orderBy = None

      (gpAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, groupBy, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      val result = txnAggService.getTurnOver(criteria, None, Nil, isOnTheFly = Some(false), None)

      whenReady(result) { result ⇒

        result mustBe Right(Seq(expectedResult))
      }
    }

    "fail to get turnover if aggregated amount is invalid" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.of(2019, 12, 1, 0, 0, 0).toOption,
        endDate = LocalDateTime.of(2019, 12, 6, 0, 0, 0).toOption)

      val grouping = Grouping(column = "currency_name", value = "KES")
      val aggregatedValue = AggregatedValue(columnOrAlias = "turnover", value = "random query output")
      val daoAggregationResult = AggregationResult(aggregations = Seq(aggregatedValue), grouping = Seq(grouping))

      //dao inputs:

      val entity = Seq(
        (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
        (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

        (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,
        (UserSqlDao.TableName, Some(UserSqlDao.TableAlias), Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountUserId}" → s"${UserSqlDao.TableAlias}.${UserSqlDao.id}")).asDao,

        (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

      val expressionsToAgg = AggregationInput(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cAmount}", Sum, Some("turnover"))
      val daoCriteria = criteria.asGenericDao(None)
      val groupBy = Nil
      val orderBy = None

      (mysqlAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, groupBy, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      val result = txnAggService.getTurnOver(criteria, None, Nil, isOnTheFly = Some(true), None)

      whenReady(result) { result ⇒

        result.isLeft mustBe true
        result.left.get.message contains "Unable to convert AggregationResult to TransactionAggregationResult"
      }
    }

    "get turnover with different grouping" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.of(2019, 12, 1, 0, 0, 0).toOption,
        endDate = LocalDateTime.of(2019, 12, 5, 0, 0, 0).toOption)

      val txnGrouping = TransactionGrouping(currencyCode = true, transactionType = true)

      val grouping = Seq(
        Grouping(column = "c.currency_name", value = "KES"),
        Grouping(column = "tx.transaction_type", value = "international_remittance"))
      val aggregatedValue = AggregatedValue(columnOrAlias = "turnover", value = "5000000")
      val daoAggregationResult = AggregationResult(aggregations = Seq(aggregatedValue), grouping = grouping)
      val expectedResult = Seq(TransactionAggregationResult(Some(2500000), None, None, None, Some("KES"), None, None, None, None, None))

      //dao inputs:
      val entity = Seq(
        (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
        (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

        (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,
        (UserSqlDao.TableName, Some(UserSqlDao.TableAlias), Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountUserId}" → s"${UserSqlDao.TableAlias}.${UserSqlDao.id}")).asDao,

        (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

      val expressionsToAgg = AggregationInput(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cAmount}", Sum, Some("turnover"))
      val groupByInputs = txnGrouping.asDao
      val daoCriteria = criteria.asGenericDao(txnGrouping.some)
      val orderBy = None
      (mysqlAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, groupByInputs, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      (gpAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, groupByInputs, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      val result = txnAggService.getTurnOver(criteria, Some(txnGrouping), Nil, isOnTheFly = Some(true), None)

      whenReady(result) { result ⇒

        result mustBe Right(expectedResult)
      }
    }

    "get third_party_fees from mysql" in {
      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.of(2019, 12, 1, 0, 0, 0).toOption,
        endDate = LocalDateTime.of(2019, 12, 6, 0, 0, 0).toOption)

      val aggregatedValue = AggregatedValue(columnOrAlias = TransactionAggregationService.thirdPartyFees, value = "999.00")
      val daoAggregationResult = AggregationResult(aggregations = Seq(aggregatedValue), grouping = Nil)

      val expectedResult = TransactionAggregationResult(sumAmount = Some(499.50))

      val entity = Seq(
        (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
        (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

        (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,

        (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

      val mockExpression =
        s"""
           |CASE
           |WHEN pr.name = 'mpesa' AND tx.type IS NULL AND c.currency_name IS NULL THEN
           |10.50
           |WHEN pr.name = 'mpesa' AND tx.type = 'currency_exchange' AND c.currency_name IS NULL THEN
           |CASE
           |WHEN tx.amount >= 120 AND tx.amount <= 350 THEN 0.25
           |WHEN tx.amount > 350 AND tx.amount <= 600 THEN 1.25
           |WHEN tx.amount > 600 THEN 3.25
           |ELSE 0.0
           |END
           |WHEN pr.name = 'pesalink' AND tx.type IS NULL AND c.currency_name IS NULL THEN
           |tx.amount * 0.0275
           |ELSE 0.0 END
         """.stripMargin

      (thirdPartyFeeExpressionCreator.getCompleteThirdPartyFeesCalculationNestedExpression _)
        .when(entity(0), TransactionSqlDao.cAmount, None)
        .returns(Right(mockExpression).toFuture)

      val expressionsToAgg = AggregationInput(mockExpression, Sum, Some(TransactionAggregationService.thirdPartyFees))
      val daoCriteria = criteria.asGenericDao(None)

      (mysqlAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, Nil, None, None, None)
        .returns(Right(Seq(daoAggregationResult)))

      val result = txnAggService.getThirdPartyFees(criteria, None, Nil, isOnTheFly = Some(true), Some(10))

      whenReady(result) { result ⇒

        result mustBe Right(Seq(expectedResult))
      }
    }

    "get third_party_fees from mysql (grouped by institutions)" in {
      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.of(2019, 12, 1, 0, 0, 0).toOption,
        endDate = LocalDateTime.of(2019, 12, 6, 0, 0, 0).toOption)

      val daoAggregationResult = Seq(
        AggregationResult(aggregations = Seq(AggregatedValue(columnOrAlias = TransactionAggregationService.thirdPartyFees, value = "999.00")), grouping = Seq(Grouping(txnProviderAlias, "pesalink"))),
        AggregationResult(aggregations = Seq(AggregatedValue(columnOrAlias = TransactionAggregationService.thirdPartyFees, value = "888.00")), grouping = Seq(Grouping(txnProviderAlias, "mpesa"))),
        AggregationResult(aggregations = Seq(AggregatedValue(columnOrAlias = TransactionAggregationService.thirdPartyFees, value = "777.00")), grouping = Seq(Grouping(txnProviderAlias, "airtel"))))

      val expectedResult = Seq(
        TransactionAggregationResult(sumAmount = Some(499.50), institutionGrouping = Some("pesalink")),
        TransactionAggregationResult(sumAmount = Some(444.00), institutionGrouping = Some("mpesa")),
        TransactionAggregationResult(sumAmount = Some(388.50), institutionGrouping = Some("airtel")))

      val entity = Seq(
        (TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias), Nil).asDao,
        (AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}" → s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}")).asDao,

        (CurrencySqlDao.TableName, Some(CurrencySqlDao.TableAlias),
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}" → s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}")).asDao,

        (ProviderSqlDao.TableName, ProviderSqlDao.TableAlias.some,
          Seq(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}" → s"${ProviderSqlDao.TableAlias}.${Provider.cId}")).asDao)

      val mockExpression =
        s"""
           |CASE
           |WHEN pr.name = 'mpesa' AND tx.type IS NULL AND c.currency_name IS NULL THEN
           |10.50
           |WHEN pr.name = 'mpesa' AND tx.type = 'currency_exchange' AND c.currency_name IS NULL THEN
           |CASE
           |WHEN tx.amount >= 120 AND tx.amount <= 350 THEN 0.25
           |WHEN tx.amount > 350 AND tx.amount <= 600 THEN 1.25
           |WHEN tx.amount > 600 THEN 3.25
           |ELSE 0.0
           |END
           |WHEN pr.name = 'pesalink' AND tx.type IS NULL AND c.currency_name IS NULL THEN
           |tx.amount * 0.0275
           |ELSE 0.0 END
         """.stripMargin

      (thirdPartyFeeExpressionCreator.getCompleteThirdPartyFeesCalculationNestedExpression _)
        .when(entity(0), TransactionSqlDao.cAmount, None)
        .returns(Right(mockExpression).toFuture)

      val expressionsToAgg = AggregationInput(mockExpression, Sum, Some(TransactionAggregationService.thirdPartyFees))
      val daoCriteria = criteria.asGenericDao(TransactionGrouping(institution = true).some)
      val groupByInput = GroupByInput(txnProvider, None, Some(txnProviderAlias))

      (mysqlAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, Seq(groupByInput), None, None, None)
        .returns(Right(daoAggregationResult))

      val txnGrouping = TransactionGrouping(institution = true)
      val result = txnAggService.getThirdPartyFees(criteria, Some(txnGrouping), Nil, isOnTheFly = Some(true), Some(10))

      whenReady(result) { result ⇒

        result mustBe Right(expectedResult)
      }
    }

    "get total balance of collection accounts without any grouping" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        accountType = "collection".toOption,
        startDate = LocalDateTime.of(2019, 12, 1, 0, 0, 0).toOption,
        endDate = LocalDateTime.of(2019, 12, 5, 0, 0, 0).toOption)

      val grouping = Seq(
        Grouping(column = currencyName, value = "KES"))
      val aggregatedValue = AggregatedValue(columnOrAlias = "balance", value = "50000")
      val daoAggregationResult = AggregationResult(aggregations = Seq(aggregatedValue), grouping = grouping)
      val expectedResult = Seq(TransactionAggregationResult(Some(50000), None, None, None, Some("KES"), None, None, None, None, None))

      //dao inputs:
      val entity = Seq(Entity("accounts", Some("a"), List()), Entity("currencies", Some("c"), List(JoinColumn("a.currency_id", "c.id"))),
        Entity("account_types", Some("at"), List(JoinColumn("a.account_type_id", "at.id"))))
      val expressionsToAgg = AggregationInput(s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cBalance}", Sum, Some("balance"))

      val daoCriteria = criteria.asGenericDaoWithoutTransaction
      val orderBy = None
      (mysqlAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, Nil, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      (gpAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, Nil, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      val result = txnAggService.getTotalAccountBalances(criteria, None, Nil, isOnTheFly = Some(true), None)

      whenReady(result) { result ⇒

        result mustBe Right(expectedResult)
      }
    }

    "get total balance of distribution accounts with only currency grouping" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        accountType = "distribution".toOption,
        startDate = LocalDateTime.of(2019, 12, 1, 0, 0, 0).toOption,
        endDate = LocalDateTime.of(2019, 12, 5, 0, 0, 0).toOption)

      val txnGrouping = TransactionGrouping(currencyCode = true)

      val grouping = Seq(
        Grouping(column = currencyName, value = "KES"))
      val aggregatedValue = AggregatedValue(columnOrAlias = "balance", value = "50000")
      val daoAggregationResult = AggregationResult(aggregations = Seq(aggregatedValue), grouping = grouping)
      val expectedResult = Seq(TransactionAggregationResult(Some(50000), None, None, None, Some("KES"), None, None, None, None, None))

      //dao inputs:
      val entity = Seq(Entity("accounts", Some("a"), List()), Entity("currencies", Some("c"), List(JoinColumn("a.currency_id", "c.id"))),
        Entity("account_types", Some("at"), List(JoinColumn("a.account_type_id", "at.id"))))
      val expressionsToAgg = AggregationInput(s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cBalance}", Sum, Some("balance"))
      val groupByInputs = txnGrouping.asDao
      val daoCriteria = criteria.asGenericDaoWithoutTransaction
      val orderBy = None

      (mysqlAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, groupByInputs, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      (gpAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, groupByInputs, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      val result = txnAggService.getTotalAccountBalances(criteria, Some(txnGrouping), Nil, isOnTheFly = Some(true), None)

      whenReady(result) { result ⇒

        result mustBe Right(expectedResult)
      }
    }

    "get total balance of collection accounts with currency and institution grouping" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.of(2019, 12, 1, 0, 0, 0).toOption,
        endDate = LocalDateTime.of(2019, 12, 5, 0, 0, 0).toOption)

      val txnGrouping = TransactionGrouping(currencyCode = true, institution = true)

      val grouping = Seq(
        Grouping(column = currencyName, value = "KES"),
        Grouping(column = txnProviderAlias, value = "SBM"))
      val aggregatedValue = AggregatedValue(columnOrAlias = "balance", value = "50000")
      val daoAggregationResult = AggregationResult(aggregations = Seq(aggregatedValue), grouping = grouping)
      val expectedResult = Seq(TransactionAggregationResult(Some(50000), None, None, None, Some("KES"), Some("SBM"), None, None, None, None))

      //dao inputs:
      val entity = Seq(Entity("transactions", Some("tx"), List()), Entity("currencies", Some("c"), List(JoinColumn("tx.currency_id", "c.id"))),
        Entity("accounts", Some("a"), List(JoinColumn("tx.primary_account_id", "a.id"))),
        Entity("account_types", Some("at"), List(JoinColumn("a.account_type_id", "at.id"))))

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
      val expressionsToAgg = AggregationInput(rawExpression, Sum, Some("balance"))
      val groupByInputs = txnGrouping.asDao
      val daoCriteria = criteria.asGenericDao(txnGrouping.some)
      val orderBy = None

      (mysqlAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(*, Seq(expressionsToAgg), daoCriteria, groupByInputs, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      (gpAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(*, Seq(expressionsToAgg), daoCriteria, groupByInputs, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      val result = txnAggService.getTotalBalance(criteria, Some(txnGrouping), Nil, isOnTheFly = Some(true), None)

      whenReady(result) { result ⇒

        result mustBe Right(expectedResult)
      }
    }

    "get total balance of user accounts" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        startDate = LocalDateTime.of(2019, 12, 1, 0, 0, 0).toOption,
        endDate = LocalDateTime.of(2019, 12, 11, 0, 0, 0).toOption)

      val txnGrouping = TransactionGrouping(currencyCode = true)

      val grouping = Seq(
        Grouping(column = currencyName, value = "KES"))
      val aggregatedValue = AggregatedValue(columnOrAlias = "balance", value = "50000")
      val daoAggregationResult = AggregationResult(aggregations = Seq(aggregatedValue), grouping = grouping)
      val expectedResult = Seq(TransactionAggregationResult(Some(50000), None, None, None, Some("KES"), None, None, None, None, None))

      //dao inputs:
      val entity = Seq(Entity("accounts", Some("a"), List()), Entity("currencies", Some("c"), List(JoinColumn("a.currency_id", "c.id"))),
        Entity("account_types", Some("at"), List(JoinColumn("a.account_type_id", "at.id"))))
      val expressionsToAgg = AggregationInput(s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cBalance}", Sum, Some("balance"))

      val groupByInputs = txnGrouping.asDao
      val daoCriteria = criteria.asGenericDaoWithoutTransaction
      val orderBy = None

      (gpAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(entity, Seq(expressionsToAgg), daoCriteria, groupByInputs, orderBy, None, None).returns(Right(Seq(daoAggregationResult)))

      val result = txnAggService.getTotalAccountBalances(criteria, Some(txnGrouping), Nil, isOnTheFly = Some(true), Some(7))

      whenReady(result) { result ⇒

        result mustBe Right(expectedResult)
      }
    }

    "get total cashin amount grouped by daily" in {

      val criteria = TxnAggregationsCriteria(
        currencyCode = "KES".toOption,
        institution = "SBM".toOption,
        startDate = LocalDateTime.of(2019, 12, 1, 0, 0, 0).toOption,
        endDate = LocalDateTime.of(2019, 12, 5, 0, 0, 0).toOption)

      val txnGrouping = TransactionGrouping(currencyCode = true, institution = true, daily = true)

      val grouping = Seq(
        Grouping(column = currencyName, value = "KES"),
        Grouping(column = txnProviderAlias, value = "SBM"))

      val aggregatedValue1 = AggregatedValue(columnOrAlias = "balance", value = "50000")
      val aggregatedValue2 = AggregatedValue(columnOrAlias = "balance", value = "30000")
      val aggregatedValue3 = AggregatedValue(columnOrAlias = "balance", value = "20000")
      val aggregatedValue4 = AggregatedValue(columnOrAlias = "balance", value = "60000")
      val aggregatedValue5 = AggregatedValue(columnOrAlias = "balance", value = "55000")
      val daoAggregationResult1 = AggregationResult(aggregations = Seq(aggregatedValue1), grouping = Grouping(column = "date", value = "2019-12-01") +: grouping)
      val daoAggregationResult2 = AggregationResult(aggregations = Seq(aggregatedValue2), grouping = Grouping(column = "date", value = "2019-12-02") +: grouping)
      val daoAggregationResult3 = AggregationResult(aggregations = Seq(aggregatedValue3), grouping = Grouping(column = "date", value = "2019-12-03") +: grouping)
      val daoAggregationResult4 = AggregationResult(aggregations = Seq(aggregatedValue4), grouping = Grouping(column = "date", value = "2019-12-04") +: grouping)
      val daoAggregationResult5 = AggregationResult(aggregations = Seq(aggregatedValue5), grouping = Grouping(column = "date", value = "2019-12-05") +: grouping)

      val expectedResult = Seq(
        TransactionAggregationResult(Some(50000), None, None, None, Some("KES"), Some("SBM"), None, LocalDate.of(2019, 12, 1).toOption, None, None),
        TransactionAggregationResult(Some(30000), None, None, None, Some("KES"), Some("SBM"), None, LocalDate.of(2019, 12, 2).toOption, None, None),
        TransactionAggregationResult(Some(20000), None, None, None, Some("KES"), Some("SBM"), None, LocalDate.of(2019, 12, 3).toOption, None, None),
        TransactionAggregationResult(Some(60000), None, None, None, Some("KES"), Some("SBM"), None, LocalDate.of(2019, 12, 4).toOption, None, None),
        TransactionAggregationResult(Some(55000), None, None, None, Some("KES"), Some("SBM"), None, LocalDate.of(2019, 12, 5).toOption, None, None))

      //dao inputs:
      val entity = Seq(Entity("transactions", Some("tx"), List()), Entity("currencies", Some("c"), List(JoinColumn("tx.currency_id", "c.id"))),
        Entity("accounts", Some("a"), List(JoinColumn("tx.primary_account_id", "a.id"))),
        Entity("account_types", Some("at"), List(JoinColumn("a.account_type_id", "at.id"))))

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

      val expressionsToAgg = AggregationInput(rawExpression, Sum, Some("balance"))
      val groupByInputs = txnGrouping.asDao
      val daoCriteria = criteria.asGenericDao(txnGrouping.some)
      val orderBy = None

      (mysqlAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(*, Seq(expressionsToAgg), daoCriteria, groupByInputs, orderBy, None, None).returns(Right(Seq(daoAggregationResult1, daoAggregationResult2, daoAggregationResult3, daoAggregationResult4, daoAggregationResult5)))

      (gpAggDao.aggregate(
        _: Seq[Entity],
        _: Seq[AggregationInput],
        _: Seq[CriteriaField[_]],
        _: Seq[GroupByInput],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(*, Seq(expressionsToAgg), daoCriteria, groupByInputs, orderBy, None, None).returns(Right(Seq(daoAggregationResult1, daoAggregationResult2, daoAggregationResult3, daoAggregationResult4, daoAggregationResult5)))

      val result = txnAggService.getTotalAmount(criteria, Some(txnGrouping), Nil, isOnTheFly = Some(true), None)

      whenReady(result) { result ⇒

        result mustBe Right(expectedResult)
      }
    }

  }

}
