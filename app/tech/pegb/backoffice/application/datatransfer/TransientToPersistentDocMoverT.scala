package tech.pegb.backoffice.application.datatransfer

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse

import scala.concurrent.Future
import scala.concurrent.duration.Duration

@ImplementedBy(classOf[CouchbaseToHdfsDocMover])
trait TransientToPersistentDocMoverT {
  val defaultTransientDocExpiration: Duration
  def persistTransientDocument(uuids: UUID*): Future[ServiceResponse[Seq[UUID]]]
}
