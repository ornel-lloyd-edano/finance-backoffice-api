package tech.pegb.backoffice.mapping.api.domain.auth.businessunit

import java.time.ZonedDateTime
import java.util.UUID

import tech.pegb.backoffice.api.auth.dto.{BusinessUnitToCreate, BusinessUnitToUpdate}
import tech.pegb.backoffice.domain.auth.dto.{BusinessUnitCriteria, BusinessUnitToRemove, BusinessUnitToCreate ⇒ DomainBusinessUnitToCreate, BusinessUnitToUpdate ⇒ DomainBusinessUnitToUpdate}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class BusinessUnitToCreateDomainAdapter(val arg: BusinessUnitToCreate) extends AnyVal {
    def asDomain(doneBy: String, doneAt: ZonedDateTime) = DomainBusinessUnitToCreate(
      name = arg.name.sanitize,
      createdBy = doneBy.sanitize,
      createdAt = doneAt.toLocalDateTimeUTC)
  }

  implicit class BusinessUnitToUpdateDomainAdapter(val arg: BusinessUnitToUpdate) extends AnyVal {
    def asDomain(doneBy: String, doneAt: ZonedDateTime) = DomainBusinessUnitToUpdate(
      name = Some(arg.name.sanitize),
      updatedBy = doneBy.sanitize,
      updatedAt = doneAt.toLocalDateTimeUTC,
      lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
  }

  private type BuId = Option[UUID]
  private type Name = Option[String]
  implicit class BusinessUnitQueryParamToCriteriaAdapter(val arg: (BuId, Name)) extends AnyVal {
    def asDomain = BusinessUnitCriteria(id = arg._1, name = arg._2.map(_.sanitize))
  }

  private type RemovedBy = String
  private type RemovedAt = ZonedDateTime
  private type LastUpdatedAt = Option[ZonedDateTime]
  implicit class BusinessUnitToRemoveAdapter(val arg: (RemovedBy, RemovedAt, LastUpdatedAt)) extends AnyVal {
    def asDomain = BusinessUnitToRemove(
      removedBy = arg._1.sanitize,
      removedAt = arg._2.toLocalDateTimeUTC,
      lastUpdatedAt = arg._3.map(_.toLocalDateTimeUTC))
  }

}
