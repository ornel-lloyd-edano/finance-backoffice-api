package tech.pegb.backoffice.api.limit

import java.time._
import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.libs.json.JsString
import play.api.mvc.Headers
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerSubscription, CustomerTier}
import tech.pegb.backoffice.domain.customer.model.UserType
import tech.pegb.backoffice.domain.limit.abstraction.LimitManagement
import tech.pegb.backoffice.domain.limit.dto.{LimitProfileCriteria, LimitProfileToCreate, LimitProfileToUpdate}
import tech.pegb.backoffice.domain.limit.model.{LimitProfile, _}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class LimitProfileControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec: ExecutionContext = TestExecutionContext.genericOperations

  private val limitProfileManagement = stub[LimitManagement]
  private val latestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[LimitManagement].to(limitProfileManagement),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock: Clock = Clock.fixed(Instant.ofEpochMilli(1551830400000L), ZoneId.of("UTC"))
  private val id = UUID.randomUUID()

  "LimitProfileController" should {

    val expectedLimitProfile = LimitProfile(
      id = id,
      limitType = LimitType.TransactionBased,
      userType = UserType("individual"),
      tier = CustomerTier("One"),
      subscription = CustomerSubscription("sub"),
      transactionType = Some(TransactionType("cashin")),
      channel = Some(Channel("bank")),
      otherParty = Some("otherParty"),
      instrument = Some("card"),
      interval = Some(TimeIntervals.Daily),
      maxIntervalAmount = Some(BigDecimal(100.50)),
      maxAmountPerTransaction = Some(BigDecimal(50.00)),
      minAmountPerTransaction = Some(BigDecimal(5.00)),
      maxCount = Some(1),
      maxBalanceAmount = None,
      currencyCode = Currency.getInstance("KES"),
      createdBy = mockRequestFrom,
      createdAt = mockRequestDate.toLocalDateTime,
      updatedBy = None,
      updatedAt = None)
    val expectedJson =
      s"""{
         |"id":"$id",
         |"limit_type":"${expectedLimitProfile.limitType}",
         |"user_type":"${expectedLimitProfile.userType.underlying}",
         |"tier":"${expectedLimitProfile.tier.underlying}",
         |"subscription":"${expectedLimitProfile.subscription.underlying}",
         |"transaction_type":"${expectedLimitProfile.transactionType.map(_.underlying).getOrElse("")}",
         |"channel":"${expectedLimitProfile.channel.map(_.underlying).getOrElse("")}",
         |"other_party":"${expectedLimitProfile.otherParty.getOrElse("")}",
         |"instrument":"${expectedLimitProfile.instrument.getOrElse("")}",
         |"currency_code":"${expectedLimitProfile.currencyCode.getCurrencyCode}",
         |"updated_at":null,
         |"max_balance_amount":null,
         |"interval":"${expectedLimitProfile.interval.map(_.toString).getOrElse("")}",
         |"max_amount_per_interval":100.5,
         |"max_amount_per_txn":50.0,
         |"min_amount_per_txn":5.0,
         |"max_count_per_interval":1
         |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "").replaceAll("\\s", "")

    "respond with CREATED when limit profile is successfully created" in {

      val jsonRequest =
        s"""{
           |	"limit_type": "${LimitType.TransactionBased}",
           |	"user_type": "individual",
           |	"tier": "One",
           |	"subscription": "sub",
           |	"transaction_type": "cashin",
           |	"channel": "bank",
           |	"other_party": "otherParty",
           |	"instrument": "card",
           |	"interval": "daily",
           |	"max_amount_per_interval": 100.50,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           | "currency_code":"KES"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val domainDto = LimitProfileToCreate(
        limitType = LimitType.TransactionBased,
        userType = UserType("individual"),
        tier = CustomerTier("One"),
        subscription = CustomerSubscription("sub"),
        transactionType = Some(TransactionType("cashin")),
        channel = Some(Channel("bank")),
        otherParty = Some("otherParty"),
        instrument = Some("card"),
        interval = Some(TimeIntervalWrapper("daily")),
        maxIntervalAmount = Some(BigDecimal(100.50)),
        maxAmount = Some(BigDecimal(50.00)),
        minAmount = Some(BigDecimal(5.00)),
        maxCount = Some(1),
        maxBalance = None,
        currencyCode = Currency.getInstance("KES"),
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      (limitProfileManagement.createLimitProfile(_: LimitProfileToCreate)(_: UUID)).when(domainDto, *)
        .returns(Future.successful(Right(expectedLimitProfile)))

      val fakeRequest = FakeRequest(POST, s"/limit_profiles",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "respond with BAD_REQUEST when limit profile invalidates " in {
      val jsonRequest =
        s"""{
           |	"limit_type": "limitType",
           |	"user_type": "individual_user",
           |	"tier": "One",
           |	"subscription": "sub",
           |	"transaction_type": "cashin",
           |	"channel": "bank",
           |	"other_party": "otherParty",
           |	"instrument": "card",
           |	"interval": "daily",
           |	"max_amount_per_interval": 100  ,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"MalformedRequest",
           |"msg":"Malformed request to create a limit profile. Mandatory field is missing or value of a field is of wrong type."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/limit_profiles",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "find limit profile by id" in {
      (limitProfileManagement.getLimitProfile(_: UUID)(_: UUID)).when(id, *)
        .returns(Future.successful(Right(expectedLimitProfile)))

      val fakeRequest = FakeRequest(GET, s"/limit_profiles/$id")
      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "update limit profile" in {
      val dto = LimitProfileToUpdate(
        maxIntervalAmount = Some(BigDecimal(100)),
        maxAmount = None,
        minAmount = None,
        maxCount = None,
        maxBalanceAmount = None,
        updatedBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = None)
      val jsonRequest =
        s"""{
           |	"max_amount_per_interval": 100,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": null,
           | "updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      (limitProfileManagement.updateLimitProfileValues(_: UUID, _: LimitProfileToUpdate)(_: UUID)).when(id, dto, *)
        .returns(Future.successful(Right(expectedLimitProfile)))

      val fakeRequest = FakeRequest(PUT, s"/limit_profiles/$id", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "delete limit profile" in {
      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (limitProfileManagement.deleteLimitProfile(_: UUID, _: String, _: LocalDateTime, _: Option[LocalDateTime])(_: UUID)).when(id, *, *, None, *)
        .returns(Future.successful(Right(expectedLimitProfile)))

      val fakeRequest = FakeRequest(DELETE, s"/limit_profiles/$id", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "update limit profile fail (precondition fail)" in {
      val fakeLastUpdateAt = ZonedDateTime.now()
      val dto = LimitProfileToUpdate(
        maxIntervalAmount = Some(BigDecimal(100)),
        maxAmount = None,
        minAmount = None,
        maxCount = None,
        maxBalanceAmount = None,
        updatedBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = Some(fakeLastUpdateAt.toLocalDateTimeUTC))
      val jsonRequest =
        s"""{
           |	"max_amount_per_interval": 100,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": null,
           | "updated_at": "$fakeLastUpdateAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (limitProfileManagement.updateLimitProfileValues(_: UUID, _: LimitProfileToUpdate)(_: UUID)).when(id, dto, *)
        .returns(Future.successful(Left(ServiceError.staleResourceAccessError(s"Update failed. Limit profile ${id} has been modified by another process.", mockRequestId.toOption))))

      val fakeRequest = FakeRequest(PUT, s"/limit_profiles/$id", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val errorJson =
        s"""{"id":"$mockRequestId",
           |"code":"PreconditionFailed",
           |"msg":"Update failed. Limit profile $id has been modified by another process.",
           |"tracking_id":"$mockRequestId"}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe PRECONDITION_FAILED
      contentAsString(resp) mustBe errorJson
    }

    "delete limit profile (precondition fail)" in {
      val fakeLastUpdateAt = ZonedDateTime.now()
      val jsonRequest =
        s"""{
           |"updated_at": "$fakeLastUpdateAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (limitProfileManagement.deleteLimitProfile(_: UUID, _: String, _: LocalDateTime, _: Option[LocalDateTime])(_: UUID)).when(id, *, *, Some(fakeLastUpdateAt.toLocalDateTimeUTC), *)
        .returns(Future.successful(Left(ServiceError.staleResourceAccessError(s"Update failed. Limit profile ${id} has been modified by another process.", mockRequestId.toOption))))

      val fakeRequest = FakeRequest(DELETE, s"/limit_profiles/$id", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val errorJson =
        s"""{"id":"$mockRequestId",
           |"code":"PreconditionFailed",
           |"msg":"Update failed. Limit profile $id has been modified by another process.",
           |"tracking_id":"$mockRequestId"}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe PRECONDITION_FAILED
      contentAsString(resp) mustBe errorJson
    }
  }

  "LimitProfileController getLimitProfile" should {
    "respond with LimitProfile" in {
      val id = UUID.randomUUID()

      val expectedLimitProfile = LimitProfile(
        id = id,
        limitType = LimitType.BalanceBased,
        userType = UserType("individual"),
        tier = CustomerTier("One"),
        subscription = CustomerSubscription("sub"),
        transactionType = Some(TransactionType("cashin")),
        channel = Some(Channel("bank")),
        otherParty = Some("otherParty"),
        instrument = Some("card"),
        maxBalanceAmount = Some(BigDecimal(50.00)),
        interval = None,
        maxIntervalAmount = None,
        maxAmountPerTransaction = None,
        minAmountPerTransaction = None,
        maxCount = None,
        currencyCode = Currency.getInstance("KES"),
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTime,
        updatedBy = None,
        updatedAt = None)

      val expectedJson =
        s"""{
           |"id":"${id}",
           |"limit_type":"balance_based",
           |"user_type":"individual",
           |"tier":"One",
           |"subscription":"sub",
           |"transaction_type":"cashin",
           |"channel":"bank",
           |"other_party":"otherParty",
           |"instrument":"card",
           |"currency_code":"KES",
           |"updated_at":null,
           |"max_balance_amount":50.0,
           |"interval":null,
           |"max_amount_per_interval":null,
           |"max_amount_per_txn":null,
           |"min_amount_per_txn":null,
           |"max_count_per_interval":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "").replaceAll("\\s", "")

      (limitProfileManagement.getLimitProfile(_: UUID)(_: UUID)).when(id, *)
        .returns(Future.successful(Right(expectedLimitProfile)))

      val resp = route(app, FakeRequest(GET, s"/limit_profiles/$id")).get

      contentAsString(resp) mustBe expectedJson

    }

    "respond 404 NotFound in /limit_profiles/:id if limit_profile uuid was not found" in {
      val requestId = UUID.randomUUID()
      val fakeUUID = UUID.randomUUID()
      (limitProfileManagement.getLimitProfile(_: UUID)(_: UUID)).when(fakeUUID, *)
        .returns(Future.successful(Left(ServiceError.notFoundError("Limit Profile not Found", requestId.toOption))))

      val resp = route(app, FakeRequest(GET, s"/limit_profiles/$fakeUUID")
        .withHeaders(Headers("request-id" → requestId.toString))).get

      val expected =
        s"""{"id":"$requestId",
           |"code":"NotFound",
           |"msg":"Limit Profile not Found",
           |"tracking_id":"$requestId"}""".stripMargin.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
      (contentAsJson(resp) \ "code").get.toString() mustBe "\"NotFound\""
    }
  }

  "LimitProfileController getLimitProfileByCriteria" should {
    val lp1 = LimitProfile(
      id = id,
      limitType = LimitType.TransactionBased,
      userType = UserType("individual"),
      tier = CustomerTier("One"),
      subscription = CustomerSubscription("platinum"),
      transactionType = Some(TransactionType("cashin")),
      channel = Some(Channel("bank")),
      otherParty = Some("otherParty"),
      instrument = Some("card"),
      interval = Some(TimeIntervals.Daily),
      maxIntervalAmount = Some(BigDecimal(30.00)),
      maxAmountPerTransaction = Some(BigDecimal(50.00)),
      minAmountPerTransaction = Some(BigDecimal(5.00)),
      maxCount = Some(1),
      currencyCode = Currency.getInstance("KES"),
      createdBy = mockRequestFrom,
      createdAt = mockRequestDate.toLocalDateTime,
      updatedBy = None,
      updatedAt = None)

    val lp2 = LimitProfile(
      id = id,
      limitType = LimitType.BalanceBased,
      userType = UserType("business"),
      tier = CustomerTier("Two"),
      subscription = CustomerSubscription("platinum"),
      transactionType = Some(TransactionType("cashin")),
      channel = Some(Channel("bank")),
      otherParty = Some("otherParty"),
      instrument = Some("card"),
      maxBalanceAmount = Some(BigDecimal(50.00)),
      currencyCode = Currency.getInstance("KES"),
      createdBy = mockRequestFrom,
      createdAt = mockRequestDate.toLocalDateTime,
      updatedBy = None,
      updatedAt = None)

    val lp3 = LimitProfile(
      id = id,
      limitType = LimitType.TransactionBased,
      userType = UserType("individual"),
      tier = CustomerTier("One"),
      subscription = CustomerSubscription("standard"),
      transactionType = Some(TransactionType("cashin")),
      channel = Some(Channel("bank")),
      otherParty = Some("NBD"),
      instrument = Some("card"),
      interval = Some(TimeIntervals.Monthly),
      maxIntervalAmount = Some(BigDecimal(40.00)),
      maxAmountPerTransaction = Some(BigDecimal(50.00)),
      minAmountPerTransaction = Some(BigDecimal(5.00)),
      maxCount = Some(1),
      currencyCode = Currency.getInstance("AED"),
      createdBy = mockRequestFrom,
      createdAt = mockRequestDate.toLocalDateTime,
      updatedBy = None,
      updatedAt = None)

    "respond with LimitProfile List satisfying fitler" in {
      val criteria = LimitProfileCriteria(
        userType = Some(UserType("individual")),
        isDeleted = Some(false),
        partialMatchFields = Constants.limitProfilePartialMatchFields.filterNot(_ == "disabled"))

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{
           |"id":"${lp3.id}",
           |"limit_type":"${lp3.limitType}",
           |"user_type":"${lp3.userType.underlying}",
           |"tier":"One",
           |"subscription":"standard",
           |"transaction_type":"cashin",
           |"channel":"bank",
           |"other_party":"NBD",
           |"instrument":"card",
           |"updated_at":null,
           |"currency_code":"AED"
           |},
           |{
           |"id":"${lp1.id}",
           |"limit_type":"${lp1.limitType}",
           |"user_type":"${lp1.userType.underlying}",
           |"tier":"One",
           |"subscription":"platinum",
           |"transaction_type":"cashin",
           |"channel":"bank",
           |"other_party":"otherParty",
           |"instrument":"card",
           |"updated_at":null,
           |"currency_code":"KES"
           |}
           |],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "").replaceAll("\\s", "")

      val ordering = Seq(Ordering("currency_code", Ordering.ASCENDING))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (limitProfileManagement.countLimitProfileByCriteria _).when(criteria)
        .returns(Future.successful(Right(2)))
      (limitProfileManagement.getLimitProfileByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(lp3, lp1))))

      val resp = route(app, FakeRequest(GET, s"/limit_profiles?user_type=individual&order_by=currency_code")).get

      contentAsString(resp) mustBe expectedJson

    }

    "respond with LimitProfile List satisfying partialFilter" in {
      val criteria = LimitProfileCriteria(
        isDeleted = Some(false),
        otherParty = Some("other"),
        partialMatchFields = Set("other_party"))

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{
           |"id":"${lp2.id}",
           |"limit_type":"${lp2.limitType}",
           |"user_type":"${lp2.userType.underlying}",
           |"tier":"Two",
           |"subscription":"platinum",
           |"transaction_type":"cashin",
           |"channel":"bank",
           |"other_party":"otherParty",
           |"instrument":"card",
           |"updated_at":null,
           |"currency_code":"KES"
           |},
           |{
           |"id":"${lp1.id}",
           |"limit_type":"${lp1.limitType}",
           |"user_type":"${lp1.userType.underlying}",
           |"tier":"One",
           |"subscription":"platinum",
           |"transaction_type":"cashin",
           |"channel":"bank",
           |"other_party":"otherParty",
           |"instrument":"card",
           |"updated_at":null,
           |"currency_code":"KES"
           |}
           |],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "").replaceAll("\\s", "")

      val ordering = Seq(Ordering("limit_type", Ordering.ASCENDING), Ordering("max_amount", Ordering.DESCENDING))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)
      (limitProfileManagement.countLimitProfileByCriteria _).when(criteria)
        .returns(Future.successful(Right(2)))
      (limitProfileManagement.getLimitProfileByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(lp2, lp1))))

      val resp = route(app, FakeRequest(GET, s"/limit_profiles?other_party=other&partial_match=other_party&order_by=limit_type,-max_amount_per_txn")).get

      contentAsString(resp) mustBe expectedJson

    }

    "respond error when orderBy contain invalid" in {
      val requestId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/limit_profiles?order_by=deadbeef")
        .withHeaders(Headers("request-id" → requestId.toString))).get

      val json = contentAsJson(resp)

      (json \ "id").get mustBe JsString(requestId.toString)
      (json \ "code").get mustBe JsString("InvalidRequest")
      (json \ "msg").get.toString().contains("invalid field for order_by found.") mustBe true
    }

    "respond error when partial_match contain invalid" in {
      val requestId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/limit_profiles?partial_match=deadbeef")
        .withHeaders(Headers("request-id" → requestId.toString))).get

      val json = contentAsJson(resp)

      (json \ "id").get mustBe JsString(requestId.toString)
      (json \ "code").get mustBe JsString("InvalidRequest")
      (json \ "msg").get.toString().contains("invalid field for partial matching found.") mustBe true
    }

  }
}
