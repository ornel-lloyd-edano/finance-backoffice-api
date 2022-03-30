package tech.pegb.backoffice.mapping.api.domain.application

import java.time.{LocalDate}

import tech.pegb.backoffice.domain.application.dto.WalletApplicationCriteria
import tech.pegb.backoffice.domain.application.model.ApplicationStatus
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

object Implicits {

  private type Status = Option[String]
  private type Name = Option[String]
  private type FullName = Option[String]
  private type Msisdn = Option[String]
  private type NationalId = Option[String]
  private type StartDate = Option[LocalDate]
  private type EndDate = Option[LocalDate]

  implicit class WalletApplicationCriteriaDomainAdapter(val arg: (Status, Name, FullName, Msisdn, NationalId, StartDate, EndDate)) extends AnyVal {
    def asDomain = Try(WalletApplicationCriteria.getEmpty.copy(
      status = arg._1.map(s ⇒ ApplicationStatus(s.sanitize)),
      name = arg._2.map(_.sanitize),
      fullName = arg._3.map(_.sanitize),
      msisdn = arg._4.map(Msisdn),
      nationalId = arg._5.map(_.sanitize),
      createdAtStartingFrom = arg._6,
      createdAtUpTo = arg._7))
  }

}
