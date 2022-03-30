package it.scala.tech.pegb.backoffice.template

import anorm._
import org.scalatest.concurrent.ScalaFutures
import play.api.db.{DBApi, Database}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.PlayIntegrationTest

//Rename me
class TemplateForIntegTest extends PlayIntegrationTest with ScalaFutures {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  override def cleanupSql =
    s"""
       |
     """.stripMargin

  override def initSql =
    s"""
       |
     """.stripMargin

  override val endpoint = ""

  val db: Database = inject[DBApi].database("backoffice")

  "Api" should {
    "expected behavior of GET endpoint" ignore {

      val resp = route(app, FakeRequest(GET, s"$endpoint").withHeaders(AuthHeader)).get
      val expectedJson =
        s"""
           |
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson

      val isReallyInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM {Table}")
        result.executeQuery().as(result.defaultParser.*)
        true
      }
      isReallyInDB mustBe true
    }

    "expected behavior of POST endpoint" ignore {

      val jsonRequest =
        s"""
           |
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest).withHeaders(AuthHeader)).get
      val expectedJson =
        s"""
           |
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson

      val isReallyInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM {Table}")
        result.executeQuery().as(result.defaultParser.*)
        true
      }
      isReallyInDB mustBe true
    }

    "expected behavior of PUT endpoint" ignore {

      val jsonRequest =
        s"""
           |
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest).withHeaders(AuthHeader)).get
      val expectedJson =
        s"""
           |
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson

      val isReallyInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM {Table}")
        result.executeQuery().as(result.defaultParser.*)
        true
      }
      isReallyInDB mustBe true
    }

    "expected behavior of DELETE endpoint" ignore {

      val jsonRequest =
        s"""
           |
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest).withHeaders(AuthHeader)).get
      val expectedJson =
        s"""
           |
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson

      val isReallyInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM {Table}")
        result.executeQuery().as(result.defaultParser.*)
        true
      }
      isReallyInDB mustBe true
    }
  }
}
