package tech.pegb.backoffice.domain.makerchecker

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.{Binding, bind}
import play.api.libs.json.JsValue
import tech.pegb.backoffice.dao.makerchecker.entity.MakerCheckerTask
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.domain.makerchecker.abstraction.RequestCreator
import tech.pegb.backoffice.mapping.dao.domain.makerchecker.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class RequestCreatorImplSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
  private val mockHttpClient = stub[HttpClient]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[HttpClient].to(mockHttpClient),
      bind[WithExecutionContexts].to(TestExecutionContext))

  private val requestCreator = inject[RequestCreator]

  "RequestCreator" should {

    "send a request get success response" in {

      val mc1 = MakerCheckerTask(
        id = 1,
        uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "$backoffice_api_host",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = Some("""{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}"""),
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 3,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      val response: HttpResponse = HttpResponse(success = true, statusCode = 200, body = None)
      (mockHttpClient.request(
        _: String,
        _: String,
        _: Map[String, String],
        _: Map[String, String],
        _: Option[JsValue],
        _: UUID))
        .when(*, *, *, *, *, *).returns(Future.successful(response))

      val result = requestCreator.createRequest(mc1.asDomain(None).get, "$backoffice_api_host", "http://localhost:9000")

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }

    "send a request and get NOK response" in {

      val mc1 = MakerCheckerTask(
        id = 1,
        uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
        module = "strings",
        action = "create i18n string",
        verb = "PUT",
        url = "$backoffice_api_host",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = Some("""{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}"""),
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 3,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      val response: HttpResponse = HttpResponse(success = false, statusCode = 500, body = None)
      (mockHttpClient.request(
        _: String,
        _: String,
        _: Map[String, String],
        _: Map[String, String],
        _: Option[JsValue],
        _: UUID))
        .when(*, *, *, *, *, *).returns(Future.successful(response))

      val resultF = requestCreator.createRequest(mc1.asDomain(None).get, "$backoffice_api_host", "http://localhost:9000")

      whenReady(resultF) { result ⇒
        assert(result.isLeft)

        result.left.map(_.message) mustBe Left("Received response status 500 with no body")
      }
    }
  }

  "sends a request with invalid url" in {

    val mc1 = MakerCheckerTask(
      id = 1,
      uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
      module = "strings",
      action = "create i18n string",
      verb = "PUT",
      url = "$backoffice_api_host_one",
      headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
      body = Some("""{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}"""),
      status = "pending",
      createdBy = "pegbuser",
      createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
      makerLevel = 3,
      makerBusinessUnit = "Finance",
      checkedBy = None,
      checkedAt = None,
      reason = None,
      updatedAt = None)

    val response: HttpResponse = HttpResponse(success = false, statusCode = 500, body = None)
    (mockHttpClient.request(
      _: String,
      _: String,
      _: Map[String, String],
      _: Map[String, String],
      _: Option[JsValue],
      _: UUID))
      .when(*, *, *, *, *, *).returns(Future.successful(response))

    val resultF = requestCreator.createRequest(mc1.asDomain(None).get, "$backoffice_api_host", "http://localhost:9000")

    whenReady(resultF) { result ⇒
      assert(result.isLeft)

      result.left.map(_.message) mustBe Left("Received response status 500 with no body")
    }
  }

  "sends a request with invalid query parameters" in {

    val mc1 = MakerCheckerTask(
      id = 1,
      uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
      module = "strings",
      action = "create i18n string",
      verb = "PUT",
      url = "$backoffice_api_host?=",
      headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
      body = Some("""{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}"""),
      status = "pending",
      createdBy = "pegbuser",
      createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
      makerLevel = 3,
      makerBusinessUnit = "Finance",
      checkedBy = None,
      checkedAt = None,
      reason = None,
      updatedAt = None)

    val response: HttpResponse = HttpResponse(success = false, statusCode = 500, body = None)
    (mockHttpClient.request(
      _: String,
      _: String,
      _: Map[String, String],
      _: Map[String, String],
      _: Option[JsValue],
      _: UUID))
      .when(*, *, *, *, *, *).returns(Future.successful(response))

    val resultF = requestCreator.createRequest(mc1.asDomain(None).get, "$backoffice_api_host", "http://localhost:9000")

    whenReady(resultF) { result ⇒
      assert(result.isLeft)

      result.left.map(_.message) mustBe Left("malformed query param, Failure(java.util.NoSuchElementException: next on empty iterator)")
    }
  }
}

