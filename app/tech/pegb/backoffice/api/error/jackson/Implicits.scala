package tech.pegb.backoffice.api.error.jackson

object Implicits {

  private val MissingReqPropRegex = s"""(Missing required creator property) '([a-zA-Z_]*)' ([\\(\\)a-zA-Z0-9 ]*)""".r
  private val MissingOptPropRegex = s"""(Missing creator property) '([a-zA-Z_]*)' ([\\(\\)a-zA-Z0-9 ]*)""".r
  private val WrongTypeRegex = s"""(Cannot deserialize value of type) `([\\S]*)` from ([\\S]*) "([\\s\\S]*?)"""".r

  implicit class JsonMappingExceptionFriendlyErrorMsgAdapter(val error: Throwable) extends AnyVal {
    def asFriendlyErrorMsg: Option[String] = {
      error match {
        case error: com.fasterxml.jackson.databind.JsonMappingException ⇒
          val friendErrMsg = error.getMessage.trim.split(System.lineSeparator()).head.split(":|;").head match {
            case MissingReqPropRegex(_, missingProperty, _) ⇒
              s"Required field [$missingProperty] was missing in the request."
            case MissingOptPropRegex(_, missingProperty, _) ⇒
              s"Optional field [$missingProperty] was missing in the request. Please include [$missingProperty] and set to null or change deserialization config strictness."
            case WrongTypeRegex(_, correctType, _, currentValue) ⇒
              s"Field with value [$currentValue] should be assigned a type of [$correctType] value only."
            case errorMsg if errorMsg.contains("Cannot construct instance of") ⇒
              "Structure of the request is not a valid json. Please confirm with a json validator."
            case errorMsg if errorMsg.contains("Unexpected end-of-input") ⇒
              "Structure of the request is not a valid json. Please confirm with a json validator."
            case errorMsg ⇒
              errorMsg
          }
          Some(friendErrMsg)
        case other ⇒
          None
      }
    }
  }

}
