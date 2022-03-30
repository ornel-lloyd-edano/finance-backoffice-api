package tech.pegb.backoffice.mapping.api.domain.report

import java.time.{LocalDate, ZonedDateTime}

import tech.pegb.backoffice.api.reportsv2.dto.{ReportDefinitionToCreate, ReportDefinitionToUpdate}
import tech.pegb.backoffice.domain.report.dto
import tech.pegb.backoffice.domain.report.dto.{CashFlowReportCriteria}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.api.aggregations.controllers.Constants

object Implicits {
  implicit class ReportDefinitionCreateAdapter(val arg: ReportDefinitionToCreate) extends AnyVal {
    def asDomain(createdBy: String, createdAt: ZonedDateTime): dto.ReportDefinitionToCreate = {
      dto.ReportDefinitionToCreate(
        name = arg.name,
        title = arg.title,
        description = arg.description,
        columns = arg.columns,
        parameters = arg.parameters,
        joins = arg.joins,
        grouping = arg.grouping,
        ordering = arg.ordering,
        paginated = arg.paginated,
        sql = arg.sql,
        createdBy = createdBy,
        createdAt = createdAt.toLocalDateTimeUTC)
    }
  }

  implicit class ReportDefinitionUpdateAdapter(val arg: ReportDefinitionToUpdate) extends AnyVal {
    def asDomain(updatedBy: String, updatedAt: ZonedDateTime): dto.ReportDefinitionToUpdate = {
      dto.ReportDefinitionToUpdate(
        title = arg.title,
        description = arg.description,
        columns = arg.columns,
        parameters = arg.parameters,
        joins = arg.joins,
        grouping = arg.grouping,
        ordering = arg.ordering,
        paginated = arg.paginated,
        sql = arg.sql,
        updatedBy = updatedBy,
        updatedAt = updatedAt.toLocalDateTimeUTC)
    }
  }

  private type DateFrom = Option[LocalDate]
  private type DateTo = Option[LocalDate]
  private type Currency = String
  private type Currencies = Option[String]
  private type Institutions = Option[String]
  private type UserType = Option[String]
  private type txnOtherParty = Option[String]
  private type primaryAccNumber = Option[String]

  implicit class CashFlowReportCriteriaAdapter(val arg: (DateFrom, DateTo, Currencies, Institutions)) extends AnyVal {
    def asDomain = CashFlowReportCriteria(
      startDate = arg._1,
      endDate = arg._2,
      onlyForTheseCurrencies = arg._3.map(_.split(",").map(_.trim.sanitize).filter(_.nonEmpty).toSeq).getOrElse(Nil),
      onlyForTheseProviders = arg._4.map(_.split(",").map(_.trim.sanitize).filter(_.nonEmpty).toSeq).getOrElse(Nil),
      userType = "provider".toOption, //TODO use constant and store in customer domain
      txnOtherParty = "".toOption,
      notThisPrimaryAccNumber = Constants.AccountNumber.FilterNotPegbFees.toOption)
  }
}
