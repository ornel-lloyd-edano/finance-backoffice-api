package tech.pegb.backoffice.dao.document.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.DaoErrorHandler
import tech.pegb.backoffice.dao.document.couchbase

import scala.concurrent.Future
import scala.concurrent.duration.Duration

@ImplementedBy(classOf[couchbase.DocumentTransientFileDao])
trait DocumentTransientFileDao extends DaoErrorHandler {

  def readDocumentFile(id: UUID, extendExpiration: Option[Duration]): Future[DaoResponse[Option[Array[Byte]]]]

  def writeDocumentFile(id: UUID, contents: Array[Byte], autoExpiration: Option[Duration]): Future[DaoResponse[UUID]]

  def removeDocumentFile(id: UUID): Future[DaoResponse[Boolean]]
}
