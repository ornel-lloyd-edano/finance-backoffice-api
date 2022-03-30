package tech.pegb.backoffice.dao.types

import java.time.LocalDateTime

case class TypesToInsert(
    newKind: String,
    createdAt: LocalDateTime,
    createdBy: String,
    newValues: Seq[String])
