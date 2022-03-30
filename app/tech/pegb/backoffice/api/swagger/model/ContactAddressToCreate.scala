package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty

case class ContactAddressToCreate(
    @ApiModelProperty(name = "address_type", required = true, example = "primary_address") addressType: String,
    @ApiModelProperty(name = "country", required = true, example = "Philippines") country: String,
    @ApiModelProperty(name = "city", required = true, example = "Makati City") city: String,
    @ApiModelProperty(name = "postal_code", required = true, example = "1229") postalCode: Option[String],
    @ApiModelProperty(name = "address", required = true, example = "The Residences, Legaspi Village, Makati city, Philippines") address: Option[String],
    @ApiModelProperty(name = "coordinate_x", required = true, example = "14.551250") coordinateX: Option[BigDecimal],
    @ApiModelProperty(name = "coordinate_y", required = true, example = "121.020090") coordinateY: Option[BigDecimal])
