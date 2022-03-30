package tech.pegb.backoffice.api.businessuserapplication.dto

import io.swagger.annotations.ApiModelProperty

case class BusinessUserApplicationAddress(
    @ApiModelProperty(name = "address_type", required = true) addressType: String,
    @ApiModelProperty(name = "country", required = true) country: String,
    @ApiModelProperty(name = "city", required = true) city: String,
    @ApiModelProperty(name = "postal_code", required = false) postalCode: Option[String],
    @ApiModelProperty(name = "address", required = true) address: String,
    @ApiModelProperty(name = "coordinate_x", required = false) coordinateX: Option[BigDecimal],
    @ApiModelProperty(name = "coordinate_y", required = false) coordinateY: Option[BigDecimal])
