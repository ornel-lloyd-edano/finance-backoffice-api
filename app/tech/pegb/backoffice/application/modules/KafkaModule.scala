package tech.pegb.backoffice.application.modules

import java.time.{Duration ⇒ JDuration}
import java.util.Collections
import java.util.concurrent.Executors

import cakesolutions.kafka.{KafkaConsumer, KafkaProducer}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import tech.pegb.backoffice.application.modules.KafkaModule.{KConsumer, KProducer}
import tech.pegb.backoffice.kafka.SerdeWithTopic.CoreEventBytesST
import tech.pegb.backoffice.kafka._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

class KafkaModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    val kafkaConfiguration: Configuration = configuration
      .get[Configuration]("kafka")
    val commonKafkaConfiguration = kafkaConfiguration.get[Configuration]("common")
    val consumerConfiguration = commonKafkaConfiguration ++ kafkaConfiguration.get[Configuration]("consumer")
    val producerConfiguration = commonKafkaConfiguration ++ kafkaConfiguration.get[Configuration]("producer")

    val consumerTopicsConfiguration = kafkaConfiguration.get[Configuration]("topics")
    val consumerTopics = ConsumerTopics(
      coreEvents = consumerTopicsConfiguration.get[String]("core-events"))

    import Implicits.AvroSchemas.Serdes._
    implicit val coreEventSAT: SerdeWithTopic[Array[Byte]] = new SerdeWithTopic[Array[Byte]](consumerTopics.coreEvents, byteArraySerde)

    val kConsumer = {
      implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(consumerTopics.all.size))
      new KConsumer(consumerConfiguration, consumerTopics)
    }
    val kProducer = {
      implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
      new KProducer(producerConfiguration)(ec)
    }
    Seq(
      bind[KConsumerLike].toInstance(kConsumer),
      bind[KProducerLike].toInstance(kProducer),
      bind[CoreEventBytesST].toInstance(new CoreEventBytesST(coreEventSAT)))
  }
}

object KafkaModule {

  class KProducer(producerConfig: Configuration)(implicit ec: ExecutionContext) extends KProducerLike {

    override val dbName: String = producerConfig.get[String]("db_name")
    override val dbVersion: String = producerConfig.get[String]("db_version")
    override def targetTopicName(tableName: String): String = s"${producerConfig.get[String]("topic-suffix")}-$tableName"

    private val serializer = new ByteArraySerializer()

    private val producer = KafkaProducer(
      KafkaProducer.Conf(producerConfig.underlying, serializer, serializer))

    override def send(topic: String, bytes: Array[Byte]): Future[RecordMetadata] = {
      send(new ProducerRecord[Array[Byte], Array[Byte]](topic, bytes))
    }

    override def shutdown(): Future[Unit] = Future {
      producer.close()
    }

    private def send(record: ProducerRecord[Array[Byte], Array[Byte]]): Future[RecordMetadata] = {
      producer.send(record)
    }
  }

  class KConsumer(consumerConfig: Configuration, topics: ConsumerTopics)(implicit ec: ExecutionContext)
    extends KConsumerLike {
    private val deserializer = new ByteArrayDeserializer()
    private val consumersMap = topics.all
      .map(_ → KafkaConsumer(KafkaConsumer.Conf(consumerConfig.underlying, deserializer, deserializer)))
      .toMap
    private val durationCache: mutable.WeakHashMap[Long, JDuration] = mutable.WeakHashMap.empty

    override def poll[T >: Null: SerdeWithTopic](duration: Duration): Future[Seq[T]] = {
      val serdeAndTopic = implicitly[SerdeWithTopic[T]]
      val deserializer = serdeAndTopic.serde.deserializer()
      val topic = serdeAndTopic.topic
      consumersMap.get(topic).fold {
        Future.successful(Seq.empty[T])
      } { consumer ⇒
        Future {
          val ms = duration.toMillis
          val jDuration = durationCache.getOrElseUpdate(ms, JDuration.ofMillis(ms))
          val consumedRecords = consumer.poll(jDuration)
          val decodeData: Array[Byte] ⇒ T = deserializer.deserialize(topic, _)
          consumedRecords.records(topic).asScala
            .foldLeft(Seq.empty[T])((acc, elem) ⇒ acc :+ decodeData(elem.value()))
        }
      }
    }

    override def commit[T >: Null: SerdeWithTopic](): Unit = {
      val serdeAndTopic = implicitly[SerdeWithTopic[T]]
      consumersMap.get(serdeAndTopic.topic).foreach(_.commitSync())
    }

    override def shutdown(): Future[Unit] = Future {
      consumersMap.values.foreach(_.close())
    }

    protected def init(): Unit = {
      consumersMap.foreach {
        case (topic, consumer) ⇒ consumer.subscribe(Collections.singletonList(topic))
      }
    }
  }
}
