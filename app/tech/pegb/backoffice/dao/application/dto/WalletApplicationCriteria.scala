package tech.pegb.backoffice.dao.application.dto

import java.time.LocalDate
import java.util.UUID

case class WalletApplicationCriteria(
    customerId: Option[UUID],
    msisdn: Option[String],
    name: Option[String],
    fullName: Option[String],
    nationalId: Option[String],
    status: Option[String],
    inactiveStatuses: Set[String],
    applicationStage: Option[String],
    checkedBy: Option[String],
    checkedAtStartingFrom: Option[LocalDate],
    checkedAtUpTo: Option[LocalDate],
    createdBy: Option[String],
    createdAtStartingFrom: Option[LocalDate],
    createdAtUpTo: Option[LocalDate])

object WalletApplicationCriteria {

  def getEmpty = WalletApplicationCriteria(
    customerId = None,
    msisdn = None,
    name = None,
    fullName = None,
    nationalId = None,
    status = None,
    inactiveStatuses = Set.empty,
    applicationStage = None,
    checkedBy = None,
    checkedAtStartingFrom = None,
    checkedAtUpTo = None,
    createdBy = None,
    createdAtStartingFrom = None,
    createdAtUpTo = None)
}
