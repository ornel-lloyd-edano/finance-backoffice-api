package tech.pegb.backoffice.api.swagger.model

import java.time.LocalDate

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.businessuserapplication.dto.BusinessUserApplicationToCreateT

case class BusinessUserApplicationToCreateSwagger(
    @ApiModelProperty(name = "business_name", required = true, example = "Universal Catering Co") businessName: String,
    @ApiModelProperty(name = "brand_name", required = true, example = "Costa Coffee DSO") brandName: String,
    @ApiModelProperty(name = "business_category", required = true, example = "Restaurants - 5812") businessCategory: String,
    @ApiModelProperty(name = "user_tier", required = true, example = "Basic") userTier: String,
    @ApiModelProperty(name = "business_type", required = true, example = "Merchant") businessType: String,
    @ApiModelProperty(name = "registration_number", required = true, example = "213/564654EE") registrationNumber: String,
    @ApiModelProperty(name = "tax_number", required = true, example = "A213546468977M") taxNumber: Option[String],
    @ApiModelProperty(name = "registration_date", required = true, example = "1996-01-20") registrationDate: Option[LocalDate]) extends BusinessUserApplicationToCreateT
