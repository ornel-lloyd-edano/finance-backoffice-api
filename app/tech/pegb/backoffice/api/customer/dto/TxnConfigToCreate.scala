package tech.pegb.backoffice.api.customer.dto

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

trait TxnConfigToCreateT {
  val customerId: UUID
  val transactionType: String
  val currency: String
}

case class TxnConfigToCreate(
    @JsonProperty(required = true) customerId: UUID,
    @JsonProperty(required = true) transactionType: String,
    @JsonProperty(required = true) currency: String) extends TxnConfigToCreateT {

}

trait CustomerTxnConfigToCreateT {
  val transactionType: String
  val currency: String
}

case class CustomerTxnConfigToCreate(
    @JsonProperty(required = true) transactionType: String,
    @JsonProperty(required = true) currency: String) extends CustomerTxnConfigToCreateT {

}
