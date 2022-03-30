package tech.pegb.backoffice.domain.customer.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.types.abstraction.TypeEnum
import tech.pegb.backoffice.util.Implicits._

sealed trait ContactType extends TypeEnum {
  lazy val kind = ContactTypes.toString
  def isBusinessOwner: Boolean = false
  def isAssociate: Boolean = false
  def isEmployee: Boolean = false
  def isUnknown: Boolean = false

  override def equals(obj: Any): Boolean = {
    obj match {
      case obj: ContactType ⇒ obj.toString == this.toString
      case obj: String ⇒ obj == this.toString
      case obj ⇒ false
    }
  }
}

object ContactTypes {
  override def toString = "contact_types"

  lazy val toSeq = Seq(BusinessOwner, Associate, Employee)
  lazy val toMap = toSeq.map(d ⇒ d.toString → d).toMap

  //Note: to be used when reading from api layer
  def validate(arg: String): ServiceResponse[ContactType] =
    toMap.get(arg.toLowerCase).map(Right(_)).getOrElse(Left(ServiceError.validationError(s"Unknown document status [$arg]. Valid document status: ${toSeq.defaultMkString}")))

  //Note: to be used when reading from dao layer
  def fromString(arg: String): ContactType =
    toMap.get(arg.toLowerCase).getOrElse(UnknownContactType(arg))

  case object BusinessOwner extends ContactType {
    override def toString: String = "business_owner"
    override def isBusinessOwner = true
  }

  case object Associate extends ContactType {
    override def toString: String = "associate"
    override def isAssociate = true
  }

  case object Employee extends ContactType {
    override def toString: String = "employee"
    override def isEmployee = true
  }

  case class UnknownContactType private[model] (underlying: String) extends ContactType {
    override def toString = underlying
    override def isUnknown: Boolean = true
  }
}
