package tech.pegb.backoffice.api.fee

import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerSubscription
import tech.pegb.backoffice.domain.customer.model.{IndividualUserTiers, UserType}
import tech.pegb.backoffice.domain.fee.abstraction.FeeProfileManagement
import tech.pegb.backoffice.domain.fee.dto.FeeProfileToUpdate
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.{FeeCalculationMethod, FeeMethod, FeeType, TaxInclusionTypes}
import tech.pegb.backoffice.domain.fee.model.FeeProfile
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class UpdateFeeProfileControllerSpec extends PegBNoDbTestApp with MockFactory {

  private val feeProfileManagement = stub[FeeProfileManagement]

  override def additionalBindings: Seq[Binding[_]] = {
    super.additionalBindings ++
      Seq(
        bind[FeeProfileManagement].toInstance(feeProfileManagement),
        bind[WithExecutionContexts].to(TestExecutionContext))
  }

  "FeeProfileController" should {

    "update fee profile" in {
      val id = UUID.randomUUID()
      val jsonRequest =
        s"""{
           |  "calculation_method": "flat_fee",
           |  "fee_method": "add",
           |  "tax_included": null,
           |  "max_fee": null,
           |  "min_fee": null,
           |  "fee_amount": 99.99,
           |  "fee_ratio": null,
           |  "ranges": null,
           |  "updated_by": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val mockToUpdate = FeeProfileToUpdate(
        calculationMethod = FeeCalculationMethod("flat_fee"),
        feeMethod = FeeMethod("add"),
        taxInclusion = TaxInclusionTypes.NoTax,
        flatAmount = Some(BigDecimal(99.99)),
        ranges = None,
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom,
        lastUpdatedAt = None)

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
        calculationMethod = mockToUpdate.calculationMethod,
        currencyCode = Currency.getInstance("AED"),
        feeMethod = mockToUpdate.feeMethod,
        taxInclusion = TaxInclusionTypes.NoTax,
        maxFee = None,
        minFee = None,
        percentageAmount = None,
        flatAmount = mockToUpdate.flatAmount,
        ranges = None,
        updatedAt = None,
        updatedBy = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      (feeProfileManagement.getFeeProfile(_: UUID)(_: UUID))
        .when(id, *)
        .returns(Future.successful(Right(expected)))
      (feeProfileManagement.updateFeeProfile(_: UUID, _: FeeProfileToUpdate)(_: UUID))
        .when(id, mockToUpdate, *)
        .returns(Future.successful(Right(expected)))

      val fakeRequest = FakeRequest(PUT, s"/fee_profiles/$id",
        jsonHeaders,
        jsonRequest.toString)

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
      contentAsString(resp) mustBe expectedJson
      status(resp) mustBe OK
    }
  }

}
