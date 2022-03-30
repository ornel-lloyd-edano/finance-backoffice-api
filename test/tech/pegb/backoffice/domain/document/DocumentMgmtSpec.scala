package tech.pegb.backoffice.domain.document

import java.sql.Connection
import java.time.{LocalDateTime}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.{Binding, bind}
import tech.pegb.backoffice.application.{CommunicationService, MockCommunicationServiceImpl}
import tech.pegb.backoffice.dao.Dao.EntityId
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.BusinessUserApplicationDao
import tech.pegb.backoffice.dao.businessuserapplication.entity.BusinessUserApplication
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.dto.UserToInsert
import tech.pegb.backoffice.dao.document.abstraction.{DocumentDao, DocumentImmutableFileDao, DocumentTransientFileDao}
import tech.pegb.backoffice.dao.document.entity.Document
import tech.pegb.backoffice.domain.ErrorCodes
import tech.pegb.backoffice.domain.application.model.ApplicationTypes
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.Stages
import tech.pegb.backoffice.domain.businessuserapplication.dto.BusinessUserApplicationCriteria
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.dto.{DocumentToApprove, DocumentToCreate, DocumentToReject, DocumentToUpload, DocumentCriteria}
import tech.pegb.backoffice.domain.document.model.{DocumentStatuses, DocumentTypes}
import tech.pegb.backoffice.mapping.domain.dao.businessuserapplication.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.document.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, Utils}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.PegBNoDbTestApp

class DocumentMgmtSpec
  extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val activeUser = UserToInsert(
    userName = "unit-test-active",
    password = None,
    `type` = None,
    tier = None,
    segment = None,
    subscription = None,
    email = None,
    status = "WAITING_FOR_ACTIVATION",
    createdAt = Utils.nowAsLocal(),
    createdBy = "root")
  private val passiveUser = activeUser.copy(
    userName = "unit-test-passive",
    status = "PASSIVE")
  private var activeUserUuid: String = _

  private var activeUserDocId: UUID = _
  private var passiveUserDocId: UUID = _
  private var user3DocId: UUID = _
  private var user4DocId: UUID = _
  private var user4DocInternalId: Int = _
  private val validDocData = Seq[Byte](4, 3, 2, 1).toArray

  val userDao: UserDao = stub[UserDao]
  val docsDao: DocumentDao = stub[DocumentDao]
  val docFilesDao: DocumentTransientFileDao = stub[DocumentTransientFileDao]
  val immutableDocDao = stub[DocumentImmutableFileDao]
  val applicationDao = stub[BusinessUserApplicationDao]

  override def additionalBindings: Seq[Binding[_]] = {
    super.additionalBindings ++ Seq(
      bind[DocumentDao].toInstance(docsDao),
      bind[DocumentTransientFileDao].toInstance(docFilesDao),
      bind[DocumentImmutableFileDao].toInstance(immutableDocDao),
      bind[BusinessUserApplicationDao].toInstance(applicationDao),
      bind[UserDao].toInstance(userDao))
  }

  "DocumentMgmt getDocument" should {
    "return Future[Right[Document]] if document metadata was found" in {
      val service = inject[DocumentManagement]
      val mockId = UUID.randomUUID()
      val mockDoc = Document(
        id = 1,
        uuid = activeUserDocId,
        docType = "type1",
        purpose = "application",
        status = "ongoing",
        createdBy = "test_user",
        createdAt = LocalDateTime.now)
      (docsDao.getDocument _).when(mockId).returns(Right(Some(mockDoc)))

      (userDao.getUser _).when("").returns(Right(None))

      whenReady(service.getDocument(mockId)) { resp ⇒
        resp.isRight mustBe true
      }
    }

    "return Future[Left[NotFound]] if document metadata was not found" ignore { //TODO: fix test case to use scalamock and unignore
      val service = inject[DocumentManagement]
      whenReady(service.getDocument(UUID.randomUUID())) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.NotFound) mustBe true
      }
    }

    "return Future[Left[InactiveCustomer]] if document metadata was found but customer is no longer active" ignore {
      val service = inject[DocumentManagement]
      whenReady(service.getDocument(passiveUserDocId)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.ValidationFailed) mustBe true
      }
    }
  }

  "DocumentMgmt uploadDocumentFile" should {
    "return Future[Right[Document]] if document metadata and matching document file was found" ignore {
      val service = inject[DocumentManagement]
      whenReady(service.uploadDocumentFile(activeUserDocId, validDocData, "unit-test", Utils.nowAsLocal(), None)) { resp ⇒
        resp.isRight mustBe true
        resp.exists(_.fileUploadedAt.isDefined) mustBe true
        resp.exists(_.fileUploadedBy.isDefined) mustBe true
      }
    }

    "upload more test docs" ignore {
      val service = inject[DocumentManagement]
      whenReady(service.uploadDocumentFile(user4DocId, validDocData, "unit-test", Utils.nowAsLocal(), None)) { resp ⇒
        resp.isRight mustBe true
        resp.exists(_.fileUploadedAt.isDefined) mustBe true
        resp.exists(_.fileUploadedBy.isDefined) mustBe true
      }
    }

    "return Future[Left[NotFound]] if document file was not found" ignore {
      val service = inject[DocumentManagement]
      whenReady(service.uploadDocumentFile(UUID.randomUUID(), validDocData, "unit-test", Utils.nowAsLocal(), None)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.NotFound)
      }
    }

    "return Future[Left[InactiveCustomer]] if document file was found but customer is no longer active" ignore {
      val service = inject[DocumentManagement]
      whenReady(service.uploadDocumentFile(passiveUserDocId, validDocData, "unit-test", Utils.nowAsLocal(), None)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.ValidationFailed)
      }
    }
  }

  "DocumentMgmt getDocumentFile" should {
    "return Future[Right[Document]] if document metadata and matching document file was found" ignore {
      val service = inject[DocumentManagement]
      whenReady(service.getDocumentFile(activeUserDocId)) { resp ⇒
        resp.isRight mustBe true
        resp.exists(_.content sameElements validDocData) mustBe true
      }
    }

    "return Future[Left[NotFound]] if document file was not found" ignore {
      val service = inject[DocumentManagement]
      whenReady(service.getDocumentFile(UUID.randomUUID())) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.NotFound)
      }
    }

    "return Future[Left[MetaDataNotFound]] if document file was found but metadata was missing" ignore {
      val service = inject[DocumentManagement]
      whenReady(service.getDocumentFile(passiveUserDocId)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.NotFound)
      }
    }

    "return Future[Left[InactiveCustomer]] if document file was found but customer is no longer active" ignore {
      val service = inject[DocumentManagement]
      whenReady(service.getDocumentFile(passiveUserDocId)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.ValidationFailed)
      }
    }
  }

  "DocumentMgmt getDocumentsByCriteria" should {
    "return Future[Right[Seq[Document]]] for all documents metadata of active and waiting_for_activation customers if no criteria given" ignore {
      val service = inject[DocumentManagement]
      val criteria = DocumentCriteria.Empty
      whenReady(service.getDocumentsByCriteria(criteria, Seq.empty, None, None)) { resp ⇒
        resp.isRight mustBe true
        resp.map(_.size) mustBe Right(3)
        resp.map(_.forall(_.status == DocumentStatuses.Pending)) mustBe Right(true)
      }
    }

    "return Future[Right[Seq[Document]]] for all documents metadata of active and waiting_for_activation customers which met the given search criteria" ignore {
      val service = inject[DocumentManagement]
      val customerId = UUID.fromString(activeUserUuid)
      val criteria = DocumentCriteria.Empty.copy(customerId = Some(UUIDLike.apply(customerId.toString)))
      whenReady(service.getDocumentsByCriteria(criteria, Seq.empty, None, None)) { resp ⇒
        resp.isRight mustBe true
        resp.map(_.size) mustBe Right(1)
        resp.map(_.forall(_.customerId == Some(customerId))) mustBe Right(true)
      }
    }

    "return Future[Right[Seq[Document]]] for a subset of matching documents metadata because of limit and offset" ignore {
      val service = inject[DocumentManagement]
      val criteria = DocumentCriteria.Empty
      val limit = 1
      whenReady(service.getDocumentsByCriteria(criteria, Seq.empty, Some(limit), Some(1))) { resp ⇒
        resp.isRight mustBe true
        resp.map(_.size) mustBe Right(1) // respect limit/offset
        resp.map(_.forall(_.status == DocumentStatuses.Pending)) mustBe Right(true)
      }
    }
  }

  "DocumentMgmt countDocumentsByCriteria" should {
    "return Future[Right[Int]] which is the total count of all documents metadata which met the given search criteria" ignore {
      val service = inject[DocumentManagement]
      val criteria = DocumentCriteria.Empty.copy(documentType = Some(DocumentTypes.fromString("EMIRATES_ID")))
      whenReady(service.countDocumentsByCriteria(criteria)) { resp ⇒
        resp.isRight mustBe true
        resp mustBe Right(3)
      }
    }
  }

  "DocumentMgmt approveDocument" should {
    "return Future[Left[NotFound]] if document metadata to approve was not found" ignore {
      val service = inject[DocumentManagement]
      val toApprove = DocumentToApprove(UUID.randomUUID(), "root", Utils.nowAsLocal(), None)
      whenReady(service.approveDocument(toApprove)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.NotFound) mustBe true
      }
    }

    "return Future[Left[NotFound]] if document file to approve was not found" ignore {
      val service = inject[DocumentManagement]
      val toApprove = DocumentToApprove(user3DocId, "root", Utils.nowAsLocal(), None)
      whenReady(service.approveDocument(toApprove)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.NotFound) mustBe true
      }
    }

    "return Future[Left[InactiveCustomer]] if document to approve was for a customer which is no longer active" ignore {
      val service = inject[DocumentManagement]
      val toApprove = DocumentToApprove(passiveUserDocId, "root", Utils.nowAsLocal(), None)
      whenReady(service.approveDocument(toApprove)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.NotFound) mustBe true
      }
    }

    "return Future[Right[Document]] with approved status, checkedBy and checkedAt not empty" ignore {
      val service = inject[DocumentManagement]
      val communicationService = inject[CommunicationService].asInstanceOf[MockCommunicationServiceImpl]
      communicationService.lastNotification.isDefined mustBe false
      val toApprove = DocumentToApprove(activeUserDocId, "root", Utils.nowAsLocal(), None)
      whenReady(service.approveDocument(toApprove)) { resp ⇒
        resp.isRight mustBe true
        resp.exists(_.id == activeUserDocId) mustBe true
        resp.map(_.checkedAt) mustBe Right(Some(toApprove.approvedAt))
        resp.map(_.checkedBy) mustBe Right(Some(toApprove.approvedBy))
        communicationService.lastNotification.isDefined mustBe true
      }
    }

    "return Future[Left[NotAllowed]] if document to approve is no longer pending" ignore {
      val service = inject[DocumentManagement]
      val toApprove = DocumentToApprove(activeUserDocId, "root", Utils.nowAsLocal(), None)
      whenReady(service.approveDocument(toApprove)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.ValidationFailed) mustBe true
      }
    }
  }

  "DocumentMgmt approveDocument" should {
    "successfully approve document based on internal id" ignore {
      val service = inject[DocumentManagement]
      val communicationService = inject[CommunicationService].asInstanceOf[MockCommunicationServiceImpl]
      val lastNotification = communicationService.lastNotification
      whenReady(service.approveDocumentByInternalId(user4DocInternalId, Utils.nowAsLocal(), "root", None)) { resp ⇒
        resp.isRight mustBe true
        communicationService.lastNotification == lastNotification mustBe false
      }
    }
  }

  "DocumentMgmt rejectDocument" should {
    val rejectionReason = "Just to test rejection"

    "return Future[Left[NotFound]] if document metadata to reject was not found" ignore {
      val service = inject[DocumentManagement]
      val toReject = DocumentToReject(UUID.randomUUID(), "root", Utils.nowAsLocal(), rejectionReason, None)
      whenReady(service.rejectDocument(toReject)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.NotFound) mustBe true
      }
    }

    "return Future[Left[InactiveCustomer]] if document to reject was for a customer which is no longer active" ignore {
      val service = inject[DocumentManagement]
      val toReject = DocumentToReject(passiveUserDocId, "root", Utils.nowAsLocal(), rejectionReason, None)
      whenReady(service.rejectDocument(toReject)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.ValidationFailed) mustBe true
      }
    }

    "return Future[Right[Document]] even if document file to reject was not found" ignore {
      val service = inject[DocumentManagement]
      val communicationService = inject[CommunicationService].asInstanceOf[MockCommunicationServiceImpl]
      val lastNotification = communicationService.lastNotification
      // using user3DocId as activeUserDoc was already approved
      val toReject = DocumentToReject(user3DocId, "root", Utils.nowAsLocal(), rejectionReason, None)
      whenReady(service.rejectDocument(toReject)) { resp ⇒
        resp.isRight mustBe true
        resp.exists(_.id == user3DocId) mustBe true
        resp.map(_.rejectionReason) mustBe Right(Some(rejectionReason))
        communicationService.lastNotification == lastNotification mustBe false
      }
    }

    "return Future[Left[NotAllowed]] if document to reject is no longer pending" ignore {
      val service = inject[DocumentManagement]
      val toReject = DocumentToReject(user3DocId, "root", Utils.nowAsLocal(), rejectionReason, None)
      whenReady(service.rejectDocument(toReject)) { resp ⇒
        resp.isLeft mustBe true
        resp.swap.exists(_.code == ErrorCodes.ValidationFailed) mustBe true
      }
    }

    "return Future[Right[Document]] if document previously did not exist in upsertBusinessUserDocument" in {
      val service = inject[DocumentManagement]
      val docToCreate = DocumentToCreate(
        customerId = Some(UUID.randomUUID()),
        applicationId = Some(UUID.randomUUID()),
        documentType = DocumentTypes.Image,
        documentIdentifier = None,
        fileName = Some("my_file01.jpg"),
        purpose = "application requirement",
        createdBy = "test_user",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0))
      val fileContent = "some long file contents...".getBytes

      val mockDaoBusinessUserApplic = BusinessUserApplication(
        id = 1,
        uuid = docToCreate.applicationId.get.toString,
        businessName = "businessName",
        brandName = "brandName",
        businessCategory = "businessCategory",
        stage = "stage",
        status = "ongoing",
        userTier = "userTier",
        businessType = "businessType",
        registrationNumber = "registrationNumber",
        taxNumber = None,
        registrationDate = None,
        explanation = None,
        userId = Some(1),
        submittedBy = None,
        submittedAt = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = "test_user",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        updatedBy = None,
        updatedAt = None)
      (applicationDao.getBusinessUserApplicationByCriteria _)
        .when(BusinessUserApplicationCriteria(uuid = docToCreate.applicationId.map(_.toUUIDLike)).asDao, None, None, None)
        .returns(Right(Seq(mockDaoBusinessUserApplic)))

      (docsDao.getDocumentsByCriteria _)
        .when(DocumentCriteria(businessApplicationId = docToCreate.applicationId).asDao(), None, None, None)
        .returns(Right(Nil))

      val mockTxn = mock[java.sql.Connection]
      (docsDao.startTransaction _).when().returns(Right(mockTxn))

      val mockDaoDocument = Document(
        id = 1,
        uuid = UUID.randomUUID(),
        customerId = docToCreate.customerId,
        walletApplicationId = None,
        businessUserApplicationId = docToCreate.applicationId,
        documentType = docToCreate.documentType.toString,
        documentIdentifier = None,
        purpose = "application requirement",
        status = "pending",
        rejectionReason = None,
        checkedBy = None,
        checkedAt = None,
        fileName = docToCreate.fileName,
        fileUploadedBy = None,
        fileUploadedAt = None,
        createdBy = docToCreate.createdBy,
        createdAt = docToCreate.createdAt,
        updatedBy = None,
        updatedAt = None)

      import tech.pegb.backoffice.dao.document.dto.{DocumentToCreate ⇒ DaoDocumentToCreate}

      (docsDao.createDocument(_: DaoDocumentToCreate)(_: Option[Connection]))
        .when(docToCreate.asDao(ApplicationTypes.BusinessUserApplication), Some(mockTxn))
        .returns(Right(mockDaoDocument))

      (docFilesDao.writeDocumentFile _)
        .when(mockDaoDocument.uuid, fileContent, Some(inject[AppConfig].TransientFileExpiryTime))
        .returns(Right(mockDaoDocument.uuid).toFuture)

      import tech.pegb.backoffice.dao.document.dto.{DocumentToUpdate ⇒ DaoDocumentToUpdate}
      val mockDocToUpdateInput = DocumentToUpload(
        fileUploadedBy = docToCreate.createdBy,
        fileUploadedAt = docToCreate.createdAt,
        status = Some(DocumentStatuses.Ongoing),
        lastUpdatedAt = mockDaoDocument.updatedAt)
      val mockDaoDocumentUpdated = mockDaoDocument.copy(
        fileUploadedBy = Some(mockDocToUpdateInput.fileUploadedBy),
        fileUploadedAt = Some(mockDocToUpdateInput.fileUploadedAt),
        status = mockDocToUpdateInput.status.map(_.toString).getOrElse(""))
      (docsDao.updateDocument(_: UUID, _: DaoDocumentToUpdate)(_: Option[Connection]))
        .when(mockDaoDocument.uuid, mockDocToUpdateInput.asDao, Some(mockTxn))
        .returns(Right(Option(mockDaoDocumentUpdated)))

      import tech.pegb.backoffice.dao.businessuserapplication.dto.BusinessUserApplicationToUpdate
      val mockBuToUpdateInput = BusinessUserApplicationToUpdate(
        stage = Some(Stages.Docs),
        updatedAt = docToCreate.createdAt,
        updatedBy = docToCreate.createdBy,
        lastUpdatedAt = mockDaoBusinessUserApplic.updatedAt)
      val mockUpdatedBuApplication = mockDaoBusinessUserApplic.copy(
        stage = mockBuToUpdateInput.stage.get,
        updatedAt = Some(mockBuToUpdateInput.updatedAt),
        updatedBy = Some(mockBuToUpdateInput.updatedBy))

      (applicationDao.updateBusinessUserApplication(_: EntityId, _: BusinessUserApplicationToUpdate)(_: Option[Connection]))
        .when(docToCreate.applicationId.get.asEntityId, mockBuToUpdateInput, Some(mockTxn))
        .returns(Right(Some(mockUpdatedBuApplication)))

      (docsDao.endTransaction(_: java.sql.Connection)).when(mockTxn).returns(Right(()))

      import tech.pegb.backoffice.domain.document.model.{Document ⇒ DomainDocument}
      val expected = DomainDocument(
        id = mockDaoDocument.uuid,
        customerId = docToCreate.customerId,
        applicationId = docToCreate.applicationId,
        documentName = docToCreate.fileName,
        documentType = docToCreate.documentType,
        documentIdentifier = None,
        purpose = docToCreate.purpose,
        status = DocumentStatuses.Ongoing,
        rejectionReason = None,
        checkedBy = None,
        checkedAt = None,
        createdBy = docToCreate.createdBy,
        createdAt = docToCreate.createdAt,
        fileUploadedAt = Some(docToCreate.createdAt),
        fileUploadedBy = Some(docToCreate.createdBy),
        updatedAt = None)

      whenReady(service.upsertBusinessUserDocument(docToCreate, fileContent, "test_user", LocalDateTime.of(2019, 1, 1, 0, 0))) { resp ⇒

        resp.right.get mustBe expected
      }
    }
  }

}
