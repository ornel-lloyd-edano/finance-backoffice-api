package tech.pegb.backoffice.domain.parameter.model

trait Platform {
  val underlying: String

  override def toString: String = underlying
}

object Platforms {

  val AllValidPlatforms = Seq(BackofficeWeb, MobileAndroid, MobileIOS)

  case object BackofficeWeb extends Platform {
    override val underlying: String = "BACKOFFICE"
  }
  case object MobileAndroid extends Platform {
    override val underlying: String = "ANDROID"
  }
  case object MobileIOS extends Platform {
    override val underlying: String = "IOS"
  }
  case object Unknown extends Platform {
    override val underlying: String = "UNKNOWN"
  }

  implicit class StringToPlatformAdapter(val arg: String) extends AnyVal {
    def asDomain = arg.trim.toUpperCase match {
      case BackofficeWeb.underlying ⇒ BackofficeWeb
      case MobileAndroid.underlying ⇒ MobileAndroid
      case MobileIOS.underlying ⇒ MobileIOS
      case _ ⇒ Unknown
    }
  }
}
