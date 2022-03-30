package tech.pegb.backoffice.mapping.api.domain.parameter

import java.time.ZonedDateTime
import java.util.UUID

import tech.pegb.backoffice.api.parameter.dto.{ParameterToCreate, ParameterToUpdate}
import tech.pegb.backoffice.domain.parameter.dto.{ParameterCriteria, ParameterToCreate ⇒ DomainParameterToCreate, ParameterToUpdate ⇒ DomainParameterToUpdate}
import tech.pegb.backoffice.domain.parameter.model.Platforms._
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

object Implicits {

  implicit class ParameterGetByIdApiAdapter(val arg: UUID) extends AnyVal {
    def asDomain = ParameterCriteria(id = Some(arg))
  }

  implicit class ParameterCriteriaApiAdapter(val arg: (Option[String], Option[String], Option[String])) extends AnyVal {
    def asDomain = ParameterCriteria(
      key = arg._1.map(_.sanitize),
      metadataId = arg._2.map(_.sanitize),
      platforms = arg._3.map(p ⇒ p.toSeqByComma.map(_.sanitize.asDomain)))
  }

  implicit class ParameterToCreateApiAdapter(val arg: ParameterToCreate) extends AnyVal {
    def asDomain(createdAt: ZonedDateTime, createdBy: String) = Try(DomainParameterToCreate(
      key = arg.key.sanitize,
      value = arg.value,
      explanation = arg.explanation.map(_.sanitize),
      metadataId = arg.metadataId.sanitize,
      platforms = arg.platforms.map(_.sanitize.asDomain),
      createdAt = createdAt.toLocalDateTimeUTC,
      createdBy = createdBy))
  }

  implicit class ParameterToUpdateApiAdapter(val arg: ParameterToUpdate) extends AnyVal {
    def asDomain(updatedAt: ZonedDateTime, updatedBy: String): Try[DomainParameterToUpdate] = {
      Try(DomainParameterToUpdate(
        value = arg.value,
        explanation = arg.explanation.map(_.sanitize),
        platforms = arg.platforms.map(_.map(_.sanitize.asDomain)),
        updatedAt = updatedAt.toLocalDateTimeUTC,
        updatedBy = updatedBy,
        lastUpdatedAt = arg.updatedAt.map(_.toLocalDateTimeUTC)))
    }
  }
}
