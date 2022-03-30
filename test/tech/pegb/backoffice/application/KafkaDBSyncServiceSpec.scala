package tech.pegb.backoffice.application

import java.util.concurrent.Executors

import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import play.api.Configuration
import play.api.libs.json.Json
import tech.pegb.backoffice.application.modules.KafkaModule.KProducer
import tech.pegb.backoffice.kafka.model.{ActionType, KafkaEnvelope}
import tech.pegb.core.PegBNoDbTestApp

import scala.concurrent.ExecutionContext
import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import org.scalatest.Ignore

@Ignore
class KafkaDBSyncServiceSpec extends PegBNoDbTestApp with EmbeddedKafka {
  case class Dummy(field1: String)
  implicit val f = Json.format[Dummy]

  "KafkaDBSyncService" should {
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

    val kafkaConfiguration: Configuration = conf.get[Configuration]("kafka")
    val commonKafkaConfiguration = kafkaConfiguration.get[Configuration]("common")
    val producerConfiguration = commonKafkaConfiguration ++ kafkaConfiguration.get[Configuration]("producer")

    val producer = new KProducer(producerConfiguration)(ec)
    val service = new KafkaDBSyncServiceImpl(producer)

    implicit val config: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = 9092)

    "send update message successfully" in {
      withRunningKafka {
        val entity = Dummy("value1")

        service.sendUpdate("testTable", entity)

        val receivedEnvelope = consumeFirstMessageFrom[KafkaEnvelope]("backoffice_db-1.0.0-testTable")

        receivedEnvelope.payload.action mustBe ActionType.UPDATE
        receivedEnvelope.payload.entity mustBe Json.toJson(entity)
      }
    }

    "send insert message successfully" in {
      withRunningKafka {
        val entity = Dummy("value1")

        service.sendInsert("testTable", entity)

        val receivedEnvelope = consumeFirstMessageFrom[KafkaEnvelope]("backoffice_db-1.0.0-testTable")

        receivedEnvelope.payload.action mustBe ActionType.INSERT
        receivedEnvelope.payload.entity mustBe Json.toJson(entity)
      }
    }

    "send upsert message successfully" in {
      withRunningKafka {
        val entity = Dummy("value1")

        service.sendUpsert("testTable", entity)

        val receivedEnvelope = consumeFirstMessageFrom[KafkaEnvelope]("backoffice_db-1.0.0-testTable")

        receivedEnvelope.payload.action mustBe ActionType.UPSERT
        receivedEnvelope.payload.entity mustBe Json.toJson(entity)
      }
    }

    "send delete message successfully" in {
      withRunningKafka {
        val entity = Json.obj("id" -> 12)

        service.sendDelete("testTable", 12)

        val receivedEnvelope = consumeFirstMessageFrom[KafkaEnvelope]("backoffice_db-1.0.0-testTable")

        receivedEnvelope.payload.action mustBe ActionType.DELETE
        receivedEnvelope.payload.entity mustBe entity
      }
    }

    "send delete message with uuid successfully" in {
      withRunningKafka {
        val entity = Json.obj("uuid" -> "someUUID")

        service.sendDelete("testTable", "someUUID")

        val receivedEnvelope = consumeFirstMessageFrom[KafkaEnvelope]("backoffice_db-1.0.0-testTable")

        receivedEnvelope.payload.action mustBe ActionType.DELETE
        receivedEnvelope.payload.entity mustBe entity
      }
    }
  }

}
