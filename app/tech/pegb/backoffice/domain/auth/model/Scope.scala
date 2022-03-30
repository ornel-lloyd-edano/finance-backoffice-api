package tech.pegb.backoffice.domain.auth.model

import java.time.LocalDateTime
import java.util.UUID

case class Scope(
    id: UUID,
    parentId: Option[UUID],
    name: String,
    description: Option[String] = None,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {
  assert(!parentId.contains(id))
}
