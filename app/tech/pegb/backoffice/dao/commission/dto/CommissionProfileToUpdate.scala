package tech.pegb.backoffice.dao.commission.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql

case class CommissionProfileToUpdate(
    deletedAt: Option[LocalDateTime] = None,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

}
