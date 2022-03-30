package tech.pegb.backoffice

import java.time._
import java.util.UUID

import anorm.{SQL, SqlParser}
import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.db.DBApi
import play.api.inject.{Binding, bind}
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.dao.application.abstraction.WalletApplicationDao
import tech.pegb.backoffice.dao.application.dto.WalletApplicationToCreate
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.util.AppConfig

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

class RejectWalletApplicationIntegrationTest extends PlayIntegrationTest with MockFactory with ScalaFutures {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val httpClientService = stub[HttpClient]

  //TODO: remove binding when erland endpoint is ready
  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(bind[HttpClient].to(httpClientService))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val config = inject[AppConfig]

  private var applicationId: Int = _
  private val applicationUuid = UUID.randomUUID()

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

  "Wallet Application Reject API" should {
    "[data-preparation: application] create application" in {
      val walletApplicationDao = inject[WalletApplicationDao]
      val walletToCreate = WalletApplicationToCreate.createEmpty(applicationUuid, "Admin", LocalDateTime.now(mockClock))
        .copy(id = Option(applicationId), userId = defaultUserId)
      val insertRow = walletApplicationDao.insertWalletApplication(walletToCreate)

      insertRow.isRight mustBe true
      applicationId = insertRow.right.get.id
    }

    "reject walletApplication" in {
      val doneAt = ZonedDateTime.now(mockClock)
      val doneBy = "superuser"

      (httpClientService.request(_: String, _: String, _: Option[JsValue]))
        .when("PATCH", s"${config.CoreWalletApplicationActivationUrl}/${applicationId}", Json.obj("status" → config.RejectedWalletApplicationStatus, "updated_by" → doneBy, "rejection_reason" → "insufficient document", "last_updated_at" → JsNull).some)
        .returns(Future.successful(HttpResponse(true, 204, None)))

      val jsonRequest =
        s"""
           |{"reason": "insufficient document"}
        """.stripMargin.replaceAll("\n", "")

      val resp = route(app, FakeRequest(PUT, s"/api/wallet_applications/${applicationUuid}/reject")
        .withBody(jsonRequest)
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK
    }
  }

  "WalletApplication Approve API - Negative" should {
    "reject walletApplication" in {
      val fakeUUID = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)
      val doneBy = "superuser"

      val jsonRequest =
        s"""
           |{"reason": "insufficient document"}
        """.stripMargin.replaceAll("\n", "")

      val resp = route(app, FakeRequest(PUT, s"/api/wallet_applications/${fakeUUID}/reject")
        .withBody(jsonRequest)
        .withHeaders(jsonHeaders)).get

      val errorMessage = s""""Reject Wallet Application failed. Wallet application ${fakeUUID} not found.""""

      status(resp) mustBe NOT_FOUND
      val responseJson = contentAsJson(resp)
      responseJson.isInstanceOf[JsObject] mustBe true
      (responseJson \ "msg").get.toString() mustBe errorMessage
    }

  }

}
