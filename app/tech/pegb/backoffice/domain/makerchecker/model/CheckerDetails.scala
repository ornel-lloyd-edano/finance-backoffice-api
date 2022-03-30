package tech.pegb.backoffice.domain.makerchecker.model

import java.time.LocalDateTime

case class CheckerDetails(
    checkedBy: String,
    checkedAt: LocalDateTime,
    level: Option[RoleLevel] = None,
    businessUnit: Option[String] = None) {

}
