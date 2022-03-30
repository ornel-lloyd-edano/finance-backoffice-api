package tech.pegb.backoffice.dao.makerchecker.entity

import java.time.LocalDateTime

case class MakerCheckerTask(
    id: Int,
    uuid: String,
    module: String,
    action: String,
    verb: String,
    url: String,
    headers: String,
    body: Option[String],
    valueToUpdate: Option[String] = None,
    status: String,
    createdBy: String,
    createdAt: LocalDateTime,
    makerLevel: Int,
    makerBusinessUnit: String,
    checkedBy: Option[String],
    checkedAt: Option[LocalDateTime],
    reason: Option[String],
    updatedAt: Option[LocalDateTime]) {

}
