package tech.pegb.backoffice.domain

import java.util.Properties

import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{Address, Message, Session}
import com.google.inject.{ImplementedBy, Inject}
import tech.pegb.backoffice.util.{AppConfig, Logging}

import scala.util.Try

@ImplementedBy(classOf[EmailClientService])
trait EmailClient {
  def sendEmail(recipient: Seq[String], subject: String, content: String): Either[Throwable, Unit]
}

class EmailClientService @Inject() (config: AppConfig) extends EmailClient with Logging {

  private val mailerHost = config.Mailer.Host
  private val mailerPort = config.Mailer.Port
  private val transportProtocol = "smtp"
  private val mailSender = config.Mailer.Sender

  private val getProperties: Properties = {
    val properties = new Properties

    properties.setProperty("mail.smtp.port", mailerPort.toString)
    properties.setProperty("mail.host", mailerHost)
    properties.setProperty("mail.transport.protocol", transportProtocol)
    properties.setProperty("mail.smtp.starttls.enable", "false")
    properties.setProperty("mail.user", mailSender)
    properties.setProperty("mail.smtp.auth", "false")

    properties
  }

  def sendEmail(recipient: Seq[String], subject: String, content: String): Either[Throwable, Unit] = {
    Try {
      val session: Session = Session.getInstance(getProperties)
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(mailSender))

      val recipientAddress: Array[Address] = recipient.map(new InternetAddress(_)).toArray

      message.addRecipients(Message.RecipientType.TO, recipientAddress)

      message.setSubject(subject)
      message.setHeader("Content-Type", "text/plain;")
      message.setContent(content, "text/html")
      val transport = session.getTransport(transportProtocol)

      transport.connect(mailerHost, mailerPort, mailSender, "")
      transport.sendMessage(message, message.getAllRecipients)

    }.fold(
      error ⇒ {
        logger.error(s"Mail delivery failed, ex:- ${error.getMessage}", error)
        Left(error)
      }, result ⇒ Right(()))
  }
}
