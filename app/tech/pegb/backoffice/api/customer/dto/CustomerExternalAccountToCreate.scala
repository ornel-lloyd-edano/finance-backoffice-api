package tech.pegb.backoffice.api.customer.dto

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

trait CustomerExternalAccountToCreateT {
  val provider: String
  val accountNumber: String
  val accountHolder: String
  val currency: String
}

case class CustomerExternalAccountToCreate(
    @JsonProperty(required = true) provider: String,
    @JsonProperty(required = true) accountNumber: String,
    @JsonProperty(required = true) accountHolder: String,
    @JsonProperty(required = true) currency: String) extends CustomerExternalAccountToCreateT {

}

trait ExternalAccountToCreateT {
  val customerId: UUID
  val provider: String
  val accountNumber: String
  val accountHolder: String
  val currency: String
}

case class ExternalAccountToCreate(
    @JsonProperty(required = true) customerId: UUID,
    @JsonProperty(required = true) provider: String,
    @JsonProperty(required = true) accountNumber: String,
    @JsonProperty(required = true) accountHolder: String,
    @JsonProperty(required = true) currency: String) extends ExternalAccountToCreateT {

}
