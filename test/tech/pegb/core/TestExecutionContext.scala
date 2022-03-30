package tech.pegb.core

import tech.pegb.backoffice.util.WithExecutionContexts
import scala.concurrent.ExecutionContext.Implicits.global
object TestExecutionContext extends WithExecutionContexts {
  val genericOperations = global
  val blockingIoOperations = global
}
