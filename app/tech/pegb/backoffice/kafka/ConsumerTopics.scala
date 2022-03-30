package tech.pegb.backoffice.kafka

final case class ConsumerTopics(
    coreEvents: String) {
  val all: Seq[String] = productIterator.flatMap(t ⇒ Option(t.asInstanceOf[String])).toSeq
}
