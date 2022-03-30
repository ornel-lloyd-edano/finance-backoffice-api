package tech.pegb.backoffice.domain.businessuserapplication.dto

import java.time.LocalDateTime

case class BusinessUserApplicationToUpdateStatus(
    status: String,
    explanation: Option[String] = None,
    submittedBy: Option[String] = None,
    submittedAt: Option[LocalDateTime] = None,
    checkedBy: Option[String] = None,
    checkedAt: Option[LocalDateTime] = None,
    updatedBy: String,
    updatedAt: LocalDateTime) {

}
