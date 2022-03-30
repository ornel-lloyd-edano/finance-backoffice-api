package tech.pegb.backoffice.api.customer.dto

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class ContactAddressToRead(
    @ApiModelProperty(name = "id", required = true, example = "da2a3868-4bbe-4bdc-adde-8e8e61bef2df") id: UUID,
    @ApiModelProperty(name = "address_type", required = true, example = "Primary Address") addressType: String,
    @ApiModelProperty(name = "country_name", required = true, example = "Philippines") countryName: String,
    @ApiModelProperty(name = "city", required = true, example = "Tagaytay") city: String,
    @ApiModelProperty(name = "postal_code", required = true, example = "4120") postalCode: Option[String],
    @ApiModelProperty(name = "address", required = true, example = "Tagaytay City, Cavite") address: Option[String],
    @ApiModelProperty(name = "coordinate_x", required = true) coordinateX: Option[BigDecimal],
    @ApiModelProperty(name = "coordinate_y", required = true) coordinateY: Option[BigDecimal],
    @ApiModelProperty(name = "created_by", required = true, example = "pegbuser") createdBy: String,
    @ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "updated_by", required = true, example = "pegbuser") updatedBy: Option[String],
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "is_active", required = true) isActive: Boolean)
