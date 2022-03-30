package tech.pegb.backoffice.application

import com.google.inject._
import tech.pegb.backoffice.application.model.{EmailMessage, NotificationMessage}
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.util.Logging

@ImplementedBy(classOf[CommunicationServiceImpl])
trait CommunicationService extends Logging {
  def notify(recipient: Email, subject: String, message: String): ServiceResponse[NotificationMessage]
}

//TODO: This does nothing at the moment!!!
class CommunicationServiceImpl @Inject() ()
  extends CommunicationService with Logging {

  override def notify(recipient: Email, subject: String, message: String): ServiceResponse[EmailMessage] = {
    logger.info(s"Notified ${recipient.value} about $subject")
    Right(EmailMessage(recipient = recipient, subject = subject, message = message))
  }
}
