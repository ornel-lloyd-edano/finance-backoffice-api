package tech.pegb.backoffice.domain.document.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.types.abstraction.TypeEnum
import tech.pegb.backoffice.util.Implicits._

sealed trait DocumentStatus extends TypeEnum {
  lazy val kind = DocumentStatuses.toString
  def isUnknown: Boolean = false
  def isApproved: Boolean = false
  def isPending: Boolean = false

  override def equals(obj: Any): Boolean = {
    obj match {
      case obj: DocumentStatus ⇒ obj.toString == this.toString
      case obj: String ⇒ obj == this.toString
      case obj ⇒ false
    }
  }
}

object DocumentStatuses {

  override def toString = "document_statuses"

  lazy val toSeq = Seq(Approved, Pending, Rejected)
  lazy val toMap = toSeq.map(d ⇒ d.toString → d).toMap

  //Note: to be used when reading from api layer
  def validate(arg: String): ServiceResponse[DocumentStatus] =
    toMap.get(arg.toLowerCase).map(Right(_)).getOrElse(Left(ServiceError.validationError(s"Unknown document status [$arg]. Valid document status: ${toSeq.defaultMkString}")))

  //Note: to be used when reading from dao layer
  def fromString(arg: String): DocumentStatus =
    toMap.get(arg.toLowerCase).getOrElse(UnknownDocumentStatus(arg))

  case object Approved extends DocumentStatus {
    override def toString: String = "approved"
    override def isApproved: Boolean = true
  }
  case object Pending extends DocumentStatus {
    override def toString: String = "pending"
    override def isPending: Boolean = true
  }
  case object Rejected extends DocumentStatus {
    override def toString: String = "rejected"
  }
  case object Cancelled extends DocumentStatus {
    override def toString: String = "cancelled"
  }
  case object Ongoing extends DocumentStatus {
    override def toString: String = "ongoing"
  }

  case class UnknownDocumentStatus private[model] (underlying: String) extends DocumentStatus {
    override def toString = underlying
    override def isUnknown: Boolean = true
  }
}
