package tech.pegb.backoffice.kafka

import org.apache.kafka.clients.producer.RecordMetadata

import scala.concurrent.Future

trait KProducerLike {
  val dbName: String
  val dbVersion: String
  def targetTopicName(tableName: String): String

  def send(topic: String, bytes: Array[Byte]): Future[RecordMetadata]

  def shutdown(): Future[Unit]
}
