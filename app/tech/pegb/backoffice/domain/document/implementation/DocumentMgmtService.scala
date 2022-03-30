package tech.pegb.backoffice.domain.document.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.application.datatransfer.TransientToPersistentDocMoverT
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.BusinessUserApplicationDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.document.abstraction.{DocumentDao, DocumentImmutableFileDao, DocumentTransientFileDao}
import tech.pegb.backoffice.dao.document.dto.DocumentToUpdate
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.application.model.ApplicationTypes
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.Stages
import tech.pegb.backoffice.domain.businessuserapplication.dto.{BusinessUserApplicationCriteria, BusinessUserApplicationToUpdateStage}
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.dto._
import tech.pegb.backoffice.domain.document.model.{Document, DocumentStatuses, DocumentTypes}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.document.Implicits.DocumentAdapter
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.businessuserapplication.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.document.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.types.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Left, Right}

@Singleton
class DocumentMgmtService @Inject() (
    config: AppConfig,
    executionContexts: WithExecutionContexts,
    docsDao: DocumentDao,
    transientDocDao: DocumentTransientFileDao,
    immutableDocDao: DocumentImmutableFileDao,
    docsMover: TransientToPersistentDocMoverT,
    userDao: UserDao,
    typesDao: TypesDao,
    applicationDao: BusinessUserApplicationDao) extends DocumentManagement {
  implicit val ec: ExecutionContext = executionContexts.genericOperations

  typesDao.bulkUpsert(
    existingKind = DocumentStatuses.toString,
    updatedAt = LocalDateTime.now(),
    updatedBy = "backoffice_api",
    lastUpdatedAt = None,
    newValues = DocumentStatuses.toSeq.map(_.asDao),
    disableOptimisticLockCheck = true)

  typesDao.bulkUpsert(
    existingKind = DocumentTypes.toString,
    updatedAt = LocalDateTime.now(),
    updatedBy = "backoffice_api",
    lastUpdatedAt = None,
    newValues = DocumentTypes.toSeq.map(_.asDao),
    disableOptimisticLockCheck = true)

  override def getDocument(id: UUID): Future[ServiceResponse[Document]] = Future {
    for {
      maybeDoc ← docsDao.getDocument(id).asServiceResponse
      doc ← maybeDoc.toRight(notFoundError(s"Document $id was not found"))

      maybeUser ← userDao.getUser(doc.customerId.map(_.toString).getOrElse("")).asServiceResponse

      _ ← maybeUser match {
        case Some(user) if user.status.contains(config.PassiveUserStatus) ⇒
          Left(validationError(s"User ${doc.customerId} for document $id is passive"))
        case _ ⇒
          Right(())
      }

    } yield {
      val result = doc.asDomain
      result.validate.left.map(_.foreach(error ⇒
        logger.warn(s"Inconsistent data found for document $id. ${error.message}")))
      result
    }
  }

  override def getDocumentFile(id: UUID): Future[ServiceResponse[DocumentFileToRead]] = {
    (for {
      maybeDoc ← EitherT.fromEither[Future](docsDao.getDocument(id).asServiceResponse(_.map(_.asDomain)))
      doc ← EitherT.fromOption[Future](
        maybeDoc,
        notFoundError(s"Document file for doc $id exists, but doc itself is missing"))

      _ ← EitherT {
        val result = doc.customerId match {
          case Some(customerId) ⇒
            (for {
              maybeUser ← EitherT.fromEither[Future](userDao.getUser(customerId.toString).asServiceResponse)
              user ← EitherT.fromOption[Future](
                maybeUser,
                notFoundError(s"File and doc $id exist, but user ${doc.customerId} does not"))
              result ← EitherT.fromEither[Future] {
                if (user.status.contains(config.PassiveUserStatus)) {
                  Left(validationError(s"User ${doc.customerId} for document $id is passive"))
                } else {
                  Right(())
                }
              }
            } yield {
              result
            }).value
          case None ⇒
            Future.successful(Right(()))
        }
        result
      }

      maybeFile ← if (doc.status.isApproved) {
        EitherT.fromEither[Future] {
          immutableDocDao.readDocumentFile(doc.id, None).asServiceResponse
        }
      } else {
        EitherT(transientDocDao.readDocumentFile(id, extendExpiration = None).map(_.asServiceResponse))
      }
      file ← EitherT.fromOption[Future](
        maybeFile,
        notFoundError(s"Unable to get this file. Document [${doc.id}] was not found"))
    } yield DocumentFileToRead(id.toString, file, doc.documentName, doc.getFileType)).value
  }

  override def getDocumentsByCriteria(
    criteria: DocumentCriteria,
    ordering: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Document]]] = Future {

    criteria.validate.fold(
      Left(_),
      _ ⇒ {
        val results = docsDao.getDocumentsByCriteria(criteria.asDao(), ordering.asDao, limit, offset)
          .asServiceResponse(_.map(_.asDomain))
        results.map(_.foreach(doc ⇒ doc.validate.left.map(_.foreach(error ⇒ {
          logger.warn(s"Inconsistent data found for document ${doc.id}. ${error.message}")
        }))))
        results
      })

  }

  override def countDocumentsByCriteria(criteria: DocumentCriteria): Future[ServiceResponse[Int]] = Future {
    docsDao.countDocumentsByCriteria(criteria.asDao()).asServiceResponse
  }

  override def createDocument(document: DocumentToCreate): Future[ServiceResponse[Document]] = Future {
    for {
      uId ← document.customerId.toRight(validationError(s"No customer id in the document"))
      maybeUser ← userDao.getUser(uId.toString).asServiceResponse
      user ← maybeUser.toRight(notFoundError(s"Customer $uId was not found"))
      _ ← document.validate

      created ← if (user.status.contains(config.PassiveUserStatus)) {
        Left(validationError(s"Customer $uId is in status ${config.PassiveUserStatus}"))
      } else {
        docsDao.createDocument(document.asDao()).asServiceResponse
      }
    } yield created.asDomain
  }

  override def uploadDocumentFile(
    documentId: UUID,
    content: Array[Byte],
    uploader: String,
    uploadedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Document]] = {

    (for {
      _ ← EitherT.fromEither[Future](validateDocAndCustomerBeforeUpdate(documentId))
      _ ← EitherT {
        transientDocDao.writeDocumentFile(id = documentId, autoExpiration = Some(config.TransientFileExpiryTime), contents = content)
          .map(_.asServiceResponse)
      }
      maybeUpdatedDoc ← EitherT.fromEither[Future] {
        val patch = DocumentToUpdate(
          fileUploadedBy = Some(uploader),
          fileUploadedAt = Some(uploadedAt),
          lastUpdatedAt = lastUpdatedAt)
        docsDao.updateDocument(documentId, patch).asServiceResponse(_.map(_.asDomain))
      }
      updatedDoc ← EitherT.fromOption[Future](
        maybeUpdatedDoc,
        notFoundError(s"Unable to upload this file. Document [$documentId] was not found"))
    } yield updatedDoc).value
  }

  override def upsertBusinessUserDocument(
    document: DocumentToCreate,
    content: Array[Byte],
    uploadedBy: String,
    uploadedAt: LocalDateTime): Future[ServiceResponse[Document]] = {

    val applicType = ApplicationTypes.BusinessUserApplication

    (for {
      buApplic ← {
        logger.info(s"upsertBusinessUserDocument step 1: getBusinessUserApplicationByCriteria")
        EitherT.fromEither[Future](applicationDao.getBusinessUserApplicationByCriteria(
          BusinessUserApplicationCriteria(uuid = document.applicationId.map(_.toUUIDLike)).asDao, None, None, None)
          .map(_.headOption)
          .fold(
            _.asDomainError.toLeft,
            {
              case Some(result) ⇒ Right(result)
              case None ⇒ Left(notFoundError(s"Business user application with id [${document.applicationId}] is not found for this document"))
            }))
      }

      maybeExistingDocs ← {
        logger.info(s"upsertBusinessUserDocument step 2: getDocumentsByCriteria")
        EitherT(if (buApplic.status === "ongoing") {
          getDocumentsByCriteria(DocumentCriteria(businessApplicationId = document.applicationId), Nil, None, None)
        } else {
          Left(validationError(s"Not allowed to change the documents for application with id [${document.applicationId}]")).toFuture
        })
      }

      txnConn ← {
        logger.info(s"upsertBusinessUserDocument step 3: startTransaction")
        EitherT.fromEither[Future](docsDao.startTransaction.asServiceResponse)
      }

      insertedDoc ← {
        logger.info(s"upsertBusinessUserDocument step 4: delsert")
        EitherT.fromEither[Future](if (maybeExistingDocs.nonEmpty) {
          val criteria = DocumentCriteria(
            businessApplicationId = document.applicationId,
            filename = document.fileName,
            documentType = Some(document.documentType))

          docsDao.delsert(
            dto = Seq(document.asDao(applicType)),
            criteriaToDelete = criteria.asDao())(txnConn.some)
            .asServiceResponse(_.map(_.asDomain).headOption)
            .fold(
              _.toLeft,
              {
                case None ⇒
                  Left(unknownError("document is supposed to be created but result is empty"))
                case Some(document) ⇒ Right(document)
              })
        } else {
          docsDao.createDocument(document.asDao(applicType))(txnConn.some).asServiceResponse(_.asDomain)
        })
      }

      _ ← {
        logger.info(s"upsertBusinessUserDocument step 5: writeDocumentFile")
        EitherT(transientDocDao.writeDocumentFile(id = insertedDoc.id, autoExpiration = Some(config.TransientFileExpiryTime), contents = content)
          .map(_.asServiceResponse))
      }

      updatedDoc ← {
        logger.info(s"upsertBusinessUserDocument step 6: updateDocument")
        EitherT.fromEither[Future] {
          val patch = DocumentToUpload(
            fileUploadedBy = uploadedBy,
            fileUploadedAt = uploadedAt,
            status = Some(DocumentStatuses.Ongoing),
            lastUpdatedAt = insertedDoc.updatedAt)
          docsDao.updateDocument(insertedDoc.id, patch.asDao)(txnConn.some).asServiceResponse(_.map(_.asDomain))
            .fold(_.toLeft, {
              case Some(updated) ⇒ Right(updated)
              case None ⇒ Left(unknownError(s"Upload error. Document with id [${insertedDoc.id}] was not found during update"))
            })
        }
      }

      _ ← {
        logger.info(s"upsertBusinessUserDocument step 7: updateBusinessUserApplication")
        EitherT.fromEither[Future]({
          document.applicationId match {
            case Some(applicationId) ⇒
              applicationDao.updateBusinessUserApplication(
                applicationId.asEntityId,
                BusinessUserApplicationToUpdateStage(
                  Stages.Docs,
                  document.createdBy,
                  document.createdAt).asDao(buApplic.updatedAt))(txnConn.some).asServiceResponse
            case None ⇒
              Left(validationError("Failed to create document for business user application because application_id is missing from the request"))
          }

        })
      }

      _ ← {
        logger.info(s"upsertBusinessUserDocument step 8: endTransaction")
        EitherT.fromEither[Future](docsDao.endTransaction(txnConn).asServiceResponse)
      }

    } yield {
      updatedDoc
    }).value
  }

  def persistDocument(
    documentId: UUID,
    persistedBy: String,
    persistedAt: LocalDateTime): Future[ServiceResponse[Unit]] = {

    (for {
      lastUpdatedAt ← EitherT.fromEither[Future](docsDao.getDocument(documentId)
        .fold(
          _.asDomainError.toLeft,
          {
            case Some(doc) ⇒ Right(doc.updatedAt)
            case None ⇒ Left(notFoundError(s"Failed to persist missing document with id [$documentId]"))
          }))

      _ ← EitherT(docsMover.persistTransientDocument(documentId)).map(_.headOption)

      _ ← {
        val dto = DocumentToPersist(
          documentId = documentId,
          persistedBy = persistedBy, persistedAt = persistedAt,
          lastUpdatedAt = lastUpdatedAt).asDao
        val result = docsDao.updateDocument(documentId, dto).fold(err ⇒ {
          logger.warn(s"Document with id [$documentId] is persisted in HDFS but failed to update metadata in MYSQL. Fix this with batch job.")
          Left(err.asDomainError)
        }, _ ⇒ Right(()))
        EitherT.fromEither[Future](result)
      }
    } yield {
      ()
    }).value
  }

  override def approveDocument(documentToApprove: DocumentToApprove): Future[ServiceResponse[Document]] = {
    val docId = documentToApprove.id
    (for {
      maybeDocFile ← EitherT(transientDocDao.readDocumentFile(docId, extendExpiration = None).map(_.asServiceResponse))
      docFile ← EitherT.fromOption[Future](
        maybeDocFile,
        notFoundError(s"Unable to approve document. Document [$docId] is missing"))
      _ ← EitherT.fromEither[Future] {
        immutableDocDao.writeDocumentFile(docId, docFile, None).asServiceResponse
      }
      approved ← EitherT(updateDocument(docId, documentToApprove.asDao, requireFile = true))
    } yield approved).value
  }

  override def approveDocumentByInternalId(
    id: Int,
    approvedAt: LocalDateTime,
    approvedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    (for {
      maybeDoc ← EitherT.fromEither[Future](docsDao.getDocumentByInternalId(id).asServiceResponse)
      doc ← EitherT.fromOption[Future](maybeDoc, notFoundError(s"Document $id was not found"))
      _ ← EitherT(approveDocument(DocumentToApprove(doc.uuid, approvedBy, approvedAt, lastUpdatedAt)))
    } yield ()).value
  }

  override def rejectDocument(documentToReject: DocumentToReject): Future[ServiceResponse[Document]] = {
    val docId = documentToReject.id
    (for {
      rejected ← EitherT(updateDocument(docId, documentToReject.asDao, requireFile = false))
      _ ← EitherT(transientDocDao.removeDocumentFile(docId).map(_.asServiceResponse.map(_ ⇒ ())))
    } yield rejected).value
  }

  protected def updateDocument(
    docId: UUID,
    toUpdate: DocumentToUpdate,
    requireFile: Boolean): Future[ServiceResponse[Document]] = {
    (for {
      _ ← if (requireFile) {
        // TODO: add .exists(docId) to API
        EitherT(transientDocDao.readDocumentFile(docId, None)
          .map(maybeFile ⇒ maybeFile match {
            case Right(None) ⇒ Left(unknownError(s"Document [$docId] not found"))
            case other ⇒ other.asServiceResponse.map(_ ⇒ ())
          })(ec))
      } else {
        EitherT.fromEither[Future](Right(()))
      }
      _ ← EitherT.fromEither[Future](validateDocAndCustomerBeforeUpdate(docId))
      maybeUpdatedDoc ← EitherT.fromEither[Future](docsDao.updateDocument(docId, toUpdate).asServiceResponse(_.map(_.asDomain)))
      updatedDoc ← EitherT.fromOption[Future](maybeUpdatedDoc, notFoundError(s"Document $docId was not found"))
    } yield {
      updatedDoc
    }).value
  }

  protected def validateDocAndCustomerBeforeUpdate(docId: UUID): ServiceResponse[Document] = {
    for {
      maybeDoc ← docsDao.getDocument(docId).asServiceResponse
      doc ← maybeDoc.toRight(notFoundError(s"Doc $docId was not found"))
      _ ← if (doc.status === Document.Pending) {
        Right(())
      } else {
        Left(validationError(s"Doc $docId status is `${doc.status}`, required is `${Document.Pending}`"))
      }
      customerId ← doc.customerId.toRight(validationError(s"No customer id in the document"))
      maybeCustomer ← userDao.getUser(customerId.toString).asServiceResponse
      customer ← maybeCustomer.toRight {
        notFoundError(s"Doc $docId was not found, but user $customerId it belongs to is missing")
      }
      _ ← if (customer.status === config.PassiveUserStatus) {
        Left(validationError(s"Doc $docId belongs to customer $customerId which is `${config.PassiveUserStatus}`"))
      } else {
        Right(())
      }
    } yield doc.asDomain
  }

}
