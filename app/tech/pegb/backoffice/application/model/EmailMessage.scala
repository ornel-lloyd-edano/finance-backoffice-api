package tech.pegb.backoffice.application.model

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.util.Implicits._

@ImplementedBy(classOf[EmailMessage])
trait NotificationMessage {
  val sender: Any
  val recipient: Any
  val subject: String
  val message: String
}

case class EmailMessage(sender: Option[Email] = None, recipient: Email, subject: String, message: String) extends NotificationMessage {
  assert(subject.hasSomething, "empty email subject")
  assert(message.hasSomething, "empty email message")
}
