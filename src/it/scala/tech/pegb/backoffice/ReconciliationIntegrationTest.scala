package tech.pegb.backoffice

import java.time.ZonedDateTime

import org.scalatest.Matchers._
import play.api.test.FakeRequest
import tech.pegb.backoffice.util.Implicits._
import play.api.test.Helpers.{PUT, contentAsJson, contentAsString, route, status, _}


class ReconciliationIntegrationTest extends PlayIntegrationTest {
  override val mayBeDbName = Some("reports")

  "Recon API positive tests " should {
    "Return updated record on resolve" in {
      val id = "123"

      val updateJson =
        s"""{
           |"comments":"manual txn added to solve this",
           |"updated_at":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")


      val updateRequest = FakeRequest(PUT, s"/internal_recons/$id",
        jsonHeaders,
        updateJson)

      val updateResp = route(app, updateRequest).get

      val updateJsonResult =
        s"""
           |{"id":"123",
           |"account_number":"some account num",
           |"account_type":"distribution",
           |"account_main_type":"liability",
           |"currency":"KES",
           |"user_id":"2205409c-1c83-11e9-a2a9-000c297e3e45",
           |"user_full_name":null,
           |"customer_name":null,
           |"date":"2019-05-15T00:00:00Z",
           |"total_value":3200.5000,
           |"difference":-6798.5000,
           |"total_txn":9999.0000,
           |"txn_count":100,
           |"incidents":1,
           |"status":"SOLVED",
           |"comments":"manual txn added to solve this",
           |"updated_at":"${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(updateResp) mustBe OK
      contentAsString(updateResp) mustBe updateJsonResult
    }

   "Return error when status of summary to resolve is not problem" in {
      val id = "123"

      val updateJson =
        s"""{
           |"comments":"manual txn added to solve this",
           |"updated_at":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")


      val updateRequest = FakeRequest(PUT, s"/internal_recons/$id",
        jsonHeaders,
        updateJson)

      val updateResp = route(app, updateRequest).get

      status(updateResp) mustBe BAD_REQUEST
      (contentAsJson(updateResp) \ "msg").get.toString should include("Status of recon daily to resolve must be 'FAIL'")
    }

    "Return precondition_failed updated_at does not match " in {
      val id = "124"

      val updateJson =
        s"""{
           |"comments":"manual txn added to solve this",
           |"updated_at":"${ZonedDateTime.now()}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")


      val updateRequest = FakeRequest(PUT, s"/internal_recons/$id",
        jsonHeaders,
        updateJson)

      val updateResp = route(app, updateRequest).get

      status(updateResp) mustBe PRECONDITION_FAILED
      (contentAsJson(updateResp) \ "msg").get.toString should include("Update failed. Internal Recon daily Summary 124 has been modified by another process.")
    }

    "Return not_found when summary doesn't exist" in {
      val id = "deadbeef"

      val updateJson =
        s"""{
           |"comments":"manual txn added to solve this",
           |"updated_at":"${ZonedDateTime.now()}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")


      val updateRequest = FakeRequest(PUT, s"/internal_recons/$id",
        jsonHeaders,
        updateJson)

      val updateResp = route(app, updateRequest).get

      status(updateResp) mustBe NOT_FOUND
      (contentAsJson(updateResp) \ "msg").get.toString should include("Recon Daily Summary with id: deadbeef not Found")
    }
  }

}
