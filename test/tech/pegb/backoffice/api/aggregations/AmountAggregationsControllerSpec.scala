package tech.pegb.backoffice.api.aggregations

import java.time.{LocalDate, LocalDateTime}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.aggregations.controllers.Constants
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.domain.aggregations.abstraction.{RevenueMarginCalculator, TransactionAggregationFactory}
import tech.pegb.backoffice.domain.aggregations.dto.{Margin, TransactionAggregationResult, TransactionGrouping, TxnAggregationsCriteria}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.aggregation.Implicits._
import tech.pegb.backoffice.mapping.api.domain.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.backoffice.util.time.{Week}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class AmountAggregationsControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  val latestVersionService = stub[LatestVersionService]
  val txnAggregationFactory = stub[TransactionAggregationFactory]
  val revenueMarginCalculator = stub[RevenueMarginCalculator]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[LatestVersionService].to(latestVersionService),
      bind[TransactionAggregationFactory].to(txnAggregationFactory),
      bind[RevenueMarginCalculator].to(revenueMarginCalculator),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val url = "/api/aggregations"

  "AmountAggregationController" should {

    "get turnover for KES" in {

      val criteria = (None, None, None, None, Some(LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0))),
        Some(LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59))), None, None, None, "KES".some, Set[String]()).asDomain
      val mockLatestVersion = LocalDateTime.now

      (latestVersionService.getLatestVersion _)
        .when(criteria.get).returns(Future.successful(Right(mockLatestVersion.toString.toOption)))

      val mockAggFunction = (arg1: TxnAggregationsCriteria,
        arg2: Option[TransactionGrouping],
        arg3: Seq[Ordering],
        arg4: Option[Boolean],
        arg5: Option[Int]) ⇒ {

        val txnCriteria = TxnAggregationsCriteria(currencyCode = "KES".some,
          startDate = LocalDateTime.of(2019, 1, 30, 23, 59, 59).some,
          endDate = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some)

        Future.successful(Right(Seq(
          TransactionAggregationResult(sumAmount = BigDecimal("1000").some, criteria = txnCriteria.some)
        )))
      }

      (txnAggregationFactory.getAggregationFunction _).when(Constants.Aggregations.TurnOver)
        .returns(Some(mockAggFunction))

      val resp = route(app, FakeRequest(GET, s"$url?aggregation=turnover&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30")).get
      val expectedJson =
        s"""
           |{"total":1,
           |"results":[
           |{"aggregation":"turnover",
           |"amount":1000,
           |"currency_code":"KES",
           |"transaction_type":null,
           |"institution":null,
           |"time_period":null}],
           |"limit":null,"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toString.toOption
    }

    "get gross_revenue for KES" in {

      val criteria = (None, None, None, None, Some(LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0))),
        Some(LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59))), None, None, None, "KES".some, Set[String]()).asDomain
      val mockLatestVersion = LocalDateTime.now

      (latestVersionService.getLatestVersion _)
        .when(criteria.get).returns(Future.successful(Right(mockLatestVersion.toString.toOption)))

      val mockAggFunction = (arg1: TxnAggregationsCriteria,
                             arg2: Option[TransactionGrouping],
                             arg3: Seq[Ordering],
                             arg4: Option[Boolean],
                             arg5: Option[Int]) ⇒ {

        val txnCriteria = TxnAggregationsCriteria(currencyCode = "KES".some,
          startDate = LocalDateTime.of(2019, 1, 30, 23, 59, 59).some,
          endDate = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some)

        Future.successful(Right(Seq(
          // sumAmount is halved due to double sum for credit and debit txn lines
          TransactionAggregationResult(sumAmount = BigDecimal("1000").some, criteria = txnCriteria.some)
        )))
      }

      (txnAggregationFactory.getAggregationFunction _).when(Constants.Aggregations.GrossRevenue)
        .returns(Some(mockAggFunction))

      val resp = route(app, FakeRequest(GET, s"$url?aggregation=gross_revenue&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30")).get
      val expectedJson =
        s"""
           |{"total":1,
           |"results":[
           |{"aggregation":"gross_revenue",
           |"amount":1000,
           |"currency_code":"KES",
           |"transaction_type":null,
           |"institution":null,
           |"time_period":null}],
           |"limit":null,"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toString.toOption
    }

    "get gross_revenue for KES and group_by transaction_type" in {

      val criteria = (None, None, None, None, Some(LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0))),
        Some(LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59))), None, None, None, "KES".some, Set[String]()).asDomain
      val mockLatestVersion = LocalDateTime.now

      (latestVersionService.getLatestVersion _)
        .when(criteria.get).returns(Future.successful(Right(mockLatestVersion.toString.toOption)))

      val mockAggFunction = (arg1: TxnAggregationsCriteria,
                             arg2: Option[TransactionGrouping],
                             arg3: Seq[Ordering],
                             arg4: Option[Boolean],
                             arg5: Option[Int]) ⇒ {

        val txnCriteria = TxnAggregationsCriteria(currencyCode = "KES".some,
          startDate = LocalDateTime.of(2019, 1, 30, 23, 59, 59).some,
          endDate = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some)

        Future.successful(Right(Seq(
          TransactionAggregationResult(sumAmount = BigDecimal("1000").some, criteria = txnCriteria.some, transactionTypeGrouping = "P2P".some),
          TransactionAggregationResult(sumAmount = BigDecimal("2000").some, criteria = txnCriteria.some, transactionTypeGrouping = "Remittance".some),
          TransactionAggregationResult(sumAmount = BigDecimal("3000").some, criteria = txnCriteria.some, transactionTypeGrouping = "Exchange".some),
          TransactionAggregationResult(sumAmount = BigDecimal("4000").some, criteria = txnCriteria.some, transactionTypeGrouping = "Split Bill".some)
        )))
      }

      (txnAggregationFactory.getAggregationFunction _).when(Constants.Aggregations.GrossRevenue)
        .returns(Some(mockAggFunction))

      val resp = route(app, FakeRequest(GET, s"$url?aggregation=gross_revenue&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30&group_by=transaction_type")).get
      val expectedJson =
        s"""
           |{"total":4,
           |"results":[
           |{"aggregation":"gross_revenue",
           |"amount":3000,
           |"currency_code":"KES",
           |"transaction_type":"Exchange",
           |"institution":null,
           |"time_period":null},
           |{"aggregation":"gross_revenue",
           |"amount":1000,
           |"currency_code":"KES",
           |"transaction_type":"P2P",
           |"institution":null,
           |"time_period":null},
           |{"aggregation":"gross_revenue",
           |"amount":2000,
           |"currency_code":"KES",
           |"transaction_type":"Remittance",
           |"institution":null,
           |"time_period":null},
           |{"aggregation":"gross_revenue",
           |"amount":4000,
           |"currency_code":"KES",
           |"transaction_type":"Split Bill",
           |"institution":null,
           |"time_period":null}],
           |"limit":null,"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toString.toOption
    }

    "fail if invalid aggregation" in {

      val criteria = (None, None, None, None, Some(LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0))),
        Some(LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59))), None, None, None, "KES".some, Set[String]()).asDomain
      val mockLatestVersion = LocalDateTime.now

      (latestVersionService.getLatestVersion _)
        .when(criteria.get).returns(Future.successful(Right(mockLatestVersion.toString.toOption)))

      (txnAggregationFactory.getAggregationFunction _).when(Constants.Aggregations.GrossRevenue)
        .returns(None)

      val invalidAggregation = "total_chances_of_winning"
      val resp = route(app, FakeRequest(GET, s"$url?aggregation=$invalidAggregation&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30")
        .withHeaders(jsonHeaders)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Aggregation [total_chances_of_winning] is not valid"}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp)  mustBe expectedJson
    }


    "get gross_revenue for KES and group_by time_period where frequency is weekly" in {

      val criteria = (None, None, None, None, Some(LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0))),
        Some(LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59))), None, None, None, "KES".some, Set[String]()).asDomain
      val mockLatestVersion = LocalDateTime.now

      (latestVersionService.getLatestVersion _)
        .when(criteria.get).returns(Future.successful(Right(mockLatestVersion.toString.toOption)))

      val mockAggFunction = (arg1: TxnAggregationsCriteria,
                             arg2: Option[TransactionGrouping],
                             arg3: Seq[Ordering],
                             arg4: Option[Boolean],
                             arg5: Option[Int]) ⇒ {

        val txnCriteria = TxnAggregationsCriteria(currencyCode = "KES".some,
          startDate = LocalDateTime.of(2019, 1, 30, 23, 59, 59).some,
          endDate = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some)

        Future.successful(Right(Seq(
          TransactionAggregationResult(sumAmount = BigDecimal("1000").some, criteria = txnCriteria.some, week = Week(1, 2019).some),
          TransactionAggregationResult(sumAmount = BigDecimal("2000").some, criteria = txnCriteria.some, week = Week(2, 2019).some),
          TransactionAggregationResult(sumAmount = BigDecimal("3000").some, criteria = txnCriteria.some, week = Week(3, 2019).some),
          TransactionAggregationResult(sumAmount = BigDecimal("4000").some, criteria = txnCriteria.some, week = Week(4, 2019).some)
        )))
      }

      (txnAggregationFactory.getAggregationFunction _).when(Constants.Aggregations.GrossRevenue)
        .returns(Some(mockAggFunction))

      val resp = route(app, FakeRequest(GET, s"$url?aggregation=gross_revenue&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30&group_by=time_period&frequency=weekly")).get
      val expectedJson =
        s"""
           |{"total":4,
           |"results":[
           |{"aggregation":"gross_revenue",
           |"amount":1000,
           |"currency_code":"KES",
           |"transaction_type":null,
           |"institution":null,
           |"time_period":"1st Week, 2019"},
           |{"aggregation":"gross_revenue",
           |"amount":2000,
           |"currency_code":"KES",
           |"transaction_type":null,
           |"institution":null,
           |"time_period":"2nd Week, 2019"},
           |{"aggregation":"gross_revenue",
           |"amount":3000,
           |"currency_code":"KES",
           |"transaction_type":null,
           |"institution":null,
           |"time_period":"3rd Week, 2019"},
           |{"aggregation":"gross_revenue",
           |"amount":4000,
           |"currency_code":"KES",
           |"transaction_type":null,
           |"institution":null,
           |"time_period":"4th Week, 2019"}],
           |"limit":null,"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toString.toOption
    }

    "get third_party_fees for KES" in {

      val criteria = (None, None, None, None, Some(LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0))),
        Some(LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59))), None, None, None, "KES".some, Set[String]()).asDomain
      val mockLatestVersion = LocalDateTime.now

      (latestVersionService.getLatestVersion _)
        .when(criteria.get).returns(Future.successful(Right(mockLatestVersion.toString.toOption)))

      val mockAggFunction = (arg1: TxnAggregationsCriteria,
                             arg2: Option[TransactionGrouping],
                             arg3: Seq[Ordering],
                             arg4: Option[Boolean],
                             arg5: Option[Int]) ⇒ {

        val txnCriteria = TxnAggregationsCriteria(currencyCode = "KES".some,
          startDate = LocalDateTime.of(2019, 1, 30, 23, 59, 59).some,
          endDate = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some)

        Future.successful(Right(Seq(
          TransactionAggregationResult(sumAmount = BigDecimal("1000").some, criteria = txnCriteria.some),
        )))
      }

      (txnAggregationFactory.getAggregationFunction _).when(Constants.Aggregations.ThirdPartyFees)
        .returns(Some(mockAggFunction))

      val resp = route(app, FakeRequest(GET, s"$url?aggregation=third_party_fees&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30")).get
      val expectedJson =
        s"""
           |{"total":1,
           |"results":[
           |{"aggregation":"third_party_fees",
           |"amount":1000,
           |"currency_code":"KES",
           |"transaction_type":null,
           |"institution":null,
           |"time_period":null}],
           |"limit":null,"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toString.toOption
    }

    "fail if currency_code is invalid" in {

      val invalidCurrencyCode = "XYZ"
      val resp = route(app, FakeRequest(GET, s"$url?aggregation=third_party_fees&currency_code=$invalidCurrencyCode&date_from=2019-01-01&date_to=2019-01-30")
        .withHeaders(jsonHeaders)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid currency_code [$invalidCurrencyCode]"}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail if given date_from and date_to is invalid" in {
      val dateFrom = "2019-01-01"
      val dateToOlderThanDateFrom = "2018-12-31"
      val resp = route(app, FakeRequest(GET, s"$url?aggregation=third_party_fees&currency_code=KES&date_from=$dateFrom&date_to=$dateToOlderThanDateFrom")
        .withHeaders(jsonHeaders)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"date_from must be before or equal to date_to"}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail if transaction_type is invalid" in {
      val invalidTxnType = "lending"
      val resp = route(app, FakeRequest(GET, s"$url?aggregation=third_party_fees&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30&transaction_type=$invalidTxnType")
        .withHeaders(jsonHeaders)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Transaction type [lending] is not valid for aggregation. Valid transaction_type values [bank_transfer, cashin, cashout, etc_transactions]."}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail if account_type is invalid" in {
      val invalidAccType = "savings_account"
      val resp = route(app, FakeRequest(GET, s"$url?aggregation=third_party_fees&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30&account_type=$invalidAccType")
        .withHeaders(jsonHeaders)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Account type [savings_account] is not valid for aggregation. Valid account_type values [collection, distribution, standard_saving, standard_wallet]."}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail if frequency is invalid" in {
      val invalidFrequency = "fortnightly"
      val resp = route(app, FakeRequest(GET, s"$url?aggregation=third_party_fees&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30&group_by=time_period&frequency=$invalidFrequency")
        .withHeaders(jsonHeaders)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Frequency [fortnightly] is not valid for aggregation. Valid frequency values [daily,weekly,monthly]."}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      (contentAsString(resp)) mustBe expectedJson
    }

    "fail if group_by is invalid" in {
      val invalidGroupBy = "channel"
      val resp = route(app, FakeRequest(GET, s"$url?aggregation=gross_revenue&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30&group_by=$invalidGroupBy")
        .withHeaders(jsonHeaders)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Grouping by [channel] is not valid for aggregation. Valid group_by values [currency_code, institution, time_period, transaction_type]."}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail if order_by is invalid" in {
      val invalidOrderBy = "channel"
      val resp = route(app, FakeRequest(GET, s"$url?aggregation=gross_revenue&currency_code=KES&date_from=2019-01-01&date_to=2019-01-30&order_by=$invalidOrderBy")
        .withHeaders(jsonHeaders)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"invalid field for order_by found. Valid fields: [amount, currency_code, institution, time_period, transaction_type]"}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "get gross revenue margin for KES currency" in {
      val criteria = (None, None, None, None, Some(LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0))),
        Some(LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59))), None, None, None, "KES".some, Set[String]()).asDomain
      val mockLatestVersion = LocalDateTime.now

      (latestVersionService.getLatestVersion _)
        .when(criteria.get).returns(Future.successful(Right(mockLatestVersion.toString.toOption)))

      val mockResult = Seq(Margin(margin = BigDecimal("0.05"), currencyCode = "KES"))
      val aggCriteria = ("KES", None,  None, None, None, LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0)).some, LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59)).some, None).asDomain
      (revenueMarginCalculator.getRevenueMargin _).when(None, None, aggCriteria, None, Nil, None, None)
        .returns(Right(mockResult).toFuture)

      val resp = route(app, FakeRequest(GET, s"$url/gross_revenue_margin?currency_code=KES&date_from=2019-01-01&date_to=2019-01-30")).get
      val expectedJson =
        s"""
           |{"total":1,
           |"results":[
           |{"margin":0.05,
           |"currency_code":"KES",
           |"transaction_type":null,
           |"institution":null,
           |"time_period":null}],
           |"limit":null,"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toString.toOption
    }

    "get gross revenue margin only for KES currency MPESA and CASHIN" in {
      val criteria = (None, None, None, None, Some(LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0))),
        Some(LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59))), "cashin".some, None, None, "KES".some, Set[String]()).asDomain
      val mockLatestVersion = LocalDateTime.now

      (latestVersionService.getLatestVersion _)
        .when(criteria.get).returns(Future.successful(Right(mockLatestVersion.toString.toOption)))

      val mockResult = Seq(Margin(margin = BigDecimal("0.05"), currencyCode = "KES", transactionType = "cashin".some, institution = "mpesa".some))
      val aggCriteria = ("KES", "mpesa".some, "cashin".some, None, None, LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0)).some, LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59)).some, None).asDomain
      (revenueMarginCalculator.getRevenueMargin _).when(None, None, aggCriteria, None, Nil, None, None)
        .returns(Right(mockResult).toFuture)

      val resp = route(app, FakeRequest(GET, s"$url/gross_revenue_margin?currency_code=KES&date_from=2019-01-01&date_to=2019-01-30&transaction_type=cashin&institution=mpesa")).get
      val expectedJson =
        s"""
           |{"total":1,
           |"results":[
           |{"margin":0.05,
           |"currency_code":"KES",
           |"transaction_type":"cashin",
           |"institution":"mpesa",
           |"time_period":null}],
           |"limit":null,"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toString.toOption
    }

    "get gross revenue margin only for KES currency MPESA and CASHIN group by time_period with daily frequency" in {
      val criteria = (None, None, None, None, Some(LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0))),
        Some(LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59))), "cashin".some, None, None, "KES".some, Set[String]()).asDomain
      val mockLatestVersion = LocalDateTime.now

      (latestVersionService.getLatestVersion _)
        .when(criteria.get).returns(Future.successful(Right(mockLatestVersion.toString.toOption)))

      val mockResult = Seq(
        Margin(margin = BigDecimal("0.05"), currencyCode = "KES", transactionType = "cashin".some, institution = "mpesa".some, date = LocalDate.of(2019, 1, 1).some),
        Margin(margin = BigDecimal("0.02"), currencyCode = "KES", transactionType = "cashin".some, institution = "mpesa".some, date = LocalDate.of(2019, 1, 2).some),
        Margin(margin = BigDecimal("0.03"), currencyCode = "KES", transactionType = "cashin".some, institution = "mpesa".some, date = LocalDate.of(2019, 1, 3).some),
        Margin(margin = BigDecimal("0.07"), currencyCode = "KES", transactionType = "cashin".some, institution = "mpesa".some, date = LocalDate.of(2019, 1, 4).some)
      )
      val aggCriteria = ("KES", "mpesa".some, "cashin".some, None, None, LocalDateTimeFrom(LocalDateTime.of(2019, 1, 1, 0, 0, 0)).some, LocalDateTimeTo(LocalDateTime.of(2019, 1, 30, 23, 59, 59)).some, None).asDomain
      val grouping = Some("time_period").map(_.asDomain(frequency = "daily".some))
      (revenueMarginCalculator.getRevenueMargin _).when(None, None, aggCriteria, grouping, Nil, None, None)
        .returns(Right(mockResult).toFuture)

      val resp = route(app, FakeRequest(GET, s"$url/gross_revenue_margin?currency_code=KES&date_from=2019-01-01&date_to=2019-01-30&transaction_type=cashin&institution=mpesa&group_by=time_period&frequency=daily")).get
      val expectedJson =
        s"""
           |{"total":4,
           |"results":[
           |{"margin":0.05,
           |"currency_code":"KES",
           |"transaction_type":"cashin",
           |"institution":"mpesa",
           |"time_period":"2019-01-01"},
           |{"margin":0.02,
           |"currency_code":"KES",
           |"transaction_type":"cashin",
           |"institution":"mpesa",
           |"time_period":"2019-01-02"},
           |{"margin":0.03,
           |"currency_code":"KES",
           |"transaction_type":"cashin",
           |"institution":"mpesa",
           |"time_period":"2019-01-03"},
           |{"margin":0.07,
           |"currency_code":"KES",
           |"transaction_type":"cashin",
           |"institution":"mpesa",
           |"time_period":"2019-01-04"}],
           |"limit":null,"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toString.toOption
    }

  }

}
