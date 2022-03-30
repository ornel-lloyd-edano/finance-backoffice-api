package tech.pegb.backoffice

import java.nio.file.{Files ⇒ JFiles, Paths ⇒ JPaths}
import java.time.ZonedDateTime
import java.util.UUID

import anorm._
import play.api.db.DBApi
import play.api.libs.{Files ⇒ PlayFiles}
import play.api.mvc.MultipartFormData
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.document.dto._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.PaginatedResult
import tech.pegb.backoffice.domain.document.model.{Document, DocumentTypes}

class DocumentIntegrationTest extends PlayIntegrationTest {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  private val baseDocsPath = "/api/documents"

  private val applicationId = 6
  private val applicationUuid = UUID.randomUUID()
  private var docId: UUID = _
  private val resourceName = "/selfie.png"
  private val resourceUri = getClass.getResource(resourceName).toURI
  private val image = JFiles.readAllBytes(JPaths.get(resourceUri))

  private var updatedAt: Option[ZonedDateTime] = None

  "Documents API" should {
    "[non-doc api] create application" in {
      val dbApi = inject[DBApi]
      val db = dbApi.database("backoffice")
      val insertQuery = SQL("INSERT INTO user_applications(id, uuid, user_id, status, stage, created_by, created_at, updated_by, updated_at) " +
        s"VALUES($applicationId, '${applicationUuid.toString}', 1, 'pending', 'new', 'Admin', '2019-01-01 10:30:00', 'Admin', '2019-01-01 10:30:00');")
      db.withTransaction { implicit connection ⇒
        insertQuery.execute()
      }
      succeed
    }

    "create document" in {
      val toCreate = DocumentToCreate(defaultUserUuid, applicationUuid, None, DocumentTypes.Image.toString, None, "for wallet")
      val request = makePostJsonRequest(baseDocsPath, toCreate.toJsonStr).withHeaders(AuthHeader)
      val resp = route(app, request).get
      status(resp) mustBe CREATED
      val createdOrError = contentAsString(resp).as(classOf[DocumentToRead])
      createdOrError.isSuccess mustBe true
      val created = createdOrError.get
      docId = created.id
      created.customerId.get mustBe defaultUserUuid
    }

    "get document" in {
      val request = FakeRequest("GET", s"$baseDocsPath/$docId").withHeaders(AuthHeader)
      val resp = route(app, request).get
      status(resp) mustBe OK
      val docOrError = contentAsString(resp).as(classOf[DocumentToRead])
      docOrError.isSuccess mustBe true
      val doc = docOrError.get
      doc.id mustBe docId
      doc.customerId.get mustBe defaultUserUuid
      updatedAt = doc.updatedAt
    }

    "upload document file" in {
      val fileName = "doc.png"
      val temporaryFile = PlayFiles.SingletonTemporaryFileCreator.create()
      JFiles.write(temporaryFile.path, image)
      val mpfData = MultipartFormData(
        dataParts = Map("json" → Seq(s"""{"updated_at":${updatedAt.map(t ⇒ s""""${t.toString}"""").getOrElse(null)}}""")),
        files = Seq(MultipartFormData.FilePart("docfile", fileName, contentType = None, temporaryFile)),
        badParts = Seq.empty
      )
      val request = FakeRequest("POST", s"/documents/$docId/file") //TODO: Try To handle MultipartForm in Auth
        .withMultipartFormDataBody(mpfData)
      val resp = route(app, request).get
      status(resp) mustBe CREATED
      PlayFiles.SingletonTemporaryFileCreator.delete(temporaryFile)
      val fileIdOrError = contentAsString(resp).as(classOf[DocumentToRead])
      fileIdOrError.isSuccess mustBe true
      val fileId = fileIdOrError.get
      fileId.id mustBe docId
      fileId.customerId.get mustBe defaultUserUuid
      updatedAt = fileId.updatedAt
    }

    "get document file" in {
      val request = FakeRequest("GET", s"$baseDocsPath/$docId/file").withHeaders(AuthHeader)
      val resp = route(app, request).get
      status(resp) mustBe OK
      val fileBytes = contentAsBytes(resp)
      fileBytes.toArray[Byte].sameElements(image.deep)
    }

    "approve document" in {
      val request = makePutJsonRequest(s"$baseDocsPath/$docId/approve", s"""{"updated_at":${updatedAt.map(t ⇒ s""""${t.toString}"""").getOrElse(null)}}""")
      val resp = route(app, request.withHeaders(AuthHeader)).get
      status(resp) mustBe OK
      val approvedOrError = contentAsString(resp).as(classOf[DocumentToRead])
      approvedOrError.isSuccess mustBe true
      val approved = approvedOrError.get
      approved.id mustBe docId
      approved.customerId.get mustBe defaultUserUuid
      approved.status mustBe Document.Approved
      updatedAt = approved.updatedAt
    }

    "create one more document and upload file for it" in {
      val toCreate = DocumentToCreate(defaultUserUuid, applicationUuid, None, DocumentTypes.IdentityCard.toString, None, "for wallet")
      val request = makePostJsonRequest(baseDocsPath, toCreate.toJsonStr).withHeaders(AuthHeader)
      val resp = route(app, request).get
      status(resp) mustBe CREATED
      val createdOrError = contentAsString(resp).as(classOf[DocumentToRead])
      createdOrError.isSuccess mustBe true
      val created = createdOrError.get
      docId = created.id
      created.customerId.get mustBe defaultUserUuid
      val newUpdatedAt = created.updatedAt

      val fileName = "doc.png"
      val temporaryFile = PlayFiles.SingletonTemporaryFileCreator.create()
      JFiles.write(temporaryFile.path, image)
      val mpfData = MultipartFormData(
        dataParts = Map("json" → Seq(s"""{"updated_at":${newUpdatedAt.map(t ⇒ s""""${t.toString}"""").getOrElse(null)}}""")),
        files = Seq(MultipartFormData.FilePart("docfile", fileName, contentType = None, temporaryFile)),
        badParts = Seq.empty
      )
      val fileRequest = FakeRequest("POST", s"/documents/$docId/file").withHeaders(AuthHeader) //TODO: Try To handle MultipartForm in Auth
        .withMultipartFormDataBody(mpfData)
      val fileResp = route(app, fileRequest).get
      status(fileResp) mustBe CREATED
      PlayFiles.SingletonTemporaryFileCreator.delete(temporaryFile)
      val fileIdOrError = contentAsString(fileResp).as(classOf[DocumentToRead])
      fileIdOrError.isSuccess mustBe true
      val fileId = fileIdOrError.get
      fileId.id mustBe docId
      fileId.customerId.get mustBe defaultUserUuid
      updatedAt = fileId.updatedAt
    }

    "get all documents" in {
      val request = FakeRequest("GET", baseDocsPath).withHeaders(AuthHeader)
      val resp = route(app, request).get
      status(resp) mustBe OK
      val docsOrError = contentAsString(resp).as(classOf[PaginatedResult[Map[String, Any]]])
      docsOrError.isSuccess mustBe true
      val docs = docsOrError.get
      docs.total mustBe 2
      docs.results.exists(_.get("id").exists(_.toString == docId.toString)) mustBe true
    }

    "reject document #2 (pre-condition failed)" in {
      val rejectionEntity = RejectionReason("rejection test", None)
      val request = makePutJsonRequest(s"$baseDocsPath/$docId/reject", rejectionEntity.toJsonStr).withHeaders(AuthHeader)
      val resp = route(app, request).get
      status(resp) mustBe PRECONDITION_FAILED
    }

    "reject document #2" in {
      val rejectionEntity = RejectionReason("rejection test", updatedAt)
      val request = makePutJsonRequest(s"$baseDocsPath/$docId/reject", rejectionEntity.toJsonStr).withHeaders(AuthHeader)
      val resp = route(app, request).get
      status(resp) mustBe OK
      val rejectedOrError = contentAsString(resp).as(classOf[DocumentToRead])
      rejectedOrError.isSuccess mustBe true
      val rejected = rejectedOrError.get
      rejected.id mustBe docId
      rejected.customerId.get mustBe defaultUserUuid
      rejected.status mustBe Document.Rejected
    }

  }
}
