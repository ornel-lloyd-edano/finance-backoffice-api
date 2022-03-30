package tech.pegb.backoffice.domain.customer.error

import java.util.UUID

import tech.pegb.backoffice.dao.customer.entity.User
import tech.pegb.backoffice.domain.{ErrorCode, ErrorCodes, ServiceError}

case class CustomerAlreadyDeactivated(activeUser: User, id: UUID = UUID.randomUUID()) extends ServiceError {
  def code: ErrorCode = ErrorCodes.Duplicate
  def message = s"Customer is already deactivated"
}
