package tech.pegb.backoffice.dao.fee.dto

import java.time.LocalDateTime

class FeeProfileHistoryToInsert(
    feeProfileId: Int,
    oldFeeProfileFieldsJson: String,
    newFeeProfileFieldsJson: String,
    reason: Option[String],
    updatedBy: String,
    updatedAt: LocalDateTime) {

}
