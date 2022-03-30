package tech.pegb.backoffice.api.aggregations.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.http.HttpVerbs
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.aggregations.controllers.Constants
import tech.pegb.backoffice.api.aggregations.dto._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.parameter.dto.{ParameterToRead, ParameterToUpdate}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.util.time.{Frequency, DateTimeRangeUtil}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class FloatController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    val httpClient: HttpClient,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with api.aggregations.controllers.FloatController
  with AmountAggregationUtil {

  import InstitutionUserBalancePercentage._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  val parameterEndpoint = s"${appConfig.Hosts.MainBackofficeApi}/parameters"
  val floatUserBalancePercentageParameterUrl = s"$parameterEndpoint?key=${appConfig.Aggregations.floatUserBalancePercentageKey}"

  def getTotalAggregations(currencyCode: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val sanitizedCurrency = currencyCode.sanitize
    val collectionF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.AccountBalance,
      currencyCode = sanitizedCurrency,
      accountType = Constants.AccType.Collection.some)
    val distributionF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.AccountBalance,
      currencyCode = sanitizedCurrency,
      accountType = Constants.AccType.Distribution.some)
    val endUserF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.AccountBalance,
      currencyCode = sanitizedCurrency,
      accountType = s"${Constants.AccType.UserBalanceSaving},${Constants.AccType.UserBalanceWallet}".some)

    (for {
      collection ← EitherT(collectionF)
      distribution ← EitherT(distributionF)
      endUser ← EitherT(endUserF)
    } yield {
      FloatTotals(
        institutionCollectionBalance = collection.headOption.map(_.amount).getOrElse(0),
        institutionDistributionBalance = distribution.headOption.map(_.amount).getOrElse(0),
        userBalance = endUser.headOption.map(_.amount).getOrElse(0),
        pendingBalance = BigDecimal(0)) //TODO: REMOVE MPESA MOCK PENDING
    }).fold(
      identity,
      floatTotals ⇒ handleApiResponse(Right(floatTotals.toJsonStr)))
  }

  def getInstitutionStats(currencyCode: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val sanitizedCurrency = currencyCode.sanitize
    val distributionF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.Balance,
      currencyCode = sanitizedCurrency,
      accountType = Constants.AccType.Distribution.some,
      groupBy = "institution".some)

    val endUserF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.AccountBalance,
      currencyCode = sanitizedCurrency,
      accountType = s"${Constants.AccType.UserBalanceSaving},${Constants.AccType.UserBalanceWallet}".some)

    val parameterF = sendHttpCall[Seq[ParameterToRead]](floatUserBalancePercentageParameterUrl, "results".some)

    (for {
      distribution ← EitherT(distributionF)
      endUser ← EitherT(endUserF)
      parameterSeq ← EitherT(parameterF)
      parameter ← EitherT.fromOption[Future](parameterSeq.headOption, BadRequest(s"System_setting parameter ${appConfig.Aggregations.floatUserBalancePercentageKey} missing"))
      validJson ← EitherT.fromEither[Future](Try(Json.parse(parameter.value.toString)).toEither.leftMap(_ ⇒ BadRequest(s"${parameter.value} is not a valid json")))
      valueString ← EitherT.fromEither[Future]((validJson \ "value").validate[String].asEither.leftMap(error ⇒ {
        logger.error(s"error while parsing response ${validJson} [value field]-> $error")
        BadRequest(malformedPaginatedAmountAggregationResponse(floatUserBalancePercentageParameterUrl))
      }))
      valueJson ← EitherT.fromEither[Future](Try(Json.parse(valueString.toString)).toEither.leftMap(_ ⇒ BadRequest(s"""${valueString} is not a valid json (format expected) [{"name":"someInstitution","percentage":100}]""")))
      parameterValue ← EitherT.fromEither[Future](valueJson.validate[Seq[InstitutionUserBalancePercentage]].asEither
        .leftMap(error ⇒ {
          logger.error(s"error while parsing response ${valueJson} -> $error")
          BadRequest(malformedPaginatedAmountAggregationResponse(floatUserBalancePercentageParameterUrl))
        }))
    } yield {
      val lookUp = parameterValue.map(p ⇒ (p.name → p.percentage)).toMap
      distribution.map { d ⇒
        val name = d.institution.getOrElse("UNKNOWN")
        val percentage = lookUp.get(name).getOrElse(BigDecimal(100))
        InstitutionFloatSummary(
          name = name,
          distributionAccountBalance = d.amount,
          institutionUserBalancePercentage = percentage,
          calculatedUserBalance = endUser.headOption.map(_.amount).getOrElse(BigDecimal(0)) * percentage / 100,
          pendingBalance = if (name == "mPesa") BigDecimal(14700) else BigDecimal(0)) //TODO: REMOVE MPESA MOCK PENDING
      }
    }).fold(
      identity,
      institutionFloatTotals ⇒ handleApiResponse(Right(institutionFloatTotals.toJsonStr)))
  }

  def getInstitutionTrendsGraph(
    institution: String,
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    frequency: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val sanitizedCurrency = currencyCode.sanitize
    val sanitizedInstitution = institution.sanitize
    val sanitizedFreq = frequency.sanitize
    val cashinF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.Amount,
      currencyCode = sanitizedCurrency,
      dateFrom = dateFrom,
      dateTo = dateTo,
      institution = sanitizedInstitution.some,
      transactionType = Constants.TxnType.CashIn.some,
      userType = "provider".some,
      groupBy = "institution,transaction_type,time_period".some,
      frequency = sanitizedFreq.some)

    val cashoutF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.Amount,
      currencyCode = sanitizedCurrency,
      dateFrom = dateFrom,
      dateTo = dateTo,
      institution = sanitizedInstitution.some,
      transactionType = Constants.TxnType.CashOut.some,
      userType = "provider".some,
      groupBy = "institution,transaction_type,time_period".some,
      frequency = sanitizedFreq.some)

    val etcTxnF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.Amount,
      currencyCode = sanitizedCurrency,
      dateFrom = dateFrom,
      dateTo = dateTo,
      institution = sanitizedInstitution.some,
      transactionType = Constants.TxnType.EtcTxns.some,
      userType = "provider".some,
      groupBy = "institution,time_period".some,
      frequency = sanitizedFreq.some)

    val closingUserBalanceF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.Balance,
      currencyCode = sanitizedCurrency,
      dateFrom = dateFrom,
      dateTo = dateTo,
      institution = sanitizedInstitution.some,
      transactionType = None,
      userType = appConfig.Aggregations.closingUserBalanceUserType.some,
      groupBy = "institution,time_period".some,
      frequency = sanitizedFreq.some)

    val dateRange = DateTimeRangeUtil.createDateRange(dateFrom.map(_.localDateTime.toLocalDate), dateTo.map(_.localDateTime.toLocalDate), Frequency.fromString(frequency))

    (for {
      cashin ← EitherT(cashinF)
      cashout ← EitherT(cashoutF)
      etcTxn ← EitherT(etcTxnF)
      closing ← EitherT(closingUserBalanceF)
    } yield {
      InstitutionTrendGraph(
        cashIn = fillZero(cashin, dateRange),
        transactions = fillZero(etcTxn, dateRange),
        cashOut = fillZero(cashout, dateRange),
        closingUserBalance = fillZero(closing, dateRange))
    }).fold(
      identity,
      floatTotals ⇒ handleApiResponse(Right(floatTotals.toJsonStr)))
  }

  def updatePercentage(institution: String): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val sanitizedInstitution = institution.sanitize
    val parameterF = sendHttpCall[Seq[ParameterToRead]](floatUserBalancePercentageParameterUrl, "results".some)

    (for {
      percentageToUpdateApiDto ← EitherT.fromEither[Future](ctx.body.as(
        classOf[UserBalancePercentageToUpdate],
        isDeserializationStrict).toEither.leftMap(error ⇒ {
        logger.error("Error encounter in deserializing request [updatePercentage]", error)
        BadRequest(s"Invalid request, responding with 400(BadRequest)")
      }))

      parameterSeq ← EitherT(parameterF)
      parameter ← EitherT.fromOption[Future](parameterSeq.headOption, InternalServerError(s"System_setting parameter ${appConfig.Aggregations.floatUserBalancePercentageKey} missing"))

      parentValueJson ← EitherT.fromEither[Future](Try(Json.parse(parameter.value.toString)).toEither.leftMap(_ ⇒ InternalServerError(s"${parameter.value} is not a valid json")))
      valueString ← EitherT.fromEither[Future]((parentValueJson \ "value").validate[String].asEither.leftMap(error ⇒ {
        logger.error(s"error while parsing response ${parentValueJson} [value field]-> $error")
        InternalServerError(malformedPaginatedAmountAggregationResponse(floatUserBalancePercentageParameterUrl))
      }))
      valueJson ← EitherT.fromEither[Future](Try(Json.parse(valueString.toString)).toEither.leftMap(_ ⇒ InternalServerError(s"""${valueString} is not a valid json (format expected) [{"name":"someInstitution","percentage":100}]""")))
      parameterValue ← EitherT.fromEither[Future](valueJson.validate[Seq[InstitutionUserBalancePercentage]].asEither
        .leftMap(error ⇒ {
          logger.error(s"error while parsing response ${valueJson} -> $error")
          InternalServerError(malformedPaginatedAmountAggregationResponse(floatUserBalancePercentageParameterUrl))
        }))

      updatedValue ← EitherT.fromEither[Future] {
        val newList = InstitutionUserBalancePercentage(name = sanitizedInstitution, percentage = percentageToUpdateApiDto.percentage) +:
          parameterValue.filterNot(_.name == sanitizedInstitution)
        Try(parentValueJson.as[JsObject] + ("value" → JsString(newList.toJsonStr)))
          .toEither.leftMap(_ ⇒ InternalServerError(s"${parameter.value} is not a valid json"))
      }

      parameterToUpdate = ParameterToUpdate(
        value = updatedValue,
        explanation = parameter.explanation,
        metadataId = parameter.metadataId.some,
        platforms = parameter.platforms.some,
        updatedAt = parameter.updatedAt)

      _ ← EitherT(httpClient.request(HttpVerbs.PUT, s"$parameterEndpoint/${parameter.id}", Json.toJson(parameterToUpdate).some).map { resp ⇒
        if (resp.success) {
          Right(())
        } else {
          Status(resp.statusCode)(resp.body.getOrElse(s"Missing Error body from PUT $parameterEndpoint/${parameter.id}"))
            .asLeft[Unit]
        }
      })

    } yield {
      UserBalancePercentageToRead(
        institution = sanitizedInstitution,
        userBalance = percentageToUpdateApiDto.userBalance * percentageToUpdateApiDto.percentage / 100,
        percentage = percentageToUpdateApiDto.percentage)
    }).fold(
      identity,
      floatTotals ⇒ handleApiResponse(Right(floatTotals.toJsonStr)))
  }

  private def fillZero(amountAggregation: Seq[AmountAggregation], dateRange: Seq[String]): Seq[TimePeriodData] = {
    if (amountAggregation.isEmpty) {
      Nil
    } else {
      val lookUp = amountAggregation.collect {
        case (v) if v.timePeriod.isDefined ⇒ v.timePeriod.get → v.amount
      }.toMap.withDefaultValue(BigDecimal(0))

      dateRange.map(d ⇒
        TimePeriodData(
          timePeriod = d,
          amount = lookUp(d)))
    }
  }

}

object FloatController {
  val MalformedUpdateScopeErrorMsg = "Malformed request to update a scope. Mandatory field is missing or value of a field is of wrong type."
}
