package tech.pegb.backoffice.dao.document.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.document.sql.DocumentSqlDao._

case class DocumentToUpdate(
    documentIdentifier: Option[String] = None,
    purpose: Option[String] = None,
    status: Option[String] = None,
    rejectionReason: Option[String] = None,
    checkedBy: Option[String] = None,
    checkedAt: Option[LocalDateTime] = None,
    fileUploadedBy: Option[String] = None,
    fileUploadedAt: Option[LocalDateTime] = None,
    filePersistedAt: Option[LocalDateTime] = None,
    createdBy: Option[String] = None,
    createdAt: Option[LocalDateTime] = None,
    updatedBy: Option[String] = None,
    updatedAt: Option[LocalDateTime] = None,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)

  documentIdentifier.foreach(x ⇒ append(cDocumentNumber → x))
  purpose.foreach(x ⇒ append(cPurpose → x))
  status.foreach(x ⇒ append(cStatus → x))
  rejectionReason.foreach(x ⇒ append(cRejectionReason → x))
  checkedAt.foreach(x ⇒ append(cCheckedAt → x))
  checkedBy.foreach(x ⇒ append(cCheckedBy → x))
  fileUploadedAt.foreach(x ⇒ append(cFileUploadedAt → x))
  fileUploadedBy.foreach(x ⇒ append(cFileUploadedBy → x))
  filePersistedAt.foreach(x ⇒ append(cFilePersistedAt → x))
  updatedAt.foreach(x ⇒ append(cUpdatedAt → x))
  updatedBy.foreach(x ⇒ append(cUpdatedBy → x))
}

object DocumentToUpdate {
  final val Approved = "approved"
  final val Rejected = "rejected"
}

