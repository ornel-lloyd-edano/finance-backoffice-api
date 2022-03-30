package tech.pegb.backoffice

import play.api.libs.json.{ JsArray, JsObject }
import play.api.test.FakeRequest
import play.api.test.Helpers._

class TypesApiIntegrationTest extends PlayIntegrationTest {
  private val ImageTypesCount = 2

  "Types API" should {
    "fetch all types" in {
      val request = FakeRequest("GET", "/api/types")
      val resp = route(app, request).get
      status(resp) mustBe 200
      val json = contentAsJson(resp)
      json.isInstanceOf[JsObject] mustBe true
      json.asInstanceOf[JsObject]("image_types").asInstanceOf[JsArray].value.size mustBe ImageTypesCount
    }

    "fetch image types" in {
      val request = FakeRequest("GET", "/api/types/image_types")
      val resp = route(app, request).get
      status(resp) mustBe 200
      val json = contentAsJson(resp)
      json.isInstanceOf[JsArray] mustBe true
      json.asInstanceOf[JsArray].value.size mustBe ImageTypesCount
    }
  }
}
