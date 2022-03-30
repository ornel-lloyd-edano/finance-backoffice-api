package tech.pegb.backoffice.domain.document.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.document.dto._
import tech.pegb.backoffice.domain.document.implementation.DocumentMgmtService
import tech.pegb.backoffice.domain.document.model.Document
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[DocumentMgmtService])
trait DocumentManagement extends BaseService {

  def getDocument(id: UUID): Future[ServiceResponse[Document]]

  def getDocumentFile(id: UUID): Future[ServiceResponse[DocumentFileToRead]]

  def getDocumentsByCriteria(criteria: DocumentCriteria, ordering: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Document]]]

  def countDocumentsByCriteria(criteria: DocumentCriteria): Future[ServiceResponse[Int]]

  def createDocument(document: DocumentToCreate): Future[ServiceResponse[Document]]

  def uploadDocumentFile(
    documentId: UUID,
    content: Array[Byte],
    uploadedBy: String,
    uploadedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Document]]

  def upsertBusinessUserDocument(
    document: DocumentToCreate,
    content: Array[Byte],
    uploadedBy: String,
    uploadedAt: LocalDateTime): Future[ServiceResponse[Document]]

  def persistDocument(documentId: UUID, persistedBy: String, persistedAt: LocalDateTime): Future[ServiceResponse[Unit]]

  def approveDocument(documentToApprove: DocumentToApprove): Future[ServiceResponse[Document]]

  // per Lloyd's request
  def approveDocumentByInternalId(
    id: Int,
    approvedAt: LocalDateTime,
    approvedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

  def rejectDocument(documentToReject: DocumentToReject): Future[ServiceResponse[Document]]

}
