package tech.pegb.backoffice.domain.customer.error

import java.util.UUID

import tech.pegb.backoffice.dao.customer.entity.User
import tech.pegb.backoffice.domain.ErrorCodes.ValidationFailed
import tech.pegb.backoffice.domain.{ErrorCode, ServiceError}

case class InvalidUserType(user: User, id: UUID = UUID.randomUUID()) extends ServiceError {
  def code: ErrorCode = ValidationFailed
  def message = s"Invalid user type, user ${user.uuid} is a ${user.`type`.getOrElse("UNKNOWN")} user"
}
