package tech.pegb.backoffice.api.json

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, PropertyNamingStrategy, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json._
import tech.pegb.backoffice.api.{ApiError, ApiErrorCode, ApiErrorCodes}

import scala.util.Try

object Implicits {

  val mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .registerModule(new JavaTimeModule)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .setPropertyNamingStrategy(
      PropertyNamingStrategy.SNAKE_CASE)

  implicit class JsonConverter[T](val obj: T) extends AnyVal {
    def json(implicit writes: Writes[T]): JsValue = writes.writes(obj)
  }

  implicit class JsonDeserializer(val arg: String) extends AnyVal {
    //NOTE: isStrict = false allows missing fields in the dto that were marked @JsonProperty(required = false) This is useful for update requests to include only the updated field
    def as[T](clazz: Class[T], isStrict: Boolean = true): Try[T] = Try {
      mapper
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, isStrict)
        .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, isStrict)
        .readValue(arg, clazz)
    }

    def asSeq[T](typeReference: TypeReference[Seq[T]]): Try[Seq[T]] = Try {
      val seqT: Seq[T] = mapper.readValue(arg, typeReference)
      seqT
    }

    def asList[T](typeReference: TypeReference[List[T]]): Try[List[T]] = Try {
      mapper.readValue(arg, typeReference): List[T]
    }

    def asJsNode = mapper.readTree(arg)
  }

  implicit class JsonSerializer(val obj: Any) extends AnyVal {
    def toJsonStr: String = mapper.writeValueAsString(obj).replace("\\", "")
    def toJsonStrWithoutQuotes: String = mapper.writeValueAsString(obj).replace("\\", "").replace("\"", "")
    def toJsonStrWithoutEscape: String = mapper.writeValueAsString(obj)
  }

  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

  implicit object ApiErrorCodeFormat extends Format[ApiErrorCode] {
    def writes(err: ApiErrorCode) = Json.toJson(err.toString)

    def reads(json: JsValue): JsResult[ApiErrorCode] = {
      Try(json.as[String]).fold(
        _ ⇒ JsError("Unable to read ApiErrorCode json"),
        value ⇒ Try(ApiErrorCodes.fromString(value))
          .fold(_ ⇒ JsError("Unexpected ApiErrorCode from json"), JsSuccess(_)))
    }

  }

  implicit val apiErrorFormat = Json.format[ApiError]
}
