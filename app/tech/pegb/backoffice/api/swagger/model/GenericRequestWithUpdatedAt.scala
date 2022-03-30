package tech.pegb.backoffice.api.swagger.model

import java.time.LocalDateTime

import io.swagger.annotations.ApiModelProperty

case class GenericRequestWithUpdatedAt(
    @ApiModelProperty(name = "updated_at", required = true) lastUpdatedAt: Option[LocalDateTime])
