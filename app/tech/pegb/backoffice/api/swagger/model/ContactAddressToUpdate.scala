package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty

case class ContactAddressToUpdate(
    @ApiModelProperty(name = "address_type", required = true, example = "primary_address") addressType: Option[String] = None,
    @ApiModelProperty(name = "country", required = true, example = "Philippines") country: Option[String] = None,
    @ApiModelProperty(name = "city", required = true, example = "Makati City") city: Option[String] = None,
    @ApiModelProperty(name = "postal_code", required = true, example = "1229") postalCode: Option[String] = None,
    @ApiModelProperty(name = "address", required = true, example = "The Residences, Legaspi Village, Makati city, Philippines") address: Option[String] = None,
    @ApiModelProperty(name = "coordinate_x", required = true, example = "14.551250") coordinateY: Option[BigDecimal] = None,
    @ApiModelProperty(name = "coordinate_y", required = true, example = "121.020090") coordinateX: Option[BigDecimal] = None,
    @ApiModelProperty(name = "updated_at", required = true) lastUpdatedAt: Option[ZonedDateTime])

