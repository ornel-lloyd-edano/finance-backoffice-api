package tech.pegb.backoffice.application
import play.api.libs.json.{Format, JsValue}
import tech.pegb.backoffice.kafka.model.ActionType.ActionType

class MockKafkaDbSyncServiceImpl extends KafkaDBSyncService {
  override def sendUpdate[T](tableName: String, entity: T)(implicit fmt: Format[T]): Unit = {
    logger.info(s"Received send update tableName: $tableName, entity: $entity")
  }

  override def sendInsert[T](tableName: String, entity: T)(implicit fmt: Format[T]): Unit = {
    logger.info(s"Received send insert tableName: $tableName, entity: $entity")
  }

  override def sendUpsert[T](tableName: String, entity: T)(implicit fmt: Format[T]): Unit = {
    logger.info(s"Received send upsert tableName: $tableName, entity: $entity")
  }

  override def sendDelete(tableName: String, id: Long): Unit = {
    logger.info(s"Received send delete tableName: $tableName, id: $id")
  }

  def sendDelete(tableName: String, uuid: String): Unit = {
    logger.info(s"Received send delete tableName: $tableName, uuid: $uuid")
  }

  override protected def send(tableName: String, action: ActionType, entity: JsValue): Unit = ()
}
