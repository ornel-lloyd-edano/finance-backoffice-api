package tech.pegb.backoffice.domain.customer.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessUserTier, BusinessUserTiers}
import tech.pegb.backoffice.domain.types.abstraction.TypeEnum
import tech.pegb.backoffice.domain.{ServiceError, Validatable}
import tech.pegb.backoffice.util.Implicits._

trait UserTier extends TypeEnum with Validatable[Unit] {
  def isBusinessUserTier: Boolean
  def isIndividualUserTier: Boolean

  def isUnknown: Boolean = false

  def isBasic: Boolean = false
  def isStandard: Boolean = false
  def isExtended: Boolean = false

  def isSmall: Boolean = false
  def isMedium: Boolean = false
  def isBig: Boolean = false

  override def equals(obj: Any): Boolean = {
    obj match {
      case obj: BusinessUserTier ⇒ obj.toString == this.toString
      case obj: IndividualUserTier ⇒ obj.toString == this.toString
      case obj: String ⇒ obj == this.toString
      case obj ⇒ false
    }
  }

  def validate: ServiceResponse[Unit] = {
    if (isUnknown)
      Left(ServiceError.validationError(s"Unknown user tier [${this.toString}]. Valid user tiers: ${(IndividualUserTiers.toSeq ++ BusinessUserTiers.toSeq).defaultMkString}"))
    else Right(())
  }
}

object UserTiers {

  lazy val toSeq = IndividualUserTiers.toSeq ++ BusinessUserTiers.toSeq
  lazy val toMap = toSeq.map(d ⇒ d.toString → d).toMap

  //Note: to be used when reading from dao layer
  def fromString(arg: String): UserTier =
    toMap.get(arg.toLowerCase).getOrElse(UnknownUserTier(arg))

  case class UnknownUserTier private[model] (underlying: String) extends UserTier {
    override def kind = "user_tiers"
    override def isBusinessUserTier: Boolean = false
    override def isIndividualUserTier: Boolean = false

    override def toString = underlying
    override def isUnknown: Boolean = true
  }
}
