package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty

case class RoleToUpdate(
                         @ApiModelProperty(name = "name", required = true) name: Option[String],
                         @ApiModelProperty(name = "level", required = true) level: Option[Int],
                       )
