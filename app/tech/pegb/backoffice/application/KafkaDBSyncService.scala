package tech.pegb.backoffice.application

import java.time.{Instant}
import java.util.UUID

import com.google.inject._
import play.api.libs.json._
import tech.pegb.backoffice.kafka.KProducerLike
import tech.pegb.backoffice.kafka.model.{ActionType, KafkaEnvelope, Payload}
import tech.pegb.backoffice.util.Logging
import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import tech.pegb.backoffice.kafka.model.ActionType.ActionType

@ImplementedBy(classOf[KafkaDBSyncServiceImpl])
trait KafkaDBSyncService extends Logging {
  def sendUpdate[T](tableName: String, entity: T)(implicit fmt: Format[T]): Unit
  def sendInsert[T](tableName: String, entity: T)(implicit fmt: Format[T]): Unit
  def sendUpsert[T](tableName: String, entity: T)(implicit fmt: Format[T]): Unit
  def sendDelete(tableName: String, id: Long): Unit
  def sendDelete(tableName: String, uuid: String): Unit
  protected def send(tableName: String, action: ActionType, entity: JsValue): Unit
}

@Singleton
class KafkaDBSyncServiceImpl @Inject() (kafkaProducer: KProducerLike) extends KafkaDBSyncService {

  def sendUpdate[T](tableName: String, entity: T)(implicit fmt: Format[T]): Unit = {
    send(tableName, ActionType.UPDATE, Json.toJson(entity))
  }

  def sendInsert[T](tableName: String, entity: T)(implicit fmt: Format[T]): Unit = {
    send(tableName, ActionType.INSERT, Json.toJson(entity))
  }

  def sendUpsert[T](tableName: String, entity: T)(implicit fmt: Format[T]): Unit = {
    send(tableName, ActionType.UPSERT, Json.toJson(entity))
  }

  def sendDelete(tableName: String, id: Long): Unit = {
    send(tableName, ActionType.DELETE, Json.obj("id" -> id))
  }

  def sendDelete(tableName: String, uuid: String): Unit = {
    send(tableName, ActionType.DELETE, Json.obj("uuid" -> uuid))
  }

  override protected def send(tableName: String, action: ActionType, entity: JsValue): Unit = {
    val payload = Payload(action, entity)
    val trackId = UUID.randomUUID().toString
    val createdAt = Instant.now().toEpochMilli.toString
    val envelope = KafkaEnvelope(trackId, createdAt, payload)

    val topic = kafkaProducer.targetTopicName(tableName)

    kafkaProducer.send(topic, KafkaEnvelope.serialize(topic, envelope)) //TODO: Check future and log.
  }
}
