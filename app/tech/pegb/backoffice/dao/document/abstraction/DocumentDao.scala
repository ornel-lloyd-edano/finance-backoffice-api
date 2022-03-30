package tech.pegb.backoffice.dao.document.abstraction

import java.sql.Connection
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.document.dto.DocumentToUpdate
import tech.pegb.backoffice.dao.document.dto.{DocumentCriteria, DocumentToCreate}
import tech.pegb.backoffice.dao.document.entity.Document
import tech.pegb.backoffice.dao.document.sql.DocumentSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[DocumentSqlDao])
trait DocumentDao extends Dao {

  def getDocument(id: UUID): DaoResponse[Option[Document]]

  def getDocumentByInternalId(id: Int): DaoResponse[Option[Document]]

  def getDocumentsByCriteria(criteria: DocumentCriteria, ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[Document]]

  def countDocumentsByCriteria(criteria: DocumentCriteria): DaoResponse[Int]

  def createDocument(document: DocumentToCreate)(implicit txnConn: Option[Connection] = None): DaoResponse[Document]

  def updateDocument(id: UUID, documentToUpdate: DocumentToUpdate)(implicit txnConn: Option[Connection] = None): DaoResponse[Option[Document]]

  def delsert(dto: Seq[DocumentToCreate], criteriaToDelete: DocumentCriteria)(implicit txnConn: Option[Connection] = None): DaoResponse[Seq[Document]]

}
