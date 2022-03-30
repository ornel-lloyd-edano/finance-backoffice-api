package tech.pegb.backoffice.domain.businessuserapplication.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.customer.model.UserTier
import tech.pegb.backoffice.util.Implicits._

sealed trait BusinessUserTier extends UserTier {
  override lazy val kind = BusinessUserTiers.toString
  override def isBusinessUserTier: Boolean = true
  override def isIndividualUserTier: Boolean = false

  override def equals(obj: Any): Boolean = {
    obj match {
      case obj: BusinessUserTier ⇒ obj.toString == this.toString
      case obj: String ⇒ obj == this.toString
      case obj ⇒ false
    }
  }

  override def validate: ServiceResponse[Unit] = {
    if (isUnknown)
      Left(ServiceError.validationError(s"Unknown business user tier [${this.toString}]. Valid business user tiers: ${BusinessUserTiers.toSeq.defaultMkString}"))
    else Right(())
  }
}

object BusinessUserTiers {
  override def toString = "business_user_tiers"

  lazy val toSeq = Seq(Small, Medium, Big)
  lazy val toMap = toSeq.map(d ⇒ d.toString → d).toMap

  //Note: to be used when reading from dao layer
  def fromString(arg: String): BusinessUserTier =
    toMap.get(arg.toLowerCase).getOrElse(UnknownBusinessUserTier(arg))

  case object Small extends BusinessUserTier {
    override def toString: String = "small"
    override def isSmall: Boolean = true
  }
  case object Medium extends BusinessUserTier {
    override def toString: String = "medium"
    override def isMedium: Boolean = true
  }
  case object Big extends BusinessUserTier {
    override def toString: String = "big"
    override def isBig: Boolean = true
  }

  case class UnknownBusinessUserTier private[model] (underlying: String) extends BusinessUserTier {
    override def toString = underlying
    override def isUnknown: Boolean = true
  }
}
