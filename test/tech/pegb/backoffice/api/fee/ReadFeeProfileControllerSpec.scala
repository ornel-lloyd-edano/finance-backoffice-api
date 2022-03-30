package tech.pegb.backoffice.api.fee

import java.time._
import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.mvc.Headers
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerSubscription
import tech.pegb.backoffice.domain.customer.model.{IndividualUserTiers, UserType}
import tech.pegb.backoffice.domain.fee.abstraction.FeeProfileManagement
import tech.pegb.backoffice.domain.fee.dto.FeeProfileCriteria
import tech.pegb.backoffice.domain.fee.model.FeeAttributes._
import tech.pegb.backoffice.domain.fee.model.{FeeProfile, FeeProfileRange}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future

class ReadFeeProfileControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  private val feeProfileManagement = stub[FeeProfileManagement]
  private val latestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[FeeProfileManagement].to(feeProfileManagement),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(1551830400000L), ZoneId.of("UTC"))

  "FeeProfileController getFeeProfile" should {
    "respond with FeeProfile" in {
      val id = UUID.randomUUID()
      implicit val requestId = UUID.randomUUID()

      val expectedFeeProfile = FeeProfile(
        id = id,
        feeType = FeeType("transaction_based"),
        userType = UserType("individual_user"),
        tier = IndividualUserTiers.Basic,
        subscription = CustomerSubscription("standard"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.TaxIncluded,
        maxFee = None,
        minFee = None,
        flatAmount = Some(BigDecimal(20.00)),
        percentageAmount = None,
        ranges = None,
        createdAt = LocalDateTime.now(mockClock),
        updatedAt = None,
        createdBy = "pegbuser",
        updatedBy = None)

      val expectedJson =
        s"""{
           |"id":"$id",
           |"fee_type":"transaction_based",
           |"user_type":"individual_user",
           |"tier":"basic",
           |"subscription_type":"standard",
           |"transaction_type":"p2p_domestic",
           |"channel":"mobile_application",
           |"other_party":null,
           |"instrument":"visa_debit",
           |"calculation_method":"flat_fee",
           |"currency_code":"AED",
           |"fee_method":"add",
           |"tax_included":true,
           |"max_fee":null,
           |"min_fee":null,
           |"fee_amount":20.0,
           |"fee_ratio":null,
           |"ranges":null,
           |"updated_at":null
           |}""".stripMargin.replace(System.lineSeparator(), "")

      (feeProfileManagement.getFeeProfile(_: UUID)(_: UUID)).when(id, requestId)
        .returns(Future.successful(Right(expectedFeeProfile)))

      val resp = route(app, FakeRequest(GET, s"/fee_profiles/$id")
        .withHeaders(Headers("request-id" → requestId.toString))).get

      contentAsString(resp) mustBe expectedJson

    }

    "respond with FeeProfile staircase" in {
      val id = UUID.randomUUID()
      implicit val requestId = UUID.randomUUID()

      val range1 = FeeProfileRange(
        id = 1,
        from = BigDecimal(0),
        to = Some(BigDecimal(1000)),
        flatAmount = None,
        percentageAmount = Some(BigDecimal(5)))

      val range2 = FeeProfileRange(
        id = 2,
        from = BigDecimal(1001),
        to = Some(BigDecimal(5000)),
        flatAmount = None,
        percentageAmount = Some(BigDecimal(2)))

      val range3 = FeeProfileRange(
        id = 3,
        from = BigDecimal(5001),
        to = Some(BigDecimal(10000)),
        flatAmount = None,
        percentageAmount = Some(BigDecimal(1)))

      val expectedFeeProfile = FeeProfile(
        id = id,
        feeType = FeeType("transaction_based"),
        userType = UserType("individual_user"),
        tier = IndividualUserTiers.Basic,
        subscription = CustomerSubscription("standard"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_application")),
        otherParty = None,
        instrument = Some("visa_debit"),
        calculationMethod = FeeCalculationMethod("staircase_with_percentage"),
        currencyCode = Currency.getInstance("USD"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.TaxNotIncluded,
        maxFee = Some(BigDecimal(10.00)),
        minFee = Some(BigDecimal(5.00)),
        flatAmount = None,
        percentageAmount = None,
        ranges = Some(Seq(range1, range2, range3)),
        createdAt = LocalDateTime.now(mockClock),
        updatedAt = None,
        createdBy = "pegbuser",
        updatedBy = None)

      val expectedJson =
        s"""{
           |"id":"$id",
           |"fee_type":"transaction_based",
           |"user_type":"individual_user",
           |"tier":"basic",
           |"subscription_type":"standard",
           |"transaction_type":"p2p_domestic",
           |"channel":"mobile_application",
           |"other_party":null,
           |"instrument":"visa_debit",
           |"calculation_method":"staircase_with_percentage",
           |"currency_code":"USD",
           |"fee_method":"add",
           |"tax_included":false,
           |"max_fee":10.0,
           |"min_fee":5.0,
           |"fee_amount":null,
           |"fee_ratio":null,
           |"ranges":[
             |{
             |"id":1,
             |"max":1000,
             |"min":0,
             |"fee_amount":null,
             |"fee_ratio":5
             |},
             |{
             |"id":2,
             |"max":5000,
             |"min":1001,
             |"fee_amount":null,
             |"fee_ratio":2
             |},
             |{
             |"id":3,
             |"max":10000,
             |"min":5001,
             |"fee_amount":null,
             |"fee_ratio":1
             |}],
           |"updated_at":null
           |}""".stripMargin.replace(System.lineSeparator(), "")

      (feeProfileManagement.getFeeProfile(_: UUID)(_: UUID)).when(id, requestId)
        .returns(Future.successful(Right(expectedFeeProfile)))

      val resp = route(app, FakeRequest(GET, s"/fee_profiles/$id")
        .withHeaders(Headers("request-id" → requestId.toString))).get

      contentAsString(resp) mustBe expectedJson

    }

    "respond 404 NotFound in /fee_profiles/:id if fee_profiles uuid was not found" in {
      val requestId = UUID.randomUUID()
      val fakeUUID = UUID.randomUUID()
      (feeProfileManagement.getFeeProfile(_: UUID)(_: UUID)).when(fakeUUID, *)
        .returns(Future.successful(Left(ServiceError.notFoundError("Fee Profile not Found", requestId.toOption))))

      val resp = route(app, FakeRequest(GET, s"/fee_profiles/$fakeUUID")
        .withHeaders(Headers("request-id" → requestId.toString))).get

      val expected =
        s"""{"id":"$requestId",
           |"code":"NotFound",
           |"msg":"Fee Profile not Found",
           |"tracking_id":"$requestId"}""".stripMargin.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
      (contentAsJson(resp) \ "code").get.toString() mustBe "\"NotFound\""
    }
  }

  "FeeProfile getFeeProfileByCriteria" should {
    val fp1 = FeeProfile(
      id = UUID.randomUUID(),
      feeType = FeeType("transaction_based"),
      userType = UserType("individual_user"),
      tier = IndividualUserTiers.Basic,
      subscription = CustomerSubscription("standard"),
      transactionType = TransactionType("p2p_domestic"),
      channel = Some(Channel("mobile_application")),
      otherParty = None,
      instrument = Some("visa_debit"),
      calculationMethod = FeeCalculationMethod("flat_fee"),
      currencyCode = Currency.getInstance("AED"),
      feeMethod = FeeMethod("add"),
      taxInclusion = TaxInclusionTypes.TaxIncluded,
      maxFee = None,
      minFee = None,
      flatAmount = Some(BigDecimal(20.00)),
      percentageAmount = None,
      ranges = None,
      createdAt = LocalDateTime.now(mockClock),
      updatedAt = None,
      createdBy = "pegbuser",
      updatedBy = None)

    val fp2 = FeeProfile(
      id = UUID.randomUUID(),
      feeType = FeeType("subscription_based"),
      userType = UserType("individual_user"),
      tier = IndividualUserTiers.Basic,
      subscription = CustomerSubscription("platinum"),
      transactionType = TransactionType("p2p_international"),
      channel = Some(Channel("atm")),
      otherParty = Some("Mashreq"),
      instrument = Some("visa_debit"),
      calculationMethod = FeeCalculationMethod("flat_fee"),
      currencyCode = Currency.getInstance("AED"),
      feeMethod = FeeMethod("add"),
      taxInclusion = TaxInclusionTypes.NoTax,
      maxFee = None,
      minFee = None,
      flatAmount = Some(BigDecimal(30.00)),
      percentageAmount = None,
      ranges = None,
      createdAt = LocalDateTime.now(mockClock),
      updatedAt = None,
      createdBy = "pegbuser",
      updatedBy = None)

    val range1 = FeeProfileRange(
      id = 1,
      from = BigDecimal(0),
      to = Some(BigDecimal(1000)),
      flatAmount = None,
      percentageAmount = Some(BigDecimal(5)))

    val range2 = FeeProfileRange(
      id = 2,
      from = BigDecimal(1001),
      to = Some(BigDecimal(5000)),
      flatAmount = None,
      percentageAmount = Some(BigDecimal(2)))

    val range3 = FeeProfileRange(
      id = 3,
      from = BigDecimal(5001),
      to = Some(BigDecimal(10000)),
      flatAmount = None,
      percentageAmount = Some(BigDecimal(1)))

    val fp3 = FeeProfile(
      id = UUID.randomUUID(),
      feeType = FeeType("transaction_based"),
      userType = UserType("individual_user"),
      tier = IndividualUserTiers.Basic,
      subscription = CustomerSubscription("standard"),
      transactionType = TransactionType("p2p_domestic"),
      channel = Some(Channel("mobile_application")),
      otherParty = None,
      instrument = Some("visa_debit"),
      calculationMethod = FeeCalculationMethod("staircase_with_percentage"),
      currencyCode = Currency.getInstance("USD"),
      feeMethod = FeeMethod("add"),
      taxInclusion = TaxInclusionTypes.TaxNotIncluded,
      maxFee = Some(BigDecimal(10.00)),
      minFee = Some(BigDecimal(5.00)),
      flatAmount = None,
      percentageAmount = None,
      ranges = Some(Seq(range1, range2, range3)),
      createdAt = LocalDateTime.now(mockClock),
      updatedAt = None,
      createdBy = "pegbuser",
      updatedBy = None)

    "return list of fee profile satisfying filter" in {
      val requestId = UUID.randomUUID()
      val criteria = FeeProfileCriteria(
        feeType = Some(FeeType("transaction_based")),
        taxInclusion = None,
        partialMatchFields = Constants.feeProfilePartialMatchFields.filterNot(_ == "disabled"))

      val ordering = Seq(Ordering("currency_code", Ordering.ASCENDING))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      (feeProfileManagement.countFeeProfileByCriteria _).when(criteria)
        .returns(Future.successful(Right(2)))
      (feeProfileManagement.getFeeProfileByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(fp1, fp3))))

      val resp = route(app, FakeRequest(GET, s"/fee_profiles?fee_type=transaction_based&order_by=currency_code")
        .withHeaders(Headers("request-id" → requestId.toString))).get

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{
           |"id":"${fp1.id}",
           |"fee_type":"transaction_based",
           |"user_type":"individual_user",
           |"tier":"basic",
           |"subscription_type":"standard",
           |"transaction_type":"p2p_domestic",
           |"channel":"mobile_application",
           |"other_party":null,
           |"instrument":"visa_debit",
           |"calculation_method":"flat_fee",
           |"fee_method":"add",
           |"tax_included":true,
           |"currency_code":"AED",
           |"updated_at":null
           |},
           |{
           |"id":"${fp3.id}",
           |"fee_type":"transaction_based",
           |"user_type":"individual_user",
           |"tier":"basic",
           |"subscription_type":"standard",
           |"transaction_type":"p2p_domestic",
           |"channel":"mobile_application",
           |"other_party":null,
           |"instrument":"visa_debit",
           |"calculation_method":"staircase_with_percentage",
           |"fee_method":"add",
           |"tax_included":false,
           |"currency_code":"USD",
           |"updated_at":null
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return list of fee profile with partial match filter" in {
      val requestId = UUID.randomUUID()
      val criteria = FeeProfileCriteria(
        otherParty = Some("Mas"),
        taxInclusion = None,
        partialMatchFields = Set("other_party"))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (feeProfileManagement.countFeeProfileByCriteria _).when(criteria)
        .returns(Future.successful(Right(1)))
      (feeProfileManagement.getFeeProfileByCriteria _).when(criteria, Nil, None, None)
        .returns(Future.successful(Right(Seq(fp2))))

      val resp = route(app, FakeRequest(GET, s"/fee_profiles?other_party=Mas&partial_match=other_party")
        .withHeaders(Headers("request-id" → requestId.toString))).get

      val expectedJson =
        s"""|{
            |"total":1,
            |"results":[{
            |"id":"${fp2.id}",
            |"fee_type":"subscription_based",
            |"user_type":"individual_user",
            |"tier":"basic",
            |"subscription_type":"platinum",
            |"transaction_type":"p2p_international",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"visa_debit",
            |"calculation_method":"flat_fee",
            |"fee_method":"add",
            |"tax_included":null,
            |"currency_code":"AED",
            |"updated_at":null}],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }
  }
}
