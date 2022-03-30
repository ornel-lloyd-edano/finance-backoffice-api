package tech.pegb.backoffice.mapping.api.domain.auth.role

import java.time.{ZonedDateTime}
import java.util.UUID

import tech.pegb.backoffice.api.auth.dto.{RoleToCreate ⇒ RoleToCreateApi, RoleToUpdate ⇒ RoleToUpdateApi}
import tech.pegb.backoffice.domain.auth.dto.{RoleCriteria, RoleToCreate, RoleToUpdate}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class RoleCreateApiAdaptor(roleToCreate: RoleToCreateApi) {
    def asDomain(createdAt: ZonedDateTime, createdBy: String): RoleToCreate = {
      RoleToCreate(
        name = roleToCreate.name.sanitize,
        level = roleToCreate.level,
        createdAt = createdAt.toLocalDateTimeUTC,
        createdBy = createdBy.sanitize)
    }
  }

  implicit class RoleUpdateApiAdaptor(roleToUpdate: RoleToUpdateApi) {
    def asDomain(updatedAt: ZonedDateTime, updatedBy: String): RoleToUpdate = {

      RoleToUpdate(
        name = roleToUpdate.name.map(_.sanitize),
        level = roleToUpdate.level,
        updatedAt = updatedAt.toLocalDateTimeUTC,
        updatedBy = updatedBy.sanitize,
        lastUpdatedAt = roleToUpdate.lastUpdatedAt.map(_.toLocalDateTimeUTC))
    }
  }

  implicit class RoleCriteriaApiAdaptor(arg: (Option[UUID], Option[String], Option[Int])) {
    def asDomain: RoleCriteria = {
      RoleCriteria(
        id = arg._1,
        name = arg._2.map(_.sanitize),
        level = arg._3)
    }
  }

}
