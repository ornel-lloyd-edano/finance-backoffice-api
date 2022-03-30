package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty

case class RoleToCreate(
    @ApiModelProperty(name = "name", required = true) name: String,
    @ApiModelProperty(name = "level", required = true) level: Int)

