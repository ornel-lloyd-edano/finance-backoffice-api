package tech.pegb.backoffice.api.businessuserapplication.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

trait BusinessUserApplicationContactInfoToCreateT {
  val contacts: Seq[BusinessUserApplicationContact]
  val addresses: Seq[BusinessUserApplicationAddress]
  val lastUpdatedAt: Option[ZonedDateTime]
}

case class BusinessUserApplicationContactInfoToCreate(
    contacts: Seq[BusinessUserApplicationContact],
    addresses: Seq[BusinessUserApplicationAddress],
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime]) extends BusinessUserApplicationContactInfoToCreateT

