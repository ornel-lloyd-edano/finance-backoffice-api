package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty

case class BusinessUnitToCreateOrUpdate(@ApiModelProperty(name = "name", required = true) name: String)
