package tech.pegb.backoffice.dao.application.dto

import java.time.LocalDateTime

case class WalletApplicationToUpdate(
    status: Option[String],
    msisdn: Option[String] = None,
    applicationStage: Option[String] = None,
    checkedBy: Option[String] = None,
    checkedAt: Option[LocalDateTime] = None,
    rejectionReason: Option[String] = None,
    createdAt: Option[LocalDateTime] = None,
    createdBy: Option[String] = None,
    updatedAt: LocalDateTime,
    updatedBy: String)
