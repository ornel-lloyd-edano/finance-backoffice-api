package tech.pegb.backoffice.domain.customer.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.types.abstraction.TypeEnum
import tech.pegb.backoffice.util.Implicits._

sealed trait AddressType extends TypeEnum {
  lazy val kind = AddressTypes.toString
  def isUnknown: Boolean = false
  def isPrimaryAddress: Boolean = false
  def isSecondaryAddress: Boolean = false

  override def equals(obj: Any): Boolean = {
    obj match {
      case obj: AddressType ⇒ obj.toString == this.toString
      case obj: String ⇒ obj == this.toString
      case obj ⇒ false
    }
  }
}

object AddressTypes {

  override def toString = "address_types"

  lazy val toSeq = Seq(PrimaryAddress, SecondaryAddress)
  lazy val toMap = toSeq.map(d ⇒ d.toString → d).toMap

  //Note: to be used when reading from api layer
  def validate(arg: String): ServiceResponse[AddressType] =
    toMap.get(arg.toLowerCase).map(Right(_)).getOrElse(Left(ServiceError.validationError(s"Unknown document status [$arg]. Valid document status: ${toSeq.defaultMkString}")))

  //Note: to be used when reading from dao layer
  def fromString(arg: String): AddressType =
    toMap.get(arg.toLowerCase).getOrElse(UnknownAddressType(arg))

  case object PrimaryAddress extends AddressType {
    override def toString: String = "primary_address"
    override def isPrimaryAddress: Boolean = true
  }
  case object SecondaryAddress extends AddressType {
    override def toString: String = "secondary_address"
    override def isSecondaryAddress: Boolean = true
  }

  case class UnknownAddressType private[model] (underlying: String) extends AddressType {
    override def toString = underlying
    override def isUnknown: Boolean = true
  }
}
