package tech.pegb.backoffice.util

import akka.actor.ActorSystem
import com.google.inject._

import scala.concurrent.ExecutionContext

@Singleton
class ExecutionContexts @Inject() (as: ActorSystem) extends WithExecutionContexts {

  val genericOperations: ExecutionContext = as.dispatchers.lookup("execution-contexts.generic-operations")

  val blockingIoOperations: ExecutionContext = as.dispatchers.lookup("execution-contexts.blocking-io-operations")

}

@ImplementedBy(classOf[ExecutionContexts])
trait WithExecutionContexts {
  val genericOperations: ExecutionContext
  val blockingIoOperations: ExecutionContext
}
