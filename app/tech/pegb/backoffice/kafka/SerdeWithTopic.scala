package tech.pegb.backoffice.kafka

import com.sksamuel.avro4s.kafka.GenericSerde

class SerdeWithTopic[T >: Null](val topic: String, val serde: GenericSerde[T])

object SerdeWithTopic {
  def apply[T >: Null](implicit st: SerdeWithTopic[T]): SerdeWithTopic[T] = st

  class CoreEventBytesST(val serde: SerdeWithTopic[Array[Byte]])
}
