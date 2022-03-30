package tech.pegb.backoffice.api.customer.controller

import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.{Currency, UUID}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.test.Helpers.{GET, route, _}
import play.api.test.{FakeRequest, Injecting}
import tech.pegb.backoffice.domain.customer.abstraction.SavingOptionsMgmtService
import tech.pegb.backoffice.domain.customer.model.{GenericSavingOption, SavingOptionTypes}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBTestApp, TestExecutionContext}
import tech.pegb.backoffice.domain.ServiceError._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.mapping.api.domain.customer.Implicits._

import scala.concurrent.Future

class SavingOptionsControllerSpec extends PlaySpec with PegBTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {
  implicit val ec = TestExecutionContext.genericOperations

  val savingOptionsService = stub[SavingOptionsMgmtService]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[SavingOptionsMgmtService].to(savingOptionsService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "SavingOptionsController" should {
    "respond with PaginatedSavingOptionsResult in GET /customers/:customer_id/saving_options" in {
      val savingOptions = GenericSavingOption(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        savingType = SavingOptionTypes.RoundUp,
        amount = BigDecimal(100.00).some,
        currentAmount = BigDecimal(50.00),
        currency = Currency.getInstance("KES"),
        reason = "trip to Malta	vacation".some,
        createdAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57),
        dueDate = LocalDate.of(2019, 7, 7).some,
        updatedAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57))
      val expectedResponse =
        s"""
           |[{
           |"id":"${savingOptions.id}",
           |"customer_id":"${savingOptions.customerId}",
           |"type":"roundup_savings",
           |"name":null,
           |"amount":100.0,
           |"current_amount":50.0,
           |"currency":"KES",
           |"reason":"trip to Maltatvacation",
           |"created_at":"2019-07-07T13:09:57Z",
           |"due_date":"2019-07-07",
           |"updated_at":"2019-07-07T13:09:57Z"
           |}]
        """.stripMargin.replaceAll("\n", "")

      val criteria = (Some(savingOptions.id), Some("active")).asDomain

      (savingOptionsService.getCustomerSavingOptions _)
        .when(savingOptions.id, criteria.toOption).returns(Future.successful(Right(Seq(savingOptions))))

      (savingOptionsService.getLatestVersion _)
        .when(Seq(savingOptions)).returns(Future.successful(Right("2019-07-07T13:09:57".some)))

      val resp = route(app, FakeRequest(GET, s"/customers/${savingOptions.id}/saving_options?status=active")).get
      status(resp) mustBe OK
      contentAsString(resp) mustEqual expectedResponse.trim

      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe savingOptions.updatedAt.toString.toOption
    }

    "respond with 200 OK in DELETE /customers/:id/saving_options/:id/deactivate" in {
      val zonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))
      val savingOptions = GenericSavingOption(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        savingType = SavingOptionTypes.AutoDeduct,
        amount = BigDecimal(100.00).some,
        currentAmount = BigDecimal(50.00),
        currency = Currency.getInstance("KES"),
        reason = "trip to Malta	vacation".some,
        createdAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57),
        dueDate = LocalDate.of(2019, 7, 7).some,
        updatedAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57))
      val expectedResponse =
        s"""
           |{
           |"id":"${savingOptions.id}",
           |"customer_id":"${savingOptions.customerId}",
           |"type":"auto_deduct_savings",
           |"name":null,
           |"amount":100.0,
           |"current_amount":50.0,
           |"currency":"KES",
           |"reason":"trip to Maltatvacation",
           |"created_at":"2019-07-07T13:09:57Z",
           |"due_date":"2019-07-07",
           |"updated_at":"2019-07-07T13:09:57Z"
           |}
        """.stripMargin.replaceAll("\n", "")

      (savingOptionsService.deactivateSavingOption(_: UUID, _: UUID, _: String, _: LocalDateTime, _: Option[LocalDateTime])(_: UUID))
        .when(savingOptions.id, savingOptions.customerId, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, Some(zonedDateTime.toLocalDateTimeUTC), *).returns(Future.successful(Right(savingOptions)))
      val jsonRequest = s"""{"updated_at":"$zonedDateTime"}"""
      val fakeRequest = FakeRequest(DELETE, s"/customers/${savingOptions.customerId}/saving_options/${savingOptions.id}", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expectedResponse.trim

    }

    "respond with 404 NotFound in DELETE /customers/:id/saving_options/:id/deactivate if saving option is not found" in {

      implicit val errorId: UUID = UUID.randomUUID()

      val savingOptions = GenericSavingOption(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        savingType = SavingOptionTypes.AutoDeduct,
        amount = BigDecimal(100.00).some,
        currentAmount = BigDecimal(50.00),
        currency = Currency.getInstance("KES"),
        reason = "trip to Malta	vacation".some,
        createdAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57),
        dueDate = LocalDate.of(2019, 7, 7).some,
        updatedAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57))

      val expectedResponse =
        s"""
           |{
           |"id":"$mockRequestId",
           |"code":"NotFound",
           |"msg":"entity not found",
           |"tracking_id":"$errorId"}
        """.stripMargin.replaceAll("\n", "")

      val domainErrorResponse = notFoundError("entity not found", errorId.toOption)
      (savingOptionsService.deactivateSavingOption(_: UUID, _: UUID, _: String, _: LocalDateTime, _: Option[LocalDateTime])(_: UUID))
        .when(savingOptions.id, savingOptions.customerId, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None, *).returns(Future.successful(Left(domainErrorResponse)))

      val jsonRequest = s"""{"last_updated_at":null}"""
      val fakeRequest = FakeRequest(DELETE, s"/customers/${savingOptions.customerId}/saving_options/${savingOptions.id}", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get
      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustEqual expectedResponse.trim
    }

    "respond with 404 NotFound in DELETE /customers/:id/saving_options/:id/deactivate if customer is not found" in {

      implicit val errorId: UUID = UUID.randomUUID()

      val savingOptions = GenericSavingOption(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        savingType = SavingOptionTypes.AutoDeduct,
        amount = BigDecimal(100.00).some,
        currentAmount = BigDecimal(50.00),
        currency = Currency.getInstance("KES"),
        reason = "trip to Malta	vacation".some,
        createdAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57),
        dueDate = LocalDate.of(2019, 7, 7).some,
        updatedAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57))

      val expectedResponse =
        s"""
           |{
           |"id":"$mockRequestId",
           |"code":"NotFound",
           |"msg":"customer not found",
           |"tracking_id":"$errorId"}
        """.stripMargin.replaceAll("\n", "")

      val domainErrorResponse = notFoundError("customer not found", errorId.toOption)
      (savingOptionsService.deactivateSavingOption(_: UUID, _: UUID, _: String, _: LocalDateTime, _: Option[LocalDateTime])(_: UUID))
        .when(savingOptions.id, savingOptions.customerId, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None, *)
        .returns(Future.successful(Left(domainErrorResponse)))

      val jsonRequest = s"""{"last_updated_at":null}"""
      val fakeRequest = FakeRequest(DELETE, s"/customers/${savingOptions.customerId}/saving_options/${savingOptions.id}", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get
      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustEqual expectedResponse.trim
    }

    "respond with 400 BadRequest in DELETE /customers/:id/saving_options/:id/deactivate if customer is found but not individual_user" in {
      implicit val errorId: UUID = UUID.randomUUID()

      val savingOptions = GenericSavingOption(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        savingType = SavingOptionTypes.AutoDeduct,
        amount = BigDecimal(100.00).some,
        currentAmount = BigDecimal(50.00),
        currency = Currency.getInstance("KES"),
        reason = "trip to Malta	vacation".some,
        createdAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57),
        dueDate = LocalDate.of(2019, 7, 7).some,
        updatedAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57))

      val expectedResponse =
        s"""
           |{
           |"id":"$mockRequestId",
           |"code":"NotFound",
           |"msg":"customer not found",
           |"tracking_id":"$errorId"}
        """.stripMargin.replaceAll("\n", "")

      val domainErrorResponse = notFoundError(s"customer not found", errorId.toOption)
      (savingOptionsService.deactivateSavingOption(_: UUID, _: UUID, _: String, _: LocalDateTime, _: Option[LocalDateTime])(_: UUID))
        .when(savingOptions.id, savingOptions.customerId, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None, *)
        .returns(Future.successful(Left(domainErrorResponse)))

      val jsonRequest = s"""{"last_updated_at":null}"""
      val fakeRequest = FakeRequest(DELETE, s"/customers/${savingOptions.customerId}/saving_options/${savingOptions.id}", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get
      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustEqual expectedResponse.trim
    }

    "respond with 400 BadRequest in DELETE /customers/:id/saving_options/:id/deactivate if saving goal is found but status is already deactivated" in {

      implicit val errorId: UUID = UUID.randomUUID()

      val savingOptions = GenericSavingOption(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        savingType = SavingOptionTypes.AutoDeduct,
        amount = BigDecimal(100.00).some,
        currentAmount = BigDecimal(50.00),
        currency = Currency.getInstance("KES"),
        reason = "trip to Malta	vacation".some,
        createdAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57),
        dueDate = LocalDate.of(2019, 7, 7).some,
        updatedAt = LocalDateTime.of(2019, 7, 7, 13, 9, 57))

      val expectedResponse =
        s"""
           |{
           |"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"saving goal is already deactivated",
           |"tracking_id":"$errorId"}
        """.stripMargin.replaceAll("\n", "")

      val domainErrorResponse = validationError(s"saving goal is already deactivated", errorId.toOption)
      (savingOptionsService.deactivateSavingOption(_: UUID, _: UUID, _: String, _: LocalDateTime, _: Option[LocalDateTime])(_: UUID))
        .when(savingOptions.id, savingOptions.customerId, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None, *)
        .returns(Future.successful(Left(domainErrorResponse)))

      val jsonRequest = s"""{"last_updated_at":null}"""
      val fakeRequest = FakeRequest(DELETE, s"/customers/${savingOptions.customerId}/saving_options/${savingOptions.id}", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustEqual expectedResponse.trim
    }
  }

}
