package tech.pegb.backoffice.domain.document.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.types.abstraction.TypeEnum
import tech.pegb.backoffice.util.Implicits._

sealed trait DocumentType extends TypeEnum {
  lazy val kind = DocumentTypes.toString
  def isForBusinessApplication = false
  def isUnknown = false

  override def equals(obj: Any): Boolean = {
    obj match {
      case obj: DocumentType ⇒ obj.toString == this.toString
      case obj: String ⇒ obj == this.toString
      case obj ⇒ false
    }
  }
}

object DocumentTypes {
  override def toString = "document_types"

  lazy val toSeq = Seq(WorkPermit, IdentityCard, Image, MerchantAgreement, RegistrationCertificate, NonDisclosureAgreement, PrimaryContactId)

  lazy val toMap = toSeq.map(d ⇒ d.toString → d).toMap

  //Note: to be used when reading from api layer
  def validate(arg: String): ServiceResponse[DocumentType] =
    toMap.get(arg.toLowerCase).map(Right(_)).getOrElse(Left(ServiceError.validationError(s"Unknown document type [$arg]. Valid document types: ${toSeq.defaultMkString}")))

  //Note: to be used when reading from dao layer
  def fromString(arg: String): DocumentType =
    toMap.get(arg.toLowerCase).getOrElse(UnknownDocumentType(arg))

  case object WorkPermit extends DocumentType {
    override def toString: String = "work-permit"
  }

  case object IdentityCard extends DocumentType {
    override def toString: String = "identity-card"
  }

  case object Image extends DocumentType {
    override def toString: String = "image"
  }

  case object MerchantAgreement extends DocumentType {
    override def toString: String = "merchant-agreement"
    override def isForBusinessApplication: Boolean = true
  }

  case object RegistrationCertificate extends DocumentType {
    override def toString: String = "registration-certificate"
    override def isForBusinessApplication: Boolean = true
  }

  case object NonDisclosureAgreement extends DocumentType {
    override def toString: String = "nondisclosure-agreement"
    override def isForBusinessApplication: Boolean = true
  }

  case object PrimaryContactId extends DocumentType {
    override def toString: String = "primary-contact-id"
    override def isForBusinessApplication: Boolean = true
  }

  case class UnknownDocumentType private[model] (underlying: String) extends DocumentType {
    override def toString: String = underlying
    override def isUnknown: Boolean = true
  }
}
