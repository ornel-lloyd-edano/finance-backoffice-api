package tech.pegb.backoffice.api.swagger.model

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.customer.dto.SavingOptionsToReadI

case class SavingOptionsToRead(
    @ApiModelProperty(name = "id", required = true) id: UUID,
    @ApiModelProperty(name = "customer_id", required = true) customerId: UUID,
    @ApiModelProperty(name = "type", required = true) `type`: String,
    @ApiModelProperty(name = "name", required = true) name: Option[String],
    @ApiModelProperty(name = "amount", required = false) amount: Option[BigDecimal],
    @ApiModelProperty(name = "current_amount", required = true) currentAmount: BigDecimal,
    @ApiModelProperty(name = "currency", required = true) currency: String,
    @ApiModelProperty(name = "reason", required = false) reason: Option[String],
    @ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "due_date", required = false) dueDate: Option[LocalDate],
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: ZonedDateTime) extends SavingOptionsToReadI {

}
