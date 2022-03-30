package tech.pegb.backoffice.domain.customer.error

import java.util.UUID

import tech.pegb.backoffice.dao.customer.entity.User
import tech.pegb.backoffice.domain.ErrorCodes.ValidationFailed
import tech.pegb.backoffice.domain.{ErrorCode, ServiceError}

case class InactiveCustomerFound(inactiveUser: User, id: UUID = UUID.randomUUID()) extends ServiceError {
  def code: ErrorCode = ValidationFailed
  def message = s"Customer ${inactiveUser.uuid} was found with status ${inactiveUser.status} and currently not active"
}
