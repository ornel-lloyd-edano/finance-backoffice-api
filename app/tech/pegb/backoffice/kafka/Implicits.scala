package tech.pegb.backoffice.kafka

import java.lang.{Long ⇒ JLong}
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.{Currency ⇒ JCurrency}

import com.sksamuel.avro4s._
import com.sksamuel.avro4s.kafka.GenericSerde
import org.apache.avro.Schema
import org.apache.avro.util.Utf8

object Implicits {
  implicit class SerdeWithTopicSyntax[T >: Null: SerdeWithTopic](data: T) {
    private val st = SerdeWithTopic[T]

    val topic: String = st.topic

    def encode: Array[Byte] = {
      st.serde.serialize(st.topic, data)
    }
  }

  object AvroSchemas {
    //implicit val namingStrategy: NamingStrategy = SnakeCase
    val tz: ZoneOffset = ZoneOffset.UTC

    implicit object DateTimeSchemaFor extends SchemaFor[ZonedDateTime] {
      override val schema: Schema = Schema.create(Schema.Type.LONG)
    }

    implicit object ZonedDateTimeEncoder extends Encoder[ZonedDateTime] {
      override def encode(value: ZonedDateTime, schema: Schema): AnyRef = {
        JLong.valueOf(value.withZoneSameInstant(tz).toInstant.toEpochMilli)
      }
    }

    implicit object ZonedDateTimeDecoder extends Decoder[ZonedDateTime] {
      override def decode(value: Any, schema: Schema): ZonedDateTime = {
        Instant.ofEpochMilli(value.asInstanceOf[Long]).atZone(tz)
      }
    }

    implicit object JCurrencySchemaFor extends SchemaFor[JCurrency] {
      override val schema: Schema = Schema.create(Schema.Type.STRING)
    }

    implicit object JCurrencyEncoder extends Encoder[JCurrency] {
      override def encode(value: JCurrency, schema: Schema): AnyRef = {
        value.getCurrencyCode
      }
    }

    implicit object JCurrencyDecoder extends Decoder[JCurrency] {
      override def decode(value: Any, schema: Schema): JCurrency = {
        JCurrency.getInstance(value.asInstanceOf[Utf8].toString)
      }
    }

    val byteArraySchema: Schema = AvroSchema[Array[Byte]]

    class MyGenericSerde[T >: Null: SchemaFor: Encoder: Decoder](override val schema: Schema)
      extends GenericSerde[T] {

    }

    object Serdes {
      // TODO: might be useful to override deserialize to just return input byte array
      val byteArraySerde = new MyGenericSerde[Array[Byte]](byteArraySchema)
    }
  }
}
