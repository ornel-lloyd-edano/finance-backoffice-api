package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.makerchecker.dto.{WithOptionalReason}

case class ApproveTaskRequest(
    @ApiModelProperty(name = "maybe_reason", example = "possible reason why not rejected", required = false) maybeReason: Option[String]) extends WithOptionalReason {

}
