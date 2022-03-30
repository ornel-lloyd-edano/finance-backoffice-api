package tech.pegb.backoffice.domain.businessuserapplication.dto

import java.time.LocalDateTime

case class BusinessUserApplicationToUpdateStage(
    stage: String,
    updatedBy: String,
    updatedAt: LocalDateTime) {

}
