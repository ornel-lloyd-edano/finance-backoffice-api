package tech.pegb.backoffice.domain.makerchecker.model

sealed trait HttpVerb {
  val underlying: String
  override def toString = underlying

  def isBodyRequired: Boolean = this == HttpVerbs.POST || this == HttpVerbs.PUT /*|| this == HttpVerbs.DELETE*/ //TODO uncomment once front end compiles with standard
}

object HttpVerbs {

  case object POST extends HttpVerb {
    val underlying = "POST"
  }

  case object PUT extends HttpVerb {
    val underlying: String = "PUT"
  }

  case object DELETE extends HttpVerb {
    val underlying = "DELETE"
  }

  case object GET extends HttpVerb {
    val underlying = "GET"
  }

  implicit class HttpVerbFromStringAdapter(val arg: String) /*extends AnyVal*/ {
    def asHttpVerb: HttpVerb = {
      arg.trim.toUpperCase match {
        case POST.underlying ⇒ POST
        case PUT.underlying ⇒ PUT
        case DELETE.underlying ⇒ DELETE
        case GET.underlying ⇒ GET
      }
    }
  }

}
