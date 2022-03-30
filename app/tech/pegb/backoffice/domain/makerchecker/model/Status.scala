package tech.pegb.backoffice.domain.makerchecker.model

sealed trait Status {
  val underlying: String
  override def toString = underlying

  def isPending: Boolean = this == Statuses.Pending

  def isChecked: Boolean = this == Statuses.Approved || this == Statuses.Rejected
}

object Statuses {

  case object Pending extends Status {
    val underlying = "pending"
  }

  case object Approved extends Status {
    val underlying: String = "approved"
  }

  case object Rejected extends Status {
    val underlying = "rejected"
  }

  implicit class StatusStringToDomainAdapter(val arg: String) extends AnyVal {
    def asDomain = arg.trim.toLowerCase match {
      case Pending.underlying ⇒ Pending
      case Approved.underlying ⇒ Approved
      case Rejected.underlying ⇒ Rejected
    }
  }
}
