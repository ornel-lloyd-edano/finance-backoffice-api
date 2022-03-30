package tech.pegb.backoffice.api.swagger.model

import java.util.UUID

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "AccountToCreate")
case class AccountToCreate(
    @ApiModelProperty(name = "customer_id", required = true) customerId: UUID,
    @ApiModelProperty(name = "type", required = true) `type`: String,
    @ApiModelProperty(name = "currency", required = true) currency: String)
