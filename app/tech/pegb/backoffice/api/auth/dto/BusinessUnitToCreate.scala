package tech.pegb.backoffice.api.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty

case class BusinessUnitToCreate(@JsonProperty(required = true) name: String)
