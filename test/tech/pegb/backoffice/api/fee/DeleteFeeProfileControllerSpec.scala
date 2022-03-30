package tech.pegb.backoffice.api.fee

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerSubscription
import tech.pegb.backoffice.domain.customer.model.{IndividualUserTiers, UserType}
import tech.pegb.backoffice.domain.fee.abstraction.FeeProfileManagement
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.{FeeCalculationMethod, FeeMethod, FeeType, TaxInclusionTypes}
import tech.pegb.backoffice.domain.fee.model.FeeProfile
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class DeleteFeeProfileControllerSpec extends PegBNoDbTestApp with MockFactory {

  private val feeProfileManagement = stub[FeeProfileManagement]

  override def additionalBindings: Seq[Binding[_]] = {
    super.additionalBindings ++
      Seq(
        bind[FeeProfileManagement].toInstance(feeProfileManagement),
        bind[WithExecutionContexts].to(TestExecutionContext))
  }

  "FeeProfileController" should {

    "delete fee profile" in {
      val id = UUID.randomUUID()

      val expected = FeeProfile(
        id = id,
        feeType = FeeType("transaction_based"),
        userType = UserType("individual"),
        tier = IndividualUserTiers.Basic,
        subscription = CustomerSubscription("standard"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_money")),
        otherParty = None,
        instrument = None,
        calculationMethod = FeeCalculationMethod("flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        percentageAmount = None,
        flatAmount = Some(BigDecimal(99.99)),
        ranges = None,
        updatedAt = None,
        updatedBy = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      (feeProfileManagement.getFeeProfile(_: UUID)(_: UUID))
        .when(id, *)
        .returns(Future.successful(Right(expected)))
      (feeProfileManagement.deleteFeeProfile(_: UUID, _: String, _: LocalDateTime, _: Option[LocalDateTime])(_: UUID))
        .when(id, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None, *)
        .returns(Future.successful(Right(expected)))

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(DELETE, s"/fee_profiles/$id", jsonHeaders, jsonRequest.toString)

      val resp = route(app, fakeRequest).get
      val expectedJson =
        s"""
           |{"id":"$id",
           |"fee_type":"transaction_based",
           |"user_type":"individual",
           |"tier":"basic",
           |"subscription_type":"standard",
           |"transaction_type":"p2p_domestic",
           |"channel":"mobile_money",
           |"other_party":null,
           |"instrument":null,
           |"calculation_method":"flat_fee",
           |"fee_method":"add",
           |"tax_included":null,
           |"currency_code":"AED",
           |"updated_at":null}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      contentAsString(resp) mustBe expectedJson
      status(resp) mustBe OK
    }

    "delete limit profile (precondition fail)" in {
      val id = UUID.randomUUID()
      val fakeLastUpdateAt = ZonedDateTime.now()
      val jsonRequest =
        s"""{
           |"updated_at": "$fakeLastUpdateAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val expected = FeeProfile(
        id = id,
        feeType = FeeType("transaction_based"),
        userType = UserType("individual"),
        tier = IndividualUserTiers.Basic,
        subscription = CustomerSubscription("standard"),
        transactionType = TransactionType("p2p_domestic"),
        channel = Some(Channel("mobile_money")),
        otherParty = None,
        instrument = None,
        calculationMethod = FeeCalculationMethod("flat_fee"),
        currencyCode = Currency.getInstance("AED"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        percentageAmount = None,
        flatAmount = Some(BigDecimal(99.99)),
        ranges = None,
        updatedAt = None,
        updatedBy = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      (feeProfileManagement.getFeeProfile(_: UUID)(_: UUID))
        .when(id, *)
        .returns(Future.successful(Right(expected)))
      (feeProfileManagement.deleteFeeProfile(_: UUID, _: String, _: LocalDateTime, _: Option[LocalDateTime])(_: UUID))
        .when(id, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, Some(fakeLastUpdateAt.toLocalDateTimeUTC), *)
        .returns(Future.successful(Left(ServiceError.staleResourceAccessError(s"Update failed. Fee profile ${id} has been modified by another process.", mockRequestId.toOption))))

      val fakeRequest = FakeRequest(DELETE, s"/fee_profiles/$id", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val errorJson =
        s"""{"id":"$mockRequestId",
           |"code":"PreconditionFailed",
           |"msg":"Update failed. Fee profile $id has been modified by another process.",
           |"tracking_id":"$mockRequestId"}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe PRECONDITION_FAILED
      contentAsString(resp) mustBe errorJson
    }
  }

}
