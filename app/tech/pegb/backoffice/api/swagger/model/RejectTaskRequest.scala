package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.makerchecker.dto.{WithMandatoryReason}

case class RejectTaskRequest(
    @ApiModelProperty(name = "reason", example = "operation is not allowed", required = true) reason: String) extends WithMandatoryReason {

}
