package tech.pegb.backoffice.api.mock

import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.time.{LocalDate, LocalDateTime}
import java.util.{Locale, UUID}

import cats.implicits._
import com.google.inject.Inject
import io.swagger.annotations.{Api, ApiParam, ApiResponse, ApiResponses}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.RequiredHeaders._
import tech.pegb.backoffice.api.aggregations.controllers.{AmountAggregationsController, Constants}
import tech.pegb.backoffice.api.aggregations.dto.{AmountAggregation, Margin}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.mock.data.RevenueData._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo, PaginatedResult}
import tech.pegb.backoffice.api.swagger.model.AmountAggregationPaginatedResult
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.api.mock.data._
import tech.pegb.backoffice.api.mock.data.FloatData._

import scala.concurrent.Future
import scala.util.Random

@Api(value = "MockAmountAggregations", produces = "application/json")
class AggregationMockController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with AmountAggregationsController
  with RequiredHeaders with ConfigurationHeaders {

  import AggregationMockController._
  import tech.pegb.backoffice.api.mock.MockDataReader._

  val rand = new scala.util.Random()

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AmountAggregationPaginatedResult], message = "")))
  def getAmountAggregation(
    @ApiParam(required = true, defaultValue = "turnover",
      allowableValues = "turnover, gross_revenue, third_party_fees, balance, bank_transfer, total_cash_in, total_cash_out, total_etc_transactions", allowMultiple = false) aggregation: String,
    @ApiParam(required = true, defaultValue = "KES") currencyCode: String,
    @ApiParam(required = false, defaultValue = "2019-01-01") dateFrom: Option[LocalDateTimeFrom],
    @ApiParam(required = false, defaultValue = "2019-01-31") dateTo: Option[LocalDateTimeTo],
    @ApiParam(required = false, defaultValue = "", allowableValues = "cash_in, cash_out, etc_transactions, p2p, remittance, exchange, split_bill", allowMultiple = false) transactionType: Option[String],
    @ApiParam(required = false, defaultValue = "", allowableValues = "collection, distribution, end_customer", allowMultiple = false) accountType: Option[String],
    institution: Option[String],
    @ApiParam(required = false, defaultValue = "", allowableValues = "provider, individual, business", allowMultiple = false) userType: Option[String],
    @ApiParam(required = false, defaultValue = "", allowMultiple = false) accountNumber: Option[String],
    @ApiParam(required = false, defaultValue = "daily", allowableValues = "daily, weekly, monthly", allowMultiple = false) frequency: Option[String],
    @ApiParam(required = false, defaultValue = "1", allowMultiple = false) step: Option[Int],
    @ApiParam(required = false, allowableValues = "institution, transaction_type, trend_item, time_period", allowMultiple = false) groupBy: Option[String],
    @ApiParam(required = false, allowableValues = "aggregation, amount, institution, transaction_type, time_period", allowMultiple = false) orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val amountAggregation = if (aggregation.split(",").toSet.subsetOf(Set("turnover", "gross_revenue", "third_party_fees"))) {
      //val summary = timePeriodFiltering(currencyCode, dateFrom.map(_.localDateTime.toLocalDate), dateTo.map(_.localDateTime.toLocalDate))
      val summary = prepareRevenueData(currencyCode, dateFrom.map(_.localDateTime.toLocalDate), dateTo.map(_.localDateTime.toLocalDate))
      revenueGroupBy(aggregation, summary, currencyCode, groupBy, transactionType)
    } else if (aggregation.split(",").toSet.subsetOf(Set("bank_transfer", "total_cash_in", "total_cash_out", "total_etc_transactions"))) {
      cashFlowAggFromFile(currencyCode, dateFrom, dateTo, groupBy, institution, transactionType, frequency)
    } else {
      balanceAgg(currencyCode, dateFrom, dateTo, groupBy, institution, accountType, frequency, transactionType)
    }

    val s = step.getOrElse(1)
    val fin = amountAggregation.zipWithIndex.collect { case (e, i) if i % s == 0 ⇒ e }

    val response = handleApiResponse(Right(
      PaginatedResult(
        total = fin.size,
        results = fin,
        limit = limit,
        offset = offset).toJsonStr)).withLatestVersionHeader(LocalDateTime.now().toString.some)

    Future.successful(response)
  }

  private def cashFlowAggFromFile(
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    groupBy: Option[String],
    institution: Option[String],
    txnType: Option[String],
    frequency: Option[String]): Seq[AmountAggregation] = {
    val allRecords = readCashFlowReportDataFromInMemCsv()
    val currencyFilter = allRecords.filter(_.currency.toUpperCase == currencyCode.toUpperCase)
    val institutionFilter = institution.fold(currencyFilter)(i ⇒ currencyFilter.filter(_.institution.toUpperCase == i.toUpperCase))
    val finalData = institutionFilter.filter { d ⇒
      (dateFrom, dateTo) match {
        case (Some(f), Some(t)) ⇒ d.date >= dateFormatter(f.localDateTime) && d.date <= dateFormatter(t.localDateTime)
        case (Some(f), None) ⇒ d.date >= dateFormatter(f.localDateTime)
        case (None, Some(t)) ⇒ d.date <= dateFormatter(t.localDateTime)
        case (_, _) ⇒ true

      }
    }

    txnType match {
      case Some("cash_in") ⇒

        Seq(AmountAggregation(
          aggregation = "total_cash_in",
          amount = finalData.map(_.cashIn).sum,
          currencyCode = currencyCode,
          transactionType = None,
          institution = institution,
          timePeriod = None))
      case Some("cash_out") ⇒ Seq(
        AmountAggregation(
          aggregation = "total_cash_out",
          amount = finalData.map(_.cashOut).sum,
          currencyCode = currencyCode,
          transactionType = None,
          institution = institution,
          timePeriod = None))
      case Some("etc_transactions") ⇒ Seq(
        AmountAggregation(
          aggregation = "total_etc_transactions",
          amount = finalData.map(_.otherTransactions).sum,
          currencyCode = currencyCode,
          transactionType = None,
          institution = institution,
          timePeriod = None))
      case Some("bank_transfer") ⇒ Seq(
        AmountAggregation(
          aggregation = "bank_transfer",
          amount = finalData.map(_.bankTransfer).sum,
          currencyCode = currencyCode,
          transactionType = None,
          institution = institution,
          timePeriod = None))
      case _ ⇒
        logger.warn("NOT IMPLEMENTED IN MOCK, no use case for current dashboard")
        Seq()
    }

  }

  private def dateFormatter(date: LocalDateTime): String = {
    val d = date.toLocalDate
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    d.format(formatter)
  }

  def getGrossRevenueMargin(
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    transactionType: Option[String],
    institution: Option[String],
    @ApiParam(required = false, defaultValue = "daily", allowableValues = "daily, weekly, monthly", allowMultiple = false) frequency: Option[String],
    @ApiParam(required = false, defaultValue = "1.0", allowableValues = "range[0, 1.0]", allowMultiple = false) frequencyReductionFactor: Option[Float],
    @ApiParam(required = false, allowableValues = "institution, transaction_type, time_period", allowMultiple = false) groupBy: Option[String],
    @ApiParam(required = false, allowableValues = "margin, institution, transaction_type, time_period", allowMultiple = false) orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    val decimalPercentage = rand.nextInt(10001)
    Future.successful(handleApiResponse(Right(Margin(BigDecimal(s".$decimalPercentage"), currencyCode).toJsonStr)))
  }

  def getTrendDirection(currencyCode: String, aggregation: String, dateFrom: Option[LocalDateTimeFrom], dateTo: Option[LocalDateTimeTo]): Action[AnyContent] = ???

  private def balanceAgg(
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    groupBy: Option[String],
    institution: Option[String],
    accountType: Option[String],
    frequency: Option[String],
    transactionType: Option[String]): Seq[AmountAggregation] = {

    groupBy match {
      case None ⇒
        accountType match {
          case Some("collection") ⇒ Seq(AmountAggregation(
            aggregation = "balance",
            amount = BigDecimal(1975568),
            currencyCode = currencyCode,
            transactionType = None,
            institution = None,
            timePeriod = None))
          case Some("distribution") ⇒ Seq(
            AmountAggregation(
              aggregation = "balance",
              amount = sbm.distributionBalance + kcb.distributionBalance + mpesa.distributionBalance,
              currencyCode = currencyCode,
              transactionType = None,
              institution = None,
              timePeriod = None))
          case Some("end_customer") ⇒ Seq(
            AmountAggregation(
              aggregation = "balance",
              amount = totalUserBalance,
              currencyCode = currencyCode,
              transactionType = None,
              institution = None,
              timePeriod = None))
          case _ ⇒
            logger.warn("NOT IMPLEMENTED IN MOCK, no use case for current dashboard")
            Seq()
        }
      case Some("institution") ⇒ {
        accountType match {
          case Some("distribution") ⇒ Seq(
            AmountAggregation(
              aggregation = "balance",
              amount = sbm.distributionBalance,
              currencyCode = currencyCode,
              transactionType = None,
              institution = sbm.name.some,
              timePeriod = None),
            AmountAggregation(
              aggregation = "balance",
              amount = kcb.distributionBalance,
              currencyCode = currencyCode,
              transactionType = None,
              institution = kcb.name.some,
              timePeriod = None),
            AmountAggregation(
              aggregation = "balance",
              amount = mpesa.distributionBalance,
              currencyCode = currencyCode,
              transactionType = None,
              institution = mpesa.name.some,
              timePeriod = None))
          case _ ⇒
            logger.warn("NOT IMPLEMENTED IN MOCK, no use case for current dashboard")
            Seq()
        }
      }
      case Some("transaction_type") | Some("institution,transaction_type,time_period") ⇒ {
        (dateFrom, dateTo) match {
          case (Some(from), Some(to)) ⇒
            val mockFrequency = filterFloatTrendData(
              from.localDateTime.toLocalDate,
              to.localDateTime.toLocalDate,
              frequency.getOrElse("daily"),
              institution.getOrElse(""),
              transactionType.getOrElse(""))

            mockFrequency.map(m ⇒
              AmountAggregation(
                aggregation = "balance",
                amount = m.amount,
                currencyCode = currencyCode,
                transactionType = transactionType,
                institution = institution,
                timePeriod = m.timePeriodData.some))
          case _ ⇒
            logger.warn("NOT IMPLEMENTED IN MOCK, no use case for current dashboard")
            Seq()
        }
      }
    }
  }

  private def revenueGroupBy(
    aggregation: String,
    summary: Map[String, RevenuePerTransactionType],
    currencyCode: String,
    groupBy: Option[String],
    transactionType: Option[String]): Seq[AmountAggregation] = {
    val aggMap = summary

    val aggList = aggregation.split(",")

    groupBy match {
      case None ⇒ aggList.flatMap { aggName ⇒
        val agg = aggMap.get(aggName)

        agg.map(a ⇒
          AmountAggregation(
            aggregation = aggName,
            amount = a.p2p.map(_.amount).sum + a.remittance.map(_.amount).sum + a.splitBill.map(_.amount).sum + a.exchange.map(_.amount).sum,
            currencyCode = currencyCode,
            transactionType = None,
            institution = None,
            timePeriod = None))
      }
      case Some("time_period") ⇒ aggList.flatMap { aggName ⇒
        val agg = aggMap.get(aggName)

        agg.map(a ⇒
          for {
            (((p2p, remittance), splitBill), exchange) ← a.p2p zip a.remittance zip a.splitBill zip a.exchange
          } yield {
            AmountAggregation(
              aggregation = aggName,
              amount = p2p.amount + remittance.amount + splitBill.amount + exchange.amount,
              currencyCode = currencyCode,
              transactionType = None,
              institution = None,
              timePeriod = p2p.timePeriod.toString.some)
          })
      }.flatten.toSeq

      case Some("transaction_type") ⇒ aggList.flatMap { aggName ⇒
        val agg = aggMap.get(aggName)

        agg.map(a ⇒ Seq(
          AmountAggregation(
            aggregation = aggName,
            amount = a.p2p.map(_.amount).sum,
            currencyCode = currencyCode,
            transactionType = "P2P".some,
            institution = None,
            timePeriod = None),
          AmountAggregation(
            aggregation = aggName,
            amount = a.remittance.map(_.amount).sum,
            currencyCode = currencyCode,
            transactionType = "Remittance".some,
            institution = None,
            timePeriod = None),
          AmountAggregation(
            aggregation = aggName,
            amount = a.splitBill.map(_.amount).sum,
            currencyCode = currencyCode,
            transactionType = "Split Bill".some,
            institution = None,
            timePeriod = None),
          AmountAggregation(
            aggregation = aggName,
            amount = a.exchange.map(_.amount).sum,
            currencyCode = currencyCode,
            transactionType = "Exchange".some,
            institution = None,
            timePeriod = None)))
      }.flatten.toSeq

    }
  }

  private def prepareRevenueData(
    currencyCode: String,
    dateFrom: Option[LocalDate],
    dateTo: Option[LocalDate]): Map[String, RevenuePerTransactionType] = {
    Map(
      "turnover" → RevenuePerTransactionType(
        p2p = filterByOptionalDate(p2pTurnover, dateFrom, dateTo),
        remittance = filterByOptionalDate(remittanceTurnover, dateFrom, dateTo),
        exchange = filterByOptionalDate(exchangeTurnover, dateFrom, dateTo),
        splitBill = filterByOptionalDate(splitBillTurnover, dateFrom, dateTo)),
      "gross_revenue" → RevenuePerTransactionType(
        p2p = filterByOptionalDate(p2pRevenue, dateFrom, dateTo),
        remittance = filterByOptionalDate(remittanceRevenue, dateFrom, dateTo),
        exchange = filterByOptionalDate(exchangeRevenue, dateFrom, dateTo),
        splitBill = filterByOptionalDate(splitBillRevenue, dateFrom, dateTo)),
      "third_party_fees" → RevenuePerTransactionType(
        p2p = filterByOptionalDate(p2pCost, dateFrom, dateTo),
        remittance = filterByOptionalDate(remittanceCost, dateFrom, dateTo),
        exchange = filterByOptionalDate(exchangeCost, dateFrom, dateTo),
        splitBill = filterByOptionalDate(splitBillCost, dateFrom, dateTo)))
  }

  def getCashFlowTotals(
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    currency: String,
    institution: Option[String]) = ???
}

object AggregationMockController {
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  //float
  val totalUserBalance = BigDecimal(1108690)

  val sbm = InstitutionFloats(
    id = UUID.randomUUID(),
    name = "SBM",
    distributionBalance = BigDecimal(77500),
    parameterId = UUID.randomUUID(),
    userBalancePercentage = totalUserBalance,
    calculatedUserBalance = BigDecimal(100.00),
    pendingBalance = BigDecimal(0))

  val kcb = InstitutionFloats(
    id = UUID.randomUUID(),
    name = "KCB",
    distributionBalance = BigDecimal(112400),
    parameterId = UUID.randomUUID(),
    userBalancePercentage = totalUserBalance,
    calculatedUserBalance = BigDecimal(100.00),
    pendingBalance = BigDecimal(0))

  val mpesa = InstitutionFloats(
    id = UUID.randomUUID(),
    name = "mPesa",
    distributionBalance = BigDecimal(337500),
    parameterId = UUID.randomUUID(),
    userBalancePercentage = totalUserBalance,
    calculatedUserBalance = BigDecimal(100.00),
    pendingBalance = BigDecimal(14700))

  val r = new Random()
  val weekFields = WeekFields.of(Locale.getDefault)

  val institutionTrendMap = Map(
    sbm.name → InstitutionTxnTrends(
      cashin = sbmCashIn,
      cashout = sbmCashOut,
      transactions = sbmTxn,
      closingUserBalance = Nil), //Removed per Salih to make the graphs more visible
    kcb.name → InstitutionTxnTrends(
      cashin = kcbCashIn,
      cashout = kcbCashOut,
      transactions = kcbTxn,
      closingUserBalance = Nil),
    mpesa.name → InstitutionTxnTrends(
      cashin = mpesaCashIn,
      cashout = mpesaCashOut,
      transactions = mpesaTxn,
      closingUserBalance = Nil))

  def filterFloatTrendData(from: LocalDate, to: LocalDate, frequency: String, institution: String, transactionType: String): Seq[MockTrendData] = {
    val data = institutionTrendMap.get(institution)

    val timePeriod = data.fold(Seq.empty[TimePeriodData])(d ⇒ transactionType match {
      case Constants.TxnType.CashIn ⇒ d.cashin
      case Constants.TxnType.CashOut ⇒ d.cashout
      case Constants.TxnType.EtcTxns ⇒ d.transactions
      case Constants.ClosingUserBalance ⇒ d.closingUserBalance
      case _ ⇒ Seq.empty[TimePeriodData]
    }).sortBy(_.timePeriod)

    val filtered = filterByOptionalDate(timePeriod, from.some, to.some)

    frequency match {
      case "daily" ⇒
        filtered.map(d ⇒ MockTrendData(d.amount, d.timePeriod.toString))
      case "weekly" ⇒
        val uniqueWeek = filtered.map(d ⇒ s"${toOrdinalWeek(d.timePeriod.get(weekFields.weekOfYear()), d.timePeriod.getYear)}").distinct
        val groupedByWeek = filtered.map(d ⇒ (s"${toOrdinalWeek(d.timePeriod.get(weekFields.weekOfYear()), d.timePeriod.getYear)}", d.amount)).groupBy(_._1)
        uniqueWeek.map(w ⇒ MockTrendData(groupedByWeek.get(w).fold(BigDecimal(0))(_.map(_._2).sum), w))
      case "monthly" ⇒
        val monthRange = filtered.map(d ⇒ s"${d.timePeriod.getMonth.name()} ${d.timePeriod.getYear}").distinct
        val groupedByMonth = filtered.map(d ⇒ (s"${d.timePeriod.getMonth.name()} ${d.timePeriod.getYear}", d.amount)).groupBy(_._1)
        monthRange.map(m ⇒ MockTrendData(groupedByMonth.get(m).fold(BigDecimal(0))(_.map(_._2).sum), m))
    }
  }

  def generateTxTrend(from: LocalDate, to: LocalDate, frequency: String): Seq[MockTrendData] = {
    val dateRange = Iterator.iterate(from)(_.plusDays(1L)).takeWhile(!_.isAfter(to)).toList
    frequency match {
      case "daily" ⇒
        dateRange.map(d ⇒ MockTrendData(r.nextInt(20000), d.toString))
      case "weekly" ⇒
        val weekRange = dateRange.map(d ⇒ s"${toOrdinalWeek(d.get(weekFields.weekOfYear()), d.getYear)}").distinct
        weekRange.map(d ⇒ MockTrendData(r.nextInt(20000), d))
      case "monthly" ⇒
        val monthRange = dateRange.map(d ⇒ s"${d.getMonth.name()} ${d.getYear}").distinct
        monthRange.map(d ⇒ MockTrendData(r.nextInt(20000), d))
    }
  }

  def toOrdinalWeek(num: Int, year: Int) = s"${num}${
    num % 10 match {
      case _ if Seq(11, 12, 13).contains(num) ⇒ "th"
      case 1 ⇒ "st"
      case 2 ⇒ "nd"
      case 3 ⇒ "rd"
      case _ ⇒ "th"
    }
  } Week, $year"

  def filterByOptionalDate(
    data: Seq[TimePeriodData],
    dateFrom: Option[LocalDate],
    dateTo: Option[LocalDate]): Seq[TimePeriodData] = {
    (dateFrom, dateTo) match {
      case (Some(f), Some(t)) ⇒
        data.filter(p ⇒ (p.timePeriod.isAfter(f) || p.timePeriod.isEqual(f)) && (p.timePeriod.isBefore(t) || p.timePeriod.isEqual(t)))
      case (Some(f), _) ⇒
        data.filter(p ⇒ (p.timePeriod.isAfter(f) || p.timePeriod.isEqual(f)))
      case (_, Some(t)) ⇒
        data.filter(p ⇒ (p.timePeriod.isBefore(t) || p.timePeriod.isEqual(t)))
      case _ ⇒ data
    }
  }
}

case class InstitutionFloats(
    id: UUID,
    name: String,
    distributionBalance: BigDecimal,
    parameterId: UUID,
    userBalancePercentage: BigDecimal,
    calculatedUserBalance: BigDecimal,
    pendingBalance: BigDecimal)

case class MockTrendData(
    amount: BigDecimal,
    timePeriodData: String)

case class RevenuePerTransactionType(
    p2p: Seq[TimePeriodData],
    remittance: Seq[TimePeriodData],
    exchange: Seq[TimePeriodData],
    splitBill: Seq[TimePeriodData])

case class InstitutionTxnTrends(
    cashin: Seq[TimePeriodData],
    cashout: Seq[TimePeriodData],
    transactions: Seq[TimePeriodData],
    closingUserBalance: Seq[TimePeriodData])
