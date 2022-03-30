package tech.pegb.backoffice.kafka

import java.nio.charset.StandardCharsets.UTF_8

import play.api.libs.json.Json
import tech.pegb.backoffice.kafka.model.{ActionType, KafkaEnvelope, Payload}
import KafkaEnvelope._
import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import org.scalatestplus.play.PlaySpec

class KafkaEnvelopeSpec extends PlaySpec {
  case class DummyClass(field1: String, field2: Int)
  implicit val f = Json.format[DummyClass]

  val topic = "sometopic"

  "Kafka envelope" should {
    val entity = DummyClass("f1", 2)
    val payload = Payload(ActionType.INSERT, Json.toJson(entity))
    val envelope = KafkaEnvelope("123", "321", payload)

    val json =
      """
        |{
        |  "track_id": "123",
        |  "created_at": "321",
        |  "payload": {
        |     "action": "insert",
        |     "entity": {
        |       "field1": "f1",
        |       "field2": 2
        |     }
        |  }
        |}
      """.stripMargin

    val jsonStripped = json.filterNot((x: Char) ⇒ x.isWhitespace)

    "should create proper json" in {
      Json.toJson(envelope) mustBe Json.parse(json)
    }

    "should implicitly convert to kafka Serializer" in {
      serialize(topic, envelope) mustBe jsonStripped.getBytes(UTF_8)
      serialize(topic, null) mustBe null
    }

    "should implicitly convert to kafka Deserializer" in {
      deserialize("sometopic", json.getBytes(UTF_8)) mustBe envelope
      deserialize("sometopic", null) mustBe null
    }

    "should implicitly convert to Serde" in {
      serde(topic, json.getBytes(UTF_8)) mustBe envelope
      serde(topic, null) mustBe null
    }
  }
}
