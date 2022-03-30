package tech.pegb.backoffice.application.datatransfer

import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject._
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.document.abstraction.{DocumentImmutableFileDao, DocumentTransientFileDao}
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.util.{ExecutionContexts, WithExecutionContexts}

import scala.concurrent.duration._
import scala.concurrent.Future

@Singleton
class CouchbaseToHdfsDocMover @Inject() (
    executionContexts: WithExecutionContexts,
    documentTransientFileDao: DocumentTransientFileDao,
    documentImmutableFileDao: DocumentImmutableFileDao) extends BaseService with TransientToPersistentDocMoverT {

  val defaultTransientDocExpiration = 3.days

  implicit val ec = executionContexts.blockingIoOperations

  def persistTransientDocument(documentUuidList: UUID*): Future[ServiceResponse[Seq[UUID]]] = {
    logger.info(s"UUID of documents to persist to HDFS: ${documentUuidList.mkString(", ")}")
    (for {
      couchbaseDocs ← EitherT(Future.sequence(documentUuidList.map(docUuid ⇒
        documentTransientFileDao.readDocumentFile(docUuid, Some(defaultTransientDocExpiration)).map(_.map((docUuid, _)))))
        .map(_.toList.sequence[DaoResponse, (UUID, Option[Array[Byte]])].leftMap(_.asDomainError)))
      hdfsInsertResultIds ← EitherT.fromEither[Future](couchbaseDocs.collect {
        case (docId, Some(docContent)) ⇒
          documentImmutableFileDao.writeDocumentFile(docId, docContent, None)
      }.sequence[DaoResponse, UUID].leftMap(_.asDomainError))
    } yield {
      logger.info(s"Successfully persisted documents to HDFS: ${hdfsInsertResultIds.mkString(", ")}")
      hdfsInsertResultIds
    }).value
  }

}
