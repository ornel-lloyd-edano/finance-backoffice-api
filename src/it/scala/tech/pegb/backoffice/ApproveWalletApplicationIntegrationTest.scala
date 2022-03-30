package tech.pegb.backoffice

import java.nio.file.{Files ⇒ JFiles, Paths ⇒ JPaths}
import java.time._
import java.util.UUID

import anorm.{SQL, SqlParser}
import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.db.DBApi
import play.api.inject.bind
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.document.dto.{DocumentToCreate, DocumentToRead}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.dao.application.abstraction.WalletApplicationDao
import tech.pegb.backoffice.dao.application.dto.WalletApplicationToCreate
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.domain.document.model.Document
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.TestExecutionContext

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

class ApproveWalletApplicationIntegrationTest extends
  PlayIntegrationTest
  with MockFactory with ScalaFutures {
  private val baseApplicationPath = "/api/wallet_applications"
  private val baseDocsPath = "/api/documents"

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private var applicationId: Int = _
  private val applicationUuid = UUID.randomUUID()

  //doc
  private var nationalIdFrontUuid: UUID = _
  private var nationalIdBackUuid: UUID = _
  private var selfieUuid: UUID = _
  private var livenessUuid: UUID = _

  private var nationalIdFrontArrayByte: Array[Byte] = _
  private var nationalIdBackArrayByte: Array[Byte] = _
  private var selfieArrayByte: Array[Byte] = _
  private var livenessArrayByte: Array[Byte] = _

  private val resourceName = "/selfie.png"
  private val resourceUri = getClass.getResource(resourceName).toURI
  private val image = JFiles.readAllBytes(JPaths.get(resourceUri))

  val httpClientService = stub[HttpClient]

  override def beforeAll(): Unit = {
    super.beforeAll()
    val dbApi = inject[DBApi]
    logger.info(s"Databases are:\n${dbApi.databases().map(db ⇒ s"${db.name}: ${db.url}").mkString("\n")}")
    (for {
      rawDropSql ← Try(Source.fromResource(dropSchemaSqlName).mkString).toEither.leftMap(_.getMessage)
      rawDataSql ← Try(Source.fromResource(initDataSqlName).mkString).toEither.leftMap(_.getMessage)
      db ← dbApi.database("backoffice").asRight[String]
      _ ← db.withTransaction { implicit connection ⇒
        for {
          _ ← runUpdateSql(rawDropSql)
          _ ← runUpdateSql(rawDataSql)
        } yield {
          defaultUserId = SQL("SELECT id FROM users WHERE username = '+971522106589' LIMIT 1;")
            .as(SqlParser.scalar[Int].single)
          defaultUserUuid = SQL("SELECT uuid FROM users WHERE id = {id} LIMIT 1;")
            .on("id" → defaultUserId)
            .as(SqlParser.scalar[UUID].single)
          defaultIndividualUserId = SQL("SELECT id FROM individual_users WHERE user_id = {user_id} LIMIT 1;")
            .on("user_id" → defaultUserId)
            .as(SqlParser.scalar[Int].single)

        }
      }
    } yield ())
      .leftMap(err ⇒ logger.error("Failed to prepare db: " + err))

    createSuperAdmin()
  }

  //TODO: remove binding when erland endpoint is ready
  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[HttpClient].to(httpClientService),
      //bind[DocumentImmutableFileDao].to(documentImmutableFileDao),
      //bind[DocumentTransientFileDao].to(documentTransientFileDao),
      bind[WithExecutionContexts].to(TestExecutionContext)
    )

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val config = inject[AppConfig]

  "WalletApplication Approve API" should {

    "[data-preparation: application] create application " in {
      val walletApplicationDao = inject[WalletApplicationDao]
      val walletToCreate = WalletApplicationToCreate.createEmpty(applicationUuid, "Admin", LocalDateTime.now(mockClock))
        .copy(userId = defaultUserId)
      val insertRow = walletApplicationDao.insertWalletApplication(walletToCreate)

      insertRow.isRight mustBe true
      applicationId = insertRow.right.get.id
    }

    /*    "[data-preparation: documents] create documents for application in " in {
      nationalIdFrontUuid = createDocument(defaultUserUuid, applicationUuid, config.NationalIdDocumentType)
      nationalIdBackUuid = createDocument(defaultUserUuid, applicationUuid, config.NationalIdDocumentType)
      selfieUuid = createDocument(defaultUserUuid, applicationUuid, config.SelfieDocumentType)
      livenessUuid = createDocument(defaultUserUuid, applicationUuid, config.LivenessDocumentType)
    }

    "[data-preparation]: upload documents in couchbase " in {
      val individualUserDocuments = Seq(nationalIdFrontUuid, nationalIdBackUuid, selfieUuid, livenessUuid)

      individualUserDocuments.foreach{ docId ⇒
        val fileName = s"doc_$docId.png"
        val temporaryFile = PlayFiles.SingletonTemporaryFileCreator.create()
        JFiles.write(temporaryFile.path, image)
        val mpfData = MultipartFormData(
          dataParts = Map.empty,
          files = Seq(MultipartFormData.FilePart("docfile", fileName, contentType = None, temporaryFile)),
          badParts = Seq.empty
        )

        val request = FakeRequest("POST", s"$baseDocsPath/$docId/file")
          .withMultipartFormDataBody(mpfData)
        val resp = route(app, request).get
        status(resp) mustBe CREATED
        PlayFiles.SingletonTemporaryFileCreator.delete(temporaryFile)
        val fileIdOrError = contentAsString(resp).as(classOf[DocumentToRead])
        fileIdOrError.isSuccess mustBe true
        val fileId = fileIdOrError.get
        fileId.id mustBe docId
        fileId.customerId mustBe defaultUserUuid
      }
    }

    "[data-preparation]: get document file" in {
      val individualUserDocuments = Seq(nationalIdFrontUuid, nationalIdBackUuid, selfieUuid, livenessUuid)

      nationalIdFrontArrayByte = getDocumentFromCb(nationalIdFrontUuid)
      nationalIdBackArrayByte = getDocumentFromCb(nationalIdBackUuid)
      selfieArrayByte = getDocumentFromCb(selfieUuid)
      livenessArrayByte = getDocumentFromCb(livenessUuid)
    }


    "[data-preparation]: approve document" in {
      val individualUserDocuments = Seq(nationalIdFrontUuid, nationalIdBackUuid, selfieUuid, livenessUuid)
      //make sure the same file is not yet in hdfs
      val docImmutableFileDao = inject[DocumentImmutableFileDao]
      docImmutableFileDao.deleteDocumentFile(nationalIdFrontUuid)
      docImmutableFileDao.deleteDocumentFile(nationalIdBackUuid)
      docImmutableFileDao.deleteDocumentFile(selfieUuid)
      docImmutableFileDao.deleteDocumentFile(livenessUuid)

      individualUserDocuments.foreach(approveDocument(_))
    }*/

    "approve walletApplication" in {
      val doneAt = ZonedDateTime.now(mockClock)
      val doneBy = "superuser"

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(PUT, s"$baseApplicationPath/$applicationUuid/approve", jsonHeaders, jsonRequest)

      (httpClientService.request (_: String, _: String, _: Option[JsValue]))
        .when("PATCH", s"${config.CoreWalletApplicationActivationUrl}/${applicationId}", Json.obj("status" → config.ApprovedWalletApplicationStatus, "updated_by" → doneBy, "last_updated_at" → JsNull).some)
        .returns(Future.successful(HttpResponse(true, 204, None)))

      val resp = route(app, request).get
      status(resp) mustBe OK
    }

  }

  "WalletApplication Approve API - Negative" should {

    "approve walletApplication" in {
      val fakeUUID = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)
      val doneBy = "superuser"
      val requestId = UUID.randomUUID()

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(PUT, s"$baseApplicationPath/$fakeUUID/approve", jsonHeaders, jsonRequest)

      val errorMessage = s""""Approve Wallet Application failed. Wallet application ${fakeUUID} not found.""""
      val resp = route(app, request).get
      status(resp) mustBe NOT_FOUND
      val responseJson = contentAsJson(resp)
      responseJson.isInstanceOf[JsObject] mustBe true
      (responseJson \ "msg").get.toString() mustBe errorMessage
    }

  }

  private def createDocument(defaultUserUuid: UUID, applicationUuid: UUID, docType: String): UUID = {
    val toCreate = DocumentToCreate(defaultUserUuid, applicationUuid, None, docType, None, "for wallet")
    val request = makePostJsonRequest(baseDocsPath, toCreate.toJsonStr)
    val resp = route(app, request).get
    status(resp) mustBe CREATED
    val createdOrError = contentAsString(resp).as(classOf[DocumentToRead])
    createdOrError.isSuccess mustBe true
    val created = createdOrError.get
    created.customerId mustBe defaultUserUuid
    created.id
  }

  private def getDocumentFromCb(docId: UUID): Array[Byte] = {
    val request = FakeRequest("GET", s"$baseDocsPath/$docId/file")
    val resp = route(app, request).get
    status(resp) mustBe OK
    val fileBytes = contentAsBytes(resp)
    fileBytes.toArray[Byte]
  }

  private def approveDocument(docId: UUID): Unit = {
    val jsonRequest =
      s"""{
         |"updated_at": null
         |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    val request = makePutJsonRequest(s"$baseDocsPath/$docId/approve", jsonRequest)
    val resp = route(app, request).get
    status(resp) mustBe OK
    val approvedOrError = contentAsString(resp).as(classOf[DocumentToRead])
    approvedOrError.isSuccess mustBe true
    val approved = approvedOrError.get
    approved.id mustBe docId
    approved.customerId mustBe defaultUserUuid
    approved.status mustBe Document.Approved
  }
}

