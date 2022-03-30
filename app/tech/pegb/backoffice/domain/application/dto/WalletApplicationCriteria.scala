package tech.pegb.backoffice.domain.application.dto

import java.time.LocalDate
import java.util.UUID

import tech.pegb.backoffice.domain.application.model.ApplicationStatus
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn

case class WalletApplicationCriteria(
    customerId: Option[UUID],
    msisdn: Option[Msisdn],
    name: Option[String],
    fullName: Option[String],
    nationalId: Option[String],
    status: Option[ApplicationStatus],
    applicationStage: Option[String],
    checkedBy: Option[String],
    checkedAtStartingFrom: Option[LocalDate],
    checkedAtUpTo: Option[LocalDate],
    createdBy: Option[String],
    createdAtStartingFrom: Option[LocalDate],
    createdAtUpTo: Option[LocalDate])

object WalletApplicationCriteria {
  def getEmpty =
    WalletApplicationCriteria(
      customerId = None,
      msisdn = None,
      name = None,
      fullName = None,
      nationalId = None,
      status = None,
      applicationStage = None,
      checkedBy = None,
      checkedAtStartingFrom = None,
      checkedAtUpTo = None,
      createdBy = None,
      createdAtStartingFrom = None,
      createdAtUpTo = None)
}
