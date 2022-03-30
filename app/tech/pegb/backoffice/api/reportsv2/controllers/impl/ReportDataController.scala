package tech.pegb.backoffice.api.reportsv2.controllers.impl

import java.util.UUID
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import cats.implicits._
import com.google.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.aggregations.dto.CashFlowReport
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo, PaginatedResult}
import tech.pegb.backoffice.api.reportsv2.controllers
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.aggregations.controllers.CashFlowController
import tech.pegb.backoffice.domain.report.abstraction.{CashFlowReportService, ReportManagement}
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class ReportDataController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    reportDefinitionManagement: ReportManagement,
    cashFlowReportService: CashFlowReportService,
    cashFlowController: CashFlowController,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with controllers.ReportDataController {
  import ApiController._
  import PaginatedResult.paginatedResultWrites //Intellij reports this as not used but dont remove

  implicit val executionContext: ExecutionContext = executionContexts.genericOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  def getReportData(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    val queryParams: Map[String, String] = ctx.queryString.map {
      case (k, v) ⇒ k → v.mkString
    }

    val parseLocalDateTime = (key: String, isStartOfDay: Boolean) ⇒ {
      val value = queryParams.get(key)
      (value.map(_.length), isStartOfDay) match {
        case (Some(10), true) ⇒
          queryParams.get(key).map(LocalDate.parse(_, DateTimeFormatter.ISO_DATE).atStartOfDay())
        case (Some(10), false) ⇒
          queryParams.get(key).map(LocalDate.parse(_, DateTimeFormatter.ISO_DATE).atEndOfDay)
        case _ ⇒
          queryParams.get(key).map(LocalDateTime.parse(_, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      }
    }

    Try {
      //Note: it seems redundant why we need to wrap again to LocalDateTimeFrom/To but remember we giving this arguments to a controller method
      val dateFrom = parseLocalDateTime("date_from", true).map(LocalDateTimeFrom(_))
      val dateTo = parseLocalDateTime("date_to", false).map(LocalDateTimeTo(_))
      val limit = queryParams.get("limit").map(_.toInt)
      val offset = queryParams.get("offset").map(_.toInt)

      (dateFrom, dateTo, limit, offset)
    }.fold(
      err ⇒ {
        logger.error(s"[getReportData] Error in extracting query params for get report. Reason: ${err.getMessage}", err)
        handleApiResponse("Error in extracting query params for get report".asMalformedRequestApiError.toLeft[String]).toFuture
      },
      {
        case (dateFrom, dateTo, limit, offset) ⇒
          id match {
            case cashFlowController.fixedReportId ⇒
              val currency = queryParams.get("currency")
              val institution = queryParams.get("institution")
              cashFlowController.getCashFlowReport(dateFrom, dateTo, currency, institution, None, limit, offset).apply(ctx)

            case _ ⇒
              reportDefinitionManagement.getReportData(id, queryParams).map(serviceResponse ⇒
                serviceResponse.map(report ⇒ PaginatedResult(report.count, report.result, None, None))
                  .leftMap(_.asApiError())).map(handleApiResponse(_))

          }
        case _ ⇒
          handleApiResponse("Unexpected error in matching extracted query params for get report".asUnknownApiError.toLeft[String]).toFuture
      })

  }

  //TODO for cashflow mock api only
  private def getCashflowDataFromMock(
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    currencyCode: Option[String],
    institution: Option[String],
    limit: Option[Int],
    offset: Option[Int]): (Seq[CashFlowReport], Int) = {

    import tech.pegb.backoffice.api.mock.MockDataReader._
    val result = readCashFlowReportDataFromInMemCsv()
    val currencyFiltered = currencyCode.fold(result)(key ⇒ result.filter(_.currency.toUpperCase == key.toUpperCase))
    val institutionFiltered = institution.fold(currencyFiltered)(key ⇒ currencyFiltered.filter(_.institution.toUpperCase == key.toUpperCase))
    val dateFilter = institutionFiltered.filter { d ⇒
      dateFrom.fold(true)(dFrom ⇒
        d.date >= dateFormatter(dFrom.localDateTime)) && dateTo.fold(true)(dTo ⇒ d.date <= dateFormatter(dTo.localDateTime))
    }

    val resultOrdering = dateFilter.sortWith((a, b) ⇒ Ordering[String].lt(a.date, b.date)).reverse

    ((limit, offset) match {
      case (Some(lim), Some(off)) ⇒
        resultOrdering.drop(off).take(lim)
      case (Some(lim), None) ⇒
        resultOrdering.take(lim)
      case (None, Some(off)) ⇒
        resultOrdering.drop(off).take(Int.MaxValue)
      case _ ⇒
        resultOrdering
    }, resultOrdering.size)

  }

  private def dateFormatter(date: LocalDateTime): String = {
    val d = date.toLocalDate
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    d.format(formatter)
  }

}
