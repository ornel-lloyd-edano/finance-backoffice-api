package tech.pegb.backoffice.api.commission

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.{Currency, UUID}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.commission.controllers.impl.CommissionProfileController
import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessTypes, BusinessUserTiers}
import tech.pegb.backoffice.domain.commission.abstraction.CommissionProfileManagement
import tech.pegb.backoffice.domain.commission.dto.{CommissionProfileCriteria, CommissionProfileRangeToCreate, CommissionProfileToCreate}
import tech.pegb.backoffice.domain.commission.model.{CommissionProfile, CommissionProfileRange}
import tech.pegb.backoffice.domain.transaction.model.TransactionType
import tech.pegb.backoffice.domain.types.enum.CommissionCalculationMethods
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future

class CommissionControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  private val service = stub[CommissionProfileManagement]
  private val latestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[CommissionProfileManagement].to(service),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val endpoint = inject[CommissionProfileController].getRoute

  "CommissionManagementController " should {
    "return created profile in POST /commission_profiles" in {
      val ranges = Seq(
        CommissionProfileRangeToCreate(
          from = BigDecimal(10),
          to = BigDecimal(100).some,
          flatAmount = None,
          percentageAmount = BigDecimal(0.5).some),
        CommissionProfileRangeToCreate(
          from = BigDecimal(100),
          to = BigDecimal(1000).some,
          flatAmount = None,
          percentageAmount = BigDecimal(0.25).some))

      val dto = CommissionProfileToCreate(
        uuid = mockRequestId,
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = TransactionType("cashin"),
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.StaircaseFlatPercentage,
        maxCommission = None,
        minCommission = None,
        flatAmount = None,
        percentageAmount = None,
        ranges = ranges.some,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val jsonRequest =
        s"""
           |{
           |  "business_type": "merchant",
           |  "tier": "small",
           |  "subscription_type": "standard",
           |  "transaction_type": "cashin",
           |  "currency_code": "KES",
           |  "channel": null,
           |  "instrument": null,
           |  "calculation_method": "staircase_flat_percentage",
           |  "min_commission": null,
           |  "max_commission": null,
           |  "commission_amount": null,
           |  "commission_ratio": null,
           |  "ranges": [
           |    {
           |      "min": 10,
           |      "max": 100,
           |      "commission_amount": null,
           |      "commission_ratio": 0.5
           |    },
           |    {
           |      "min": 100,
           |      "max": 1000,
           |      "commission_amount": null,
           |      "commission_ratio": 0.25
           |    }
           |  ],
           |  "created_by": "$mockRequestFrom",
           |  "updated_by": "$mockRequestFrom",
           |  "created_at": "${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC}",
           |  "updated_at": "${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC}"
           |}
         """.stripMargin

      val expected = CommissionProfile(
        id = 1,
        uuid = dto.uuid,
        businessType = dto.businessType,
        tier = dto.tier,
        subscriptionType = dto.subscriptionType,
        transactionType = dto.transactionType.underlying,
        currencyId = 1,
        currencyCode = dto.currencyCode,
        channel = dto.channel,
        instrument = dto.instrument,
        calculationMethod = dto.calculationMethod,
        maxCommission = dto.maxCommission,
        minCommission = dto.minCommission,
        commissionAmount = dto.flatAmount,
        commissionRatio = dto.percentageAmount,
        ranges = Seq(
          CommissionProfileRange(
            id = 1,
            commissionProfileId = 1,
            min = BigDecimal(10),
            max = BigDecimal(100).some,
            flatAmount = None,
            percentageAmount = BigDecimal(0.5).some,
            createdAt = now,
            updatedAt = now),
          CommissionProfileRange(
            id = 2,
            commissionProfileId = 1,
            min = BigDecimal(100),
            max = BigDecimal(1000).some,
            flatAmount = None,
            percentageAmount = BigDecimal(0.25).some,
            createdAt = now,
            updatedAt = now)).some,
        createdBy = "pegbuser",
        updatedBy = "pegbuser",
        createdAt = now,
        updatedAt = now,
        deletedAt = None)

      (service.createCommissionProfile _).when(dto)
        .returns(Future.successful(Right(expected)))

      val expectedJson =
        s"""{
           |"id": "${dto.uuid}",
           |"business_type": "merchant",
           |"tier": "small",
           |"subscription_type": "standard",
           |"transaction_type": "cashin",
           |"currency_code": "KES",
           |"channel": null,
           |"instrument": null,
           |"calculation_method": "staircase_flat_percentage",
           |"min_commission": null,
           |"max_commission": null,
           |"commission_amount": null,
           |"commission_ratio": null,
           |"ranges":[
           |{
           |"min": 10,
           |"max": 100,
           |"commission_amount": null,
           |"commission_ratio": 0.5
           |},
           |{
           | "min": 100,
           | "max": 1000,
           | "commission_amount": null,
           | "commission_ratio": 0.25
           |}
           |],
           |"created_by": "pegbuser",
           |"updated_by": "pegbuser",
           |"created_at": "${now.toZonedDateTimeUTC}",
           |"updated_at": "${now.toZonedDateTimeUTC}"
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "").replaceAll(" ", "")

      val fakeRequest = FakeRequest(POST, s"/$endpoint", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "return list of profiles in GET /commission_profiles" in {
      val c1 = CommissionProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = "cashin",
        currencyId = 1,
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.StaircaseFlatAmount,
        maxCommission = None,
        minCommission = None,
        commissionAmount = None,
        commissionRatio = None,
        ranges = None,
        createdBy = "pegbuser",
        updatedBy = "pegbuser",
        createdAt = now,
        updatedAt = now,
        deletedAt = None)

      val c2 = CommissionProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = "cashout",
        currencyId = 1,
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.FlatPercentage,
        minCommission = BigDecimal(10).some,
        maxCommission = BigDecimal(100).some,
        commissionAmount = None,
        commissionRatio = BigDecimal(0.2500).some,
        ranges = None,
        createdBy = "pegbuser",
        updatedBy = "pegbuser",
        createdAt = now,
        updatedAt = now,
        deletedAt = None)

      val criteria = CommissionProfileCriteria(
        partialMatchFields = CommissionProfileController.CommissionProfilePartialMatchFields.filterNot(_ == "disabled"))

      val ordering = Nil

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      (service.countCommissionProfileByCriteria _).when(criteria)
        .returns(Future.successful(Right(2)))
      (service.getCommissionProfileByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(c1, c2))))

      val expectedJson = s"""{
         |"total":2,
         |"results":[
         |{"id": "${c1.uuid}",
         |"business_type": "merchant",
         |"tier": "small",
         |"subscription_type": "standard",
         |"transaction_type": "cashin",
         |"currency_code": "KES",
         |"channel": null,
         |"instrument": null,
         |"calculation_method": "staircase_flat_amount",
         |"min_commission": null,
         |"max_commission": null,
         |"commission_amount": null,
         |"commission_ratio": null,
         |"created_by": "pegbuser",
         |"updated_by": "pegbuser",
         |"created_at": "${now.toZonedDateTimeUTC}",
         |"updated_at": "${now.toZonedDateTimeUTC}"
         |},
         |{"id": "${c2.uuid}",
         |"business_type": "merchant",
         |"tier": "small",
         |"subscription_type": "standard",
         |"transaction_type": "cashout",
         |"currency_code": "KES",
         |"channel": null,
         |"instrument": null,
         |"calculation_method": "flat_percentage",
         |"min_commission": 10,
         |"max_commission": 100,
         |"commission_amount": null,
         |"commission_ratio": 0.25,
         |"created_by": "pegbuser",
         |"updated_by": "pegbuser",
         |"created_at": "${now.toZonedDateTimeUTC}",
         |"updated_at": "${now.toZonedDateTimeUTC}"
         |}],
         |"limit":null,
         |"offset":null
         |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "").replaceAll(" ", "")

      val resp = route(app, FakeRequest(GET, s"/commission_profiles")).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return list of profiles in GET /commission_profiles?business_type=merchant&currency=KES" in {
      val c1 = CommissionProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = "cashin",
        currencyId = 1,
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.StaircaseFlatAmount,
        maxCommission = None,
        minCommission = None,
        commissionAmount = None,
        commissionRatio = None,
        ranges = None,
        createdBy = "pegbuser",
        updatedBy = "pegbuser",
        createdAt = now,
        updatedAt = now,
        deletedAt = None)

      val c2 = CommissionProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = "cashout",
        currencyId = 1,
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.FlatPercentage,
        minCommission = BigDecimal(10).some,
        maxCommission = BigDecimal(100).some,
        commissionAmount = None,
        commissionRatio = BigDecimal(0.2500).some,
        ranges = None,
        createdBy = "pegbuser",
        updatedBy = "pegbuser",
        createdAt = now,
        updatedAt = now,
        deletedAt = None)

      val criteria = CommissionProfileCriteria(
        businessType = BusinessTypes.Merchant.some,
        currency = Currency.getInstance("KES").some,
        partialMatchFields = CommissionProfileController.CommissionProfilePartialMatchFields.filterNot(_ == "disabled"))

      val ordering = Nil

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(criteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      (service.countCommissionProfileByCriteria _).when(criteria)
        .returns(Future.successful(Right(2)))
      (service.getCommissionProfileByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(c1, c2))))

      val expectedJson = s"""{
                            |"total":2,
                            |"results":[
                            |{"id": "${c1.uuid}",
                            |"business_type": "merchant",
                            |"tier": "small",
                            |"subscription_type": "standard",
                            |"transaction_type": "cashin",
                            |"currency_code": "KES",
                            |"channel": null,
                            |"instrument": null,
                            |"calculation_method": "staircase_flat_amount",
                            |"min_commission": null,
                            |"max_commission": null,
                            |"commission_amount": null,
                            |"commission_ratio": null,
                            |"created_by": "pegbuser",
                            |"updated_by": "pegbuser",
                            |"created_at": "${now.toZonedDateTimeUTC}",
                            |"updated_at": "${now.toZonedDateTimeUTC}"
                            |},
                            |{"id": "${c2.uuid}",
                            |"business_type": "merchant",
                            |"tier": "small",
                            |"subscription_type": "standard",
                            |"transaction_type": "cashout",
                            |"currency_code": "KES",
                            |"channel": null,
                            |"instrument": null,
                            |"calculation_method": "flat_percentage",
                            |"min_commission": 10,
                            |"max_commission": 100,
                            |"commission_amount": null,
                            |"commission_ratio": 0.25,
                            |"created_by": "pegbuser",
                            |"updated_by": "pegbuser",
                            |"created_at": "${now.toZonedDateTimeUTC}",
                            |"updated_at": "${now.toZonedDateTimeUTC}"
                            |}],
                            |"limit":null,
                            |"offset":null
                            |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "").replaceAll(" ", "")

      val resp = route(app, FakeRequest(GET, s"/commission_profiles?business_type=merchant&currency=KES")).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return detailed profile with ranges in GET /commission_profiles/:id " in {
      val c1 = CommissionProfile(
        id = 1,
        uuid = UUID.randomUUID(),
        businessType = BusinessTypes.Merchant,
        tier = BusinessUserTiers.Small,
        subscriptionType = "standard",
        transactionType = "cashin",
        currencyId = 1,
        currencyCode = Currency.getInstance("KES"),
        channel = None,
        instrument = None,
        calculationMethod = CommissionCalculationMethods.StaircaseFlatAmount,
        maxCommission = None,
        minCommission = None,
        commissionAmount = None,
        commissionRatio = None,
        ranges = Seq(
          CommissionProfileRange(
            id = 1,
            commissionProfileId = 1,
            min = BigDecimal(10),
            max = BigDecimal(100).some,
            flatAmount = None,
            percentageAmount = BigDecimal(0.5).some,
            createdAt = now,
            updatedAt = now),
          CommissionProfileRange(
            id = 2,
            commissionProfileId = 1,
            min = BigDecimal(100),
            max = BigDecimal(1000).some,
            flatAmount = None,
            percentageAmount = BigDecimal(0.25).some,
            createdAt = now,
            updatedAt = now)).some,
        createdBy = "pegbuser",
        updatedBy = "pegbuser",
        createdAt = now,
        updatedAt = now,
        deletedAt = None)

      (service.getCommissionProfile _).when(c1.uuid)
        .returns(Future.successful(Right(c1)))

      val expectedJson = s"""{"id": "${c1.uuid}",
                            |"business_type": "merchant",
                            |"tier": "small",
                            |"subscription_type": "standard",
                            |"transaction_type": "cashin",
                            |"currency_code": "KES",
                            |"channel": null,
                            |"instrument": null,
                            |"calculation_method": "staircase_flat_amount",
                            |"min_commission": null,
                            |"max_commission": null,
                            |"commission_amount": null,
                            |"commission_ratio": null,
                            |"ranges":[
                            |{
                            |"min": 10,
                            |"max": 100,
                            |"commission_amount": null,
                            |"commission_ratio": 0.5
                            |},
                            |{
                            | "min": 100,
                            | "max": 1000,
                            | "commission_amount": null,
                            | "commission_ratio": 0.25
                            |}
                            |],
                            |"created_by": "pegbuser",
                            |"updated_by": "pegbuser",
                            |"created_at": "${now.toZonedDateTimeUTC}",
                            |"updated_at": "${now.toZonedDateTimeUTC}"
                            |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "").replaceAll(" ", "")

      val resp = route(app, FakeRequest(GET, s"/commission_profiles/${c1.uuid}")).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson

    }

  }

}
