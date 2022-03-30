package tech.pegb.backoffice.dao.document.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.DaoErrorHandler
import tech.pegb.backoffice.dao.document.hdfs

@ImplementedBy(classOf[hdfs.DocumentImmutableFileDao])
trait DocumentImmutableFileDao extends DaoErrorHandler {

  def readDocumentFile(id: UUID, subpath: Option[String]): DaoResponse[Option[Array[Byte]]]

  def writeDocumentFile(id: UUID, contents: Array[Byte], subpath: Option[String]): DaoResponse[UUID]

  //used for unit test
  def deleteDocumentFile(id: UUID): DaoResponse[Unit]
}
