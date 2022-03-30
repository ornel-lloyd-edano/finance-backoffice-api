package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "CustomerAccountToCreate")
case class CustomerAccountToCreate(
    @ApiModelProperty(name = "type", required = true) `type`: String,
    @ApiModelProperty(name = "currency", required = true) currency: String)
