package tech.pegb.backoffice.domain.customer.error

import java.util.UUID

import tech.pegb.backoffice.dao.customer.entity.User
import tech.pegb.backoffice.domain.{ErrorCode, ServiceError}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.ActivationDocumentType

case class InsufficientCustomerActivationRequirements(inactiveUser: User, missingRequirements: Set[ActivationDocumentType], id: UUID = UUID.randomUUID()) extends ServiceError {
  def code: ErrorCode = ??? //TODO: Put correct ErrorCode
  def message = s"Customer ${inactiveUser.uuid} cannot be activated without these requirements: ${missingRequirements.map(_.underlying).mkString(", ")}"

  override def equals(obj: Any): Boolean = {
    val other = obj.asInstanceOf[InsufficientCustomerActivationRequirements]
    other.inactiveUser == this.inactiveUser && other.missingRequirements == this.missingRequirements
  }
}
