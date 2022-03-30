package tech.pegb.backoffice.kafka

import scala.concurrent.Future
import scala.concurrent.duration.Duration

trait KConsumerLike {
  def poll[T >: Null: SerdeWithTopic](duration: Duration): Future[Seq[T]]
  def commit[T >: Null: SerdeWithTopic](): Unit
  def shutdown(): Future[Unit]
}
