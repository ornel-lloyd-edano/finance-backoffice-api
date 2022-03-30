package tech.pegb.backoffice.domain.makerchecker.model

import java.time.LocalDateTime
import tech.pegb.backoffice.util.Implicits._

case class MakerDetails(
    createdBy: String,
    createdAt: LocalDateTime,
    level: RoleLevel,
    businessUnit: String) {

  assert(createdBy.hasSomething, "Task maker cannot be empty")
  assert(businessUnit.hasSomething, "Department of the task maker cannot be empty")
}
