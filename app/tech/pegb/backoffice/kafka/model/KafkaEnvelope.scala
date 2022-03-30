package tech.pegb.backoffice.kafka.model

import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import play.api.libs.json._

case class KafkaEnvelope(
    track_id: String,
    created_at: String,
    payload: Payload)

object KafkaEnvelope {
  implicit val f = Json.format[KafkaEnvelope]

  def serialize(topic: String, envelope: KafkaEnvelope)(implicit serializer: Serializer[KafkaEnvelope]): Array[Byte] =
    serializer.serialize(topic, envelope)

  def deserialize(topic: String, bytes: Array[Byte])(implicit deserializer: Deserializer[KafkaEnvelope]): KafkaEnvelope =
    deserializer.deserialize(topic, bytes)

  def serde(topic: String, bytes: Array[Byte])(implicit serde: Serde[KafkaEnvelope]): KafkaEnvelope =
    serde.deserializer().deserialize(topic, bytes)
}
