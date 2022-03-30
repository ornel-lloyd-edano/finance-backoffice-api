package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.businessuserapplication.dto.BusinessUserApplicationExplanationToUpdateT

case class BusinessUserApplicationExplanationToUpdateSwagger(
    @ApiModelProperty(name = "explanation", required = true, example = "Reason for cancelling/rejecting application") explanation: String,
    @ApiModelProperty(name = "updated_at", required = true, example = "2020-02-04T06:35:47.182Z") lastUpdatedAt: Option[ZonedDateTime]) extends BusinessUserApplicationExplanationToUpdateT

