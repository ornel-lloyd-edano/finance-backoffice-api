package tech.pegb.backoffice.mapping.domain.dao.auth.role

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.dao.auth.dto.{RoleCriteria, RoleToCreate, RoleToUpdate}
import tech.pegb.backoffice.dao.auth.sql.RoleSqlDao._
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.auth.dto.{RoleCriteria ⇒ RoleCriteriaDomain, RoleToCreate ⇒ RoleToCreateDomain, RoleToUpdate ⇒ RoleToUpdateDomain}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class RoleCreateAdapter(val roleToCreate: RoleToCreateDomain) extends AnyVal {

    def asDao: RoleToCreate = {

      RoleToCreate(
        id = UUID.randomUUID(),
        name = roleToCreate.name,
        isActive = 1,
        level = roleToCreate.level,
        createdBy = roleToCreate.createdBy,
        createdAt = roleToCreate.createdAt,
        updatedBy = roleToCreate.createdBy,
        updatedAt = roleToCreate.createdAt)
    }
  }

  implicit class RoleUpdateAdapter(val roleToUpdate: RoleToUpdateDomain) extends AnyVal {
    def asDao(isActive: Option[Boolean] = None, maybeMissingLastUpdatedAt: Option[LocalDateTime] = None): RoleToUpdate = {

      RoleToUpdate(
        name = roleToUpdate.name,
        level = roleToUpdate.level,
        updatedBy = roleToUpdate.updatedBy,
        updatedAt = roleToUpdate.updatedAt,
        isActive = isActive.map(_.toInt),
        lastUpdatedAt = roleToUpdate.lastUpdatedAt.orElse(maybeMissingLastUpdatedAt))
    }
  }

  implicit class RoleCriteriaAdapter(val criteria: RoleCriteriaDomain) extends AnyVal {
    def asDao(isActive: Option[Boolean] = None, butNotThisId: Option[UUID] = None): RoleCriteria = RoleCriteria(
      id = (criteria.id, butNotThisId) match {
        case (_, Some(notThisId)) ⇒
          CriteriaField(cId, notThisId.toString, MatchTypes.NotSame).toOption
        case (Some(id), _) ⇒
          CriteriaField(cId, id.toString, MatchTypes.Exact).toOption
        case _ ⇒ None
      },

      name = criteria.name.map(CriteriaField(cName, _)),
      level = criteria.level.map(CriteriaField(cLevel, _)),
      isActive = isActive.map(_.toInt).map(CriteriaField(cIsActive, _)))
  }

}
