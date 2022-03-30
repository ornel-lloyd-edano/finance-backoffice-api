package tech.pegb.backoffice.application

import javax.inject.Singleton
import tech.pegb.backoffice.application.model.{EmailMessage, NotificationMessage}
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.auth.model.Email

@Singleton
class MockCommunicationServiceImpl extends CommunicationService {
  import scala.collection.mutable

  val sentNotifications: mutable.ListBuffer[Any] = mutable.ListBuffer.empty
  val sentEmails: mutable.ListBuffer[NotificationMessage] = mutable.ListBuffer.empty

  override def notify(recipient: Email, subject: String, message: String): ServiceResponse[NotificationMessage] = {
    val msg = EmailMessage(None, recipient, subject, message)
    sentEmails.append(msg)
    Right(msg)
  }

  def lastNotification: Option[Any] = sentNotifications.lastOption
}
