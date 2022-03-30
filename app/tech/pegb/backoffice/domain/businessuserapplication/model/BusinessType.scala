package tech.pegb.backoffice.domain.businessuserapplication.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.types.abstraction.TypeEnum
import tech.pegb.backoffice.domain.{ServiceError, Validatable}
import tech.pegb.backoffice.util.Implicits._

sealed trait BusinessType extends TypeEnum with Validatable[Unit] {
  lazy val kind = BusinessTypes.toString
  def isUnknown: Boolean = false
  def isMerchant: Boolean = false
  def isSuperMerchant: Boolean = false
  def isAgent: Boolean = false

  override def equals(obj: Any): Boolean = {
    obj match {
      case obj: BusinessType ⇒ obj.toString == this.toString
      case obj: String ⇒ obj == this.toString
      case obj ⇒ false
    }
  }

  def validate: ServiceResponse[Unit] = {
    if (isUnknown)
      Left(ServiceError.validationError(s"Unknown business type [${this.toString}]. Valid business types: ${BusinessTypes.toSeq.defaultMkString}"))
    else Right(())
  }
}

object BusinessTypes {

  override def toString = "business_types"

  lazy val toSeq = Seq(Merchant, SuperMerchant, Agent)
  lazy val toMap = toSeq.map(d ⇒ d.toString → d).toMap

  //Note: to be used when reading from dao layer
  def fromString(arg: String): BusinessType =
    toMap.get(arg.toLowerCase).getOrElse(UnknownBusinessType(arg))

  case object Merchant extends BusinessType {
    override def toString: String = "merchant"
    override def isMerchant: Boolean = true
  }
  case object SuperMerchant extends BusinessType {
    override def toString: String = "super_merchant"
    override def isSuperMerchant: Boolean = true
  }
  case object Agent extends BusinessType {
    override def toString: String = "agent"
    override def isAgent: Boolean = true
  }

  case class UnknownBusinessType private[model] (underlying: String) extends BusinessType {
    override def toString = underlying
    override def isUnknown: Boolean = true
  }

}
