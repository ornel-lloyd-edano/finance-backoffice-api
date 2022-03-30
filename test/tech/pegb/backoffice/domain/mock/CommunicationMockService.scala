package tech.pegb.backoffice.domain.mock

import tech.pegb.backoffice.application.CommunicationService
import tech.pegb.backoffice.application.model.EmailMessage
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.auth.model.Email

class CommunicationMockService extends CommunicationService {
  def notify(recipient: Email, subject: String, message: String): ServiceResponse[EmailMessage] = {
    Right(EmailMessage(recipient = recipient, subject = subject, message = message))
  }
}
