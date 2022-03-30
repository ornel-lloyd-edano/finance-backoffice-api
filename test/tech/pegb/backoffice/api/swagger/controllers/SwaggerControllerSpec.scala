package tech.pegb.backoffice.api.swagger.controllers

import org.scalamock.scalatest.MockFactory
import org.scalatest.OptionValues
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.customer.controllers.CustomersController
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class SwaggerControllerSpec extends PegBNoDbTestApp with OptionValues with MockFactory {

  val fakeBusinessUsersController = stub[CustomersController]

  //TODO: remove when concrete implementation are finished

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[CustomersController].to(fakeBusinessUsersController),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "SwaggerController" should {

    "redirect to swagger from /" in {
      val resp = route(app, FakeRequest(GET, "/")).get

      status(resp) mustBe SEE_OTHER
      header(LOCATION, resp).value mustBe "/swagger-ui/index.html?url=%2Fswagger.json"
    }

    "respond 404 to non-existing swagger page" in {
      val resp = route(app, FakeRequest(GET, "/swagger-ui/index2.html")).get

      status(resp) mustBe NOT_FOUND
    }
  }
}
