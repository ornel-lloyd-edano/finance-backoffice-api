package tech.pegb.backoffice.mapping.domain.dao.auth.scope

import java.time.LocalDateTime

import cats.implicits._
import tech.pegb.backoffice.dao.auth.dto
import tech.pegb.backoffice.dao.auth.dto.ScopeToInsert
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.auth.dto.{ScopeCriteria, ScopeToCreate, ScopeToUpdate}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class ScopeToCreateAdapter(val arg: ScopeToCreate) extends AnyVal {
    def asDao = ScopeToInsert(
      name = arg.name.toString,
      parentId = arg.parentId.map(_.toString),
      description = arg.description,
      isActive = 1,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt)

    def asReactivateDao(lastUpdatedAt: Option[LocalDateTime]) = dto.ScopeToUpdate(
      description = arg.description,
      isActive = 1.some,
      updatedBy = arg.createdBy,
      updatedAt = arg.createdAt,
      lastUpdatedAt = lastUpdatedAt)
  }

  implicit class ScopeCriteriaAdapter(val arg: ScopeCriteria) extends AnyVal {
    def asDao(isActive: Boolean = true) = dto.ScopeCriteria(
      id = arg.id.map(x ⇒ CriteriaField("id", x.underlying,
        if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact)),
      parentId = arg.parentId.map(x ⇒ CriteriaField("parent_id", x.underlying,
        if (arg.partialMatchFields.contains("parent_id")) MatchTypes.Partial else MatchTypes.Exact)),
      name = arg.name.map(x ⇒ CriteriaField("name", x,
        if (arg.partialMatchFields.contains("name")) MatchTypes.Partial else MatchTypes.Exact)),
      description = arg.name.map(x ⇒ CriteriaField("description", x,
        if (arg.partialMatchFields.contains("description")) MatchTypes.Partial else MatchTypes.Exact)),
      isActive = CriteriaField("is_active", isActive.toInt).some)
  }

  implicit class ScopeUpdateDtoAdapter(val arg: ScopeToUpdate) extends AnyVal {
    def asDao(isActive: Option[Boolean] = None, maybeMissingLastUpdatedAt: Option[LocalDateTime] = None) = dto.ScopeToUpdate(
      description = arg.description,
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt,
      lastUpdatedAt = arg.lastUpdatedAt.orElse(maybeMissingLastUpdatedAt),
      isActive = isActive.map(_.toInt))
  }
}
