package tech.pegb.backoffice.api.customer.dto

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

trait SavingOptionsToReadI {
  val id: UUID
  val customerId: UUID
  val `type`: String
  val name: Option[String]
  val amount: Option[BigDecimal]
  val currentAmount: BigDecimal
  val currency: String
  val reason: Option[String]
  val createdAt: ZonedDateTime
  val dueDate: Option[LocalDate]
  val updatedAt: ZonedDateTime
}

case class SavingOptionsToRead(
    @JsonProperty(required = true) id: UUID,
    @JsonProperty(required = true) customerId: UUID,
    @JsonProperty(required = true) `type`: String,
    @JsonProperty(required = true) name: Option[String],
    @JsonProperty(required = true) amount: Option[BigDecimal],
    @JsonProperty(required = true) currentAmount: BigDecimal,
    @JsonProperty(required = true) currency: String,
    @JsonProperty(required = true) reason: Option[String],
    @JsonProperty(required = true) createdAt: ZonedDateTime,
    @JsonProperty(required = true) dueDate: Option[LocalDate],
    @JsonProperty(required = true) updatedAt: ZonedDateTime) extends SavingOptionsToReadI {

}
