package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime

case class RoleToCreate(name: String, level: Int, createdBy: String, createdAt: LocalDateTime) {

}
