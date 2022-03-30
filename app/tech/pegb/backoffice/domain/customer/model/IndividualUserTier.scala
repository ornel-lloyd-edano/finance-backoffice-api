package tech.pegb.backoffice.domain.customer.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.util.Implicits._

sealed trait IndividualUserTier extends UserTier {
  override lazy val kind = IndividualUserTiers.toString
  override def isBusinessUserTier: Boolean = false
  override def isIndividualUserTier: Boolean = true

  override def equals(obj: Any): Boolean = {
    obj match {
      case obj: IndividualUserTier ⇒ obj.toString == this.toString
      case obj: String ⇒ obj == this.toString
      case obj ⇒ false
    }
  }

  override def validate: ServiceResponse[Unit] = {
    if (isUnknown)
      Left(ServiceError.validationError(s"Unknown individual user tier [${this.toString}]. Valid individual user tiers: ${IndividualUserTiers.toSeq.defaultMkString}"))
    else Right(())
  }
}

object IndividualUserTiers {
  override def toString = "customer_tiers"

  lazy val toSeq = Seq(Basic, Standard, Extended)
  lazy val toMap = toSeq.map(d ⇒ d.toString → d).toMap

  //Note: to be used when reading from dao layer
  def fromString(arg: String): IndividualUserTier =
    toMap.get(arg.toLowerCase).getOrElse(UnknownIndividualUserTier(arg))

  case object Basic extends IndividualUserTier {
    override def toString: String = "basic"
    override def isBasic: Boolean = true
  }

  case object Standard extends IndividualUserTier {
    override def toString: String = "standard"
    override def isStandard: Boolean = true
  }

  case object Extended extends IndividualUserTier {
    override def toString: String = "extended"
    override def isExtended: Boolean = true
  }

  case class UnknownIndividualUserTier private[model] (underlying: String) extends IndividualUserTier {
    override def toString = underlying
    override def isUnknown: Boolean = true
  }
}
