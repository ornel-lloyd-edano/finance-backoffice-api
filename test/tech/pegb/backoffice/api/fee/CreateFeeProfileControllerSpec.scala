package tech.pegb.backoffice.api.fee

import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, contentAsString, route, status, _}
import tech.pegb.backoffice.domain.fee.dto.{FeeProfileRangeToCreate, FeeProfileToCreate}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerSubscription
import tech.pegb.backoffice.domain.customer.model.{IndividualUserTiers, UserType}
import tech.pegb.backoffice.domain.fee.abstraction.FeeProfileManagement
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.{FeeCalculationMethod, FeeMethod, FeeType, TaxInclusionTypes}
import tech.pegb.backoffice.domain.fee.model.{FeeProfile, FeeProfileRange}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.ServiceError

import scala.concurrent.Future

class CreateFeeProfileControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val feeProfileManagement = stub[FeeProfileManagement]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[FeeProfileManagement].to(feeProfileManagement),
      bind[WithExecutionContexts].to(TestExecutionContext))

  private val feeProfileId: UUID = UUID.randomUUID()

  "FeeProfileController" should {

    "create fee profile and respond with FeeProfileToReadDetail json in POST /fee_profiles" in {
      val jsonRequest =
        s"""{
           |  "fee_type": "transaction_based",
           |  "user_type": "individual",
           |  "tier": "basic",
           |  "subscription_type": "standard",
           |  "transaction_type": "p2p_domestic",
           |  "channel": "mobile_money",
           |  "other_party": null,
           |  "instrument": null,
           |  "calculation_method": "flat_fee",
           |  "currency_code": "AED",
           |  "fee_method": "add",
           |  "tax_included": null,
           |  "fee_amount": 99.99
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val mockToCreate = FeeProfileToCreate.empty.copy(
        feeType = FeeType("transaction_based"),
        userType = UserType("individual"),
        tier = IndividualUserTiers.Basic,
        subscription = CustomerSubscription("standard"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_money")),
        calculationMethod = FeeCalculationMethod("flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        flatAmount = Option(BigDecimal(99.99)),
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val expected = FeeProfile(
        id = UUID.randomUUID(),
        feeType = mockToCreate.feeType,
        userType = mockToCreate.userType,
        tier = IndividualUserTiers.Basic,
        subscription = mockToCreate.subscription,
        transactionType = mockToCreate.transactionType,
        channel = mockToCreate.channel,
        otherParty = None,
        instrument = None,
        calculationMethod = mockToCreate.calculationMethod,
        currencyCode = mockToCreate.currencyCode,
        feeMethod = mockToCreate.feeMethod,
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        percentageAmount = None,
        flatAmount = mockToCreate.flatAmount,
        ranges = None,
        updatedAt = None,
        updatedBy = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      (feeProfileManagement.createFeeProfile(_: FeeProfileToCreate)(_: UUID)).when(mockToCreate, mockRequestId)
        .returns(Future.successful(Right(expected)))

      val fakeRequest = FakeRequest(POST, s"/fee_profiles",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":"${expected.id}",
           |"fee_type":"transaction_based",
           |"user_type":"individual",
           |"tier":"basic",
           |"subscription_type":"standard",
           |"transaction_type":"p2p_domestic",
           |"channel":"mobile_money",
           |"other_party":null,
           |"instrument":null,
           |"calculation_method":"flat_fee",
           |"currency_code":"AED",
           |"fee_method":"add",
           |"tax_included":null,
           |"max_fee":null,
           |"min_fee":null,
           |"fee_amount":99.99,
           |"fee_ratio":null,
           |"ranges":null,
           |"updated_at":null}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "create 1 fee profile range and respond with FeeProfileToReadDetail json in POST /fee_profiles/id/range" ignore {
      val jsonRequest =
        s"""{
           |  "min": 100.00,
           |  "max": 1000.00,
           |  "fee_amount": 5.00
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val mockToRangeCreate = FeeProfileRangeToCreate.empty.copy(from = BigDecimal("100.00"), to = Some(BigDecimal("1000.00")), flatAmount = Option(BigDecimal("5.00")))

      val mockToCreate = FeeProfileToCreate.empty.copy(
        feeType = FeeType("transaction_based"),
        userType = UserType("individual"),
        tier = IndividualUserTiers.Basic,
        subscription = CustomerSubscription("standard"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_money")),
        calculationMethod = FeeCalculationMethod("staircase_flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        flatAmount = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val found = FeeProfile(
        id = feeProfileId,
        feeType = FeeType("transaction_based"),
        userType = mockToCreate.userType,
        tier = IndividualUserTiers.Basic,
        subscription = mockToCreate.subscription,
        transactionType = mockToCreate.transactionType,
        channel = mockToCreate.channel,
        otherParty = None,
        instrument = None,
        calculationMethod = mockToCreate.calculationMethod,
        currencyCode = mockToCreate.currencyCode,
        feeMethod = mockToCreate.feeMethod,
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        percentageAmount = None,
        flatAmount = mockToCreate.flatAmount,
        ranges = None,
        updatedAt = None,
        updatedBy = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      (feeProfileManagement.getFeeProfile(_: UUID)(_: UUID)).when(feeProfileId, *).returns(Future.successful(Right(found)))

      val updatedFeeProfile = found.copy(
        id = feeProfileId,
        ranges = Option(Seq(FeeProfileRange.empty
          .copy(id = 1, from = mockToRangeCreate.from, to = mockToRangeCreate.to, flatAmount = mockToRangeCreate.flatAmount))),
        updatedBy = Option(mockRequestFrom),
        updatedAt = Option(mockRequestDate.toLocalDateTimeUTC))

      (feeProfileManagement.addFeeProfileRanges _).when(feeProfileId, Seq(mockToRangeCreate), mockRequestFrom, mockRequestDate.toLocalDateTimeUTC)
        .returns(Future.successful(Right(updatedFeeProfile)))

      val fakeRequest = FakeRequest(POST, s"/fee_profiles/$feeProfileId/range",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get
      val expectedJson =
        s"""
           |{"id":"${found.id}",
           |"fee_type":"transaction_based",
           |"user_type":"individual",
           |"tier": "basic",
           |"subscription_type":"standard",
           |"transaction_type":"p2p_domestic",
           |"channel":"mobile_money",
           |"other_party":null,
           |"instrument":null,
           |"calculation_method":"staircase_flat_fee",
           |"currency_code":"AED",
           |"fee_method":"add",
           |"tax_included":null,
           |"max_fee":null,
           |"min_fee":null,
           |"fee_amount":null,
           |"fee_ratio":null,
           |"ranges":[{"id":1,"max":1000.00,"min":100.00,"fee_amount":5.00,"fee_ratio":null}],
           |"updated_at":"${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC}"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "create multiple fee profile ranges and respond with FeeProfileToReadDetail json in POST /fee_profiles/id/ranges" ignore {
      val jsonRequest =
        s"""[
           |{
           |  "min": 100.00,
           |  "max": 500.00,
           |  "fee_amount": 5.00
           |},
           |{
           |  "min": 501.00,
           |  "max": 1000.00,
           |  "fee_amount": 10.00
           |},
           |{
           |  "min": 1001.00,
           |  "max": 5000.00,
           |  "fee_amount": 15.00
           |}
           |]
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val mockRangeToCreate = Seq(
        FeeProfileRangeToCreate.empty.copy(from = BigDecimal("100.00"), to = Some(BigDecimal("500.00")), flatAmount = Option(BigDecimal("5.00"))),
        FeeProfileRangeToCreate.empty.copy(from = BigDecimal("501.00"), to = Some(BigDecimal("1000.00")), flatAmount = Option(BigDecimal("10.00"))),
        FeeProfileRangeToCreate.empty.copy(from = BigDecimal("1001.00"), to = Some(BigDecimal("5000.00")), flatAmount = Option(BigDecimal("15.00"))))

      val mockToCreate = FeeProfileToCreate.empty.copy(
        feeType = FeeType("transaction_based"),
        userType = UserType("individual"),
        subscription = CustomerSubscription("standard"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_money")),
        calculationMethod = FeeCalculationMethod("staircase_flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        flatAmount = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val found = FeeProfile(
        id = feeProfileId,
        feeType = FeeType("transaction_based"),
        userType = mockToCreate.userType,
        tier = IndividualUserTiers.Basic,
        subscription = mockToCreate.subscription,
        transactionType = mockToCreate.transactionType,
        channel = mockToCreate.channel,
        otherParty = None,
        instrument = None,
        calculationMethod = mockToCreate.calculationMethod,
        currencyCode = mockToCreate.currencyCode,
        feeMethod = mockToCreate.feeMethod,
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        percentageAmount = None,
        flatAmount = mockToCreate.flatAmount,
        ranges = None,
        updatedAt = None,
        updatedBy = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      (feeProfileManagement.getFeeProfile(_: UUID)(_: UUID)).when(feeProfileId, *).returns(Future.successful(Right(found)))

      val updatedFeeProfile = found.copy(
        id = feeProfileId,
        ranges = Option(mockRangeToCreate.map(f ⇒ FeeProfileRange.empty
          .copy(from = f.from, to = f.to, flatAmount = f.flatAmount)).zip(Seq(1, 2, 3))
          .map(tuple ⇒ tuple._1.copy(id = tuple._2))),
        updatedBy = Option(mockRequestFrom),
        updatedAt = Option(mockRequestDate.toLocalDateTimeUTC))

      (feeProfileManagement.addFeeProfileRanges _).when(feeProfileId, mockRangeToCreate, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC)
        .returns(Future.successful(Right(updatedFeeProfile)))

      val fakeRequest = FakeRequest(POST, s"/fee_profiles/$feeProfileId/ranges",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get
      val expectedJson =
        s"""
           |{"id":"${found.id}",
           |"fee_type":"transaction_based",
           |"user_type":"individual",
           |"tier": "basic",
           |"subscription_type":"standard",
           |"transaction_type":"p2p_domestic",
           |"channel":"mobile_money",
           |"other_party":null,
           |"instrument":null,
           |"calculation_method":"staircase_flat_fee",
           |"currency_code":"AED",
           |"fee_method":"add",
           |"tax_included":null,
           |"max_fee":null,
           |"min_fee":null,
           |"fee_amount":null,
           |"fee_ratio":null,
           |"ranges":[{"id":1,"max":500.00,"min":100.00,"fee_amount":5.00,"fee_ratio":null},
           |{"id":2,"max":1000.00,"min":501.00,"fee_amount":10.00,"fee_ratio":null},
           |{"id":3,"max":5000.00,"min":1001.00,"fee_amount":15.00,"fee_ratio":null}
           |],
           |"updated_at":"${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC}"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "fail to create fee profile and respond with ApiError json in POST /fee_profiles if any required field (fee_type) is null" in {
      val jsonRequest =
        s"""{
           |  "fee_type": null,
           |  "user_type": "individual",
           |  "tier": "basic",
           |  "subscription_type": "standard",
           |  "transaction_type": "p2p_domestic",
           |  "channel": "mobile_money",
           |  "other_party": null,
           |  "instrument": null,
           |  "calculation_method": "flat_fee",
           |  "currency_code": "AED",
           |  "fee_method": "add",
           |  "tax_included": null,
           |  "max_fee": null,
           |  "min_fee": null,
           |  "fee_amount": 99.99,
           |  "fee_ratio": null,
           |  "ranges": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/fee_profiles",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"could not parse create request to domain"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail to create fee profile and respond with ApiError json in POST /fee_profiles if any required field (user_type) is empty" in {
      val jsonRequest =
        s"""{
           |  "fee_type": "transaction_based",
           |  "user_type": "",
           |  "tier": "basic",
           |  "subscription_type": "standard",
           |  "transaction_type": "p2p_domestic",
           |  "channel": "mobile_money",
           |  "other_party": null,
           |  "instrument": null,
           |  "calculation_method": "flat_fee",
           |  "currency_code": "AED",
           |  "fee_method": "add",
           |  "tax_included": null,
           |  "max_fee": null,
           |  "min_fee": null,
           |  "fee_amount": 99.99,
           |  "fee_ratio": null,
           |  "ranges": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/fee_profiles",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"could not parse create request to domain"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail to create fee profile and respond with ApiError json in POST /fee_profiles if any field is missing (tax_included)" in {
      val jsonRequest =
        s"""{
           |  "fee_type": "transaction_based",
           |  "user_type": "individual",
           |  "tier": "basic",
           |  "subscription_type": "standard",
           |  "transaction_type": "p2p_domestic",
           |  "channel": "mobile_money",
           |  "other_party": null,
           |  "instrument": null,
           |  "calculation_method": "flat_fee",
           |  "currency_code": "AED",
           |  "fee_method": "add",
           |  "max_fee": null,
           |  "min_fee": null,
           |  "fee_amount": 99.99,
           |  "fee_ratio": null,
           |  "ranges": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/fee_profiles",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"MalformedRequest",
           |"msg":"Malformed request to create a fee profile. Mandatory field is missing or value of a field is of wrong type."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail to create 1 fee profile range if id was not found on POST /fee_profiles/id/range" ignore {
      val jsonRequest =
        s"""{
           |  "min": 100.00,
           |  "max": 500.00,
           |  "amount": 5.00
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val expectedError = ServiceError.notFoundError(s"Fee profile id [$feeProfileId] not found", UUID.randomUUID().toOption)
      (feeProfileManagement.getFeeProfile(_: UUID)(_: UUID)).when(feeProfileId, *).returns(Future.successful(Left(expectedError)))

      val fakeRequest = FakeRequest(POST, s"/fee_profiles/$feeProfileId/range",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get
      val expectedJson =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFoundEntity",
           |"msg":"Fee profile id [$feeProfileId] not found"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expectedJson
    }

    "fail to create multiple fee profile ranges if id was not found on POST /fee_profiles/id/ranges" ignore {
      val jsonRequest =
        s"""[
           |{
           |  "min": 100.00,
           |  "max": 500.00,
           |  "amount": 5.00
           |},
           |{
           |  "min": 501.00,
           |  "max": 1000.00,
           |  "amount": 10.00
           |},
           |{
           |  "min": 1001.00,
           |  "max": 5000.00,
           |  "amount": 15.00
           |}
           |]
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val expectedError = ServiceError.notFoundError(s"Fee profile id [$feeProfileId] not found", UUID.randomUUID().toOption)
      (feeProfileManagement.getFeeProfile(_: UUID)(_: UUID)).when(feeProfileId, *).returns(Future.successful(Left(expectedError)))

      val fakeRequest = FakeRequest(POST, s"/fee_profiles/$feeProfileId/ranges",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get
      val expectedJson =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFoundEntity",
           |"msg":"Fee profile id [$feeProfileId] not found"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expectedJson
    }

  }
}
