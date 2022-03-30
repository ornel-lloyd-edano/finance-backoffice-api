package tech.pegb.backoffice.api.recon.controllers

import java.time.LocalDate

import com.google.inject.ImplementedBy
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}

@ImplementedBy(classOf[impl.ReconciliationController])
trait ReconciliationController extends Routable {
  def getRoute: String = "internal_recons"

  def getReconciliationSummaryById(id: String): Action[AnyContent]
  def getInternalRecon(
    maybeId: Option[String],
    maybeUserId: Option[String],
    maybeAnyCustomerName: Option[String],
    maybeAccountNumber: Option[String],
    maybeAccountType: Option[String],
    maybeStatus: Option[String],
    maybeStartReconDate: Option[LocalDateTimeFrom],
    maybeEndReconDate: Option[LocalDateTimeTo],
    maybeOrderBy: Option[String],
    partialMatch: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Action[AnyContent]

  def getInternalReconIncidents(
    maybeReconId: Option[String],
    maybeAccountNumber: Option[String],
    maybeStartReconDate: Option[LocalDateTimeFrom],
    maybeEndReconDate: Option[LocalDateTimeTo],
    maybeOrderBy: Option[String],
    partialMatch: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Action[AnyContent]
  def externalRecon(
    thirdParty: String,
    source: Option[String],
    startDate: LocalDate,
    endDate: LocalDate): Action[AnyContent]

  def getTxnsForThirdPartyRecon(thirdParty: String, startDate: LocalDate, endDate: LocalDate): Action[AnyContent]

  def updateReconStatus(id: String): Action[String]
}
