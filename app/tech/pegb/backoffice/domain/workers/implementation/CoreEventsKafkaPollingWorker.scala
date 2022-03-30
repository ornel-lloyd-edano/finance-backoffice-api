package tech.pegb.backoffice.domain.workers.implementation

import akka.actor.{ActorSystem, Cancellable}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsNumber, JsObject, Json}
import tech.pegb.backoffice.api.communication.dto.{EventTypes, PayloadKeys}
import tech.pegb.backoffice.domain.application.abstraction.WalletApplicationManagement
import tech.pegb.backoffice.domain.workers.CoreEventsWorker
import tech.pegb.backoffice.kafka.KConsumerLike
import tech.pegb.backoffice.kafka.SerdeWithTopic.CoreEventBytesST

import scala.collection.immutable.{Iterable ⇒ II}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class CoreEventsKafkaPollingWorker @Inject() (
    kConsumer: KConsumerLike,
    actorSystem: ActorSystem,
    applicationManagement: WalletApplicationManagement,
    ceST: CoreEventBytesST) extends CoreEventsWorker {
  private val pollInterval = 10.seconds
  private val taskInterval = pollInterval * 2
  private val runnable = new Runnable {
    override def run(): Unit = {
      kConsumer.poll[Array[Byte]](pollInterval)(ceST.serde).map { records ⇒
        val futures = records.map { msgBytes ⇒
          Json.parse(msgBytes) match {
            case jsObj: JsObject ⇒
              jsObj("type").toString() match {
                case EventTypes.ApplicationApproved ⇒
                  jsObj(PayloadKeys.Id) match {
                    case JsNumber(x) ⇒
                      val applicationId = x.intValue()
                      applicationManagement
                        .persistApprovedFilesByInternalApplicationId(applicationId)
                        .map(
                          _.fold(
                            err ⇒ {
                              logger.warn(s"Failed to persist files for application $applicationId: ${err.message}")
                              1
                            },
                            _ ⇒ 0))
                    case _ ⇒
                      logger.warn(s"application_id is not a number in $jsObj")
                      Future.successful(1)
                  }
                case _ ⇒
                  Future.successful(0) // TODO: complete for other types when needed
              }
            case unknown ⇒
              logger.warn(s"Couldn't decode kafka message as json object: $unknown")
              Future.successful(1)
          }
        }
        Future.foldLeft[Int, Long](II.concat(futures))(0L)((acc, err) ⇒ acc + err).map { errorsCount ⇒
          if (errorsCount == 0) {
            kConsumer.commit[Array[Byte]]()(ceST.serde)
          }
        }
      }
    }
  }

  override protected def run(): Cancellable = {
    actorSystem.scheduler.schedule(Duration.Zero, taskInterval, runnable)
  }

}
