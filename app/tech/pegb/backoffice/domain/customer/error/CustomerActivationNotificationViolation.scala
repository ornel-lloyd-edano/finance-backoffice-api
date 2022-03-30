package tech.pegb.backoffice.domain.customer.error

import java.util.UUID

import tech.pegb.backoffice.dao.customer.entity.User
import tech.pegb.backoffice.domain.{ErrorCode, ServiceError}

case class CustomerActivationNotificationViolation(user: User, reason: String, id: UUID = UUID.randomUUID()) extends ServiceError {
  def code: ErrorCode = ??? //TODO: Put correct ErrorCode
  def message = s"Notifying customer ${user.uuid} for activation is not allowed. Reason: $reason"
}
