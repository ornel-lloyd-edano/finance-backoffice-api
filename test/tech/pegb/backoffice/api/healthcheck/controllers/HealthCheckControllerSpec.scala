package tech.pegb.backoffice.api.healthcheck.controllers

import org.scalamock.scalatest.MockFactory
import play.api.inject.bind
import play.api.test.Helpers._
import play.api.test.FakeRequest
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.customer.controllers.CustomersController
import tech.pegb.core.PegBNoDbTestApp

class HealthCheckControllerSpec extends PegBNoDbTestApp with MockFactory {

  //TODO: remove when concrete implementation are finished
  val fakeBusinessUsersController = stub[CustomersController]

  override val additionalBindings = super.additionalBindings ++
    Seq(bind[CustomersController].to(fakeBusinessUsersController))

  "HealthCheckController" should {

    "return OK" in {
      val resp = route(app, FakeRequest(GET, "/health")).get

      status(resp) mustBe OK
    }
  }
}
