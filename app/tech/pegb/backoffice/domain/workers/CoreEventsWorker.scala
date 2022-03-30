package tech.pegb.backoffice.domain.workers

import akka.actor.Cancellable
import tech.pegb.backoffice.domain.BaseService

abstract class CoreEventsWorker extends BaseService {

  protected def run(): Cancellable

  run()

}
