package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.businessuserapplication.dto.{BusinessUserApplicationAddress, BusinessUserApplicationContact, BusinessUserApplicationContactInfoToCreateT}

case class BusinessUserApplicationContactInfoToCreate(
    contacts: Seq[BusinessUserApplicationContact],
    addresses: Seq[BusinessUserApplicationAddress],
    @ApiModelProperty(name = "updated_at", required = false) lastUpdatedAt: Option[ZonedDateTime]) extends BusinessUserApplicationContactInfoToCreateT
