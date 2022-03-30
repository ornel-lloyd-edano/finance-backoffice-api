package tech.pegb.backoffice

import java.util.UUID

import akka.http.scaladsl.model.MediaTypes
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import tech.pegb.backoffice.util.{AppConfig, Utils}


class IndividualUserIntegrationTest extends PlayIntegrationTest /*with Results*/ with ScalaFutures {

	private val config = inject[AppConfig]

	override def beforeAll(): Unit = {
		super.beforeAll()
		createSuperAdmin()
	}

  "Users API" should {

		val doneBy = "TEST_USERNAME"
		val expectedMsisdn = JsString("+971589721075")
		val expectedFullName = JsString("imelda marcos")

		val conf = inject[Configuration]
		// val appConfig = inject[AppConfig]
		val requestDateHeaderKey = conf.get[String]("http-header-keys.request-date")
		val requestFromHeaderKey = conf.get[String]("http-header-keys.request-from")

		def defaultHeaders = Seq(
			HeaderNames.ACCEPT → MediaTypes.`application/json`.value,
			HeaderNames.CONTENT_TYPE → MediaTypes.`application/json`.value,
			requestDateHeaderKey → Utils.now().toString,
			requestFromHeaderKey → doneBy,
			"request-id" → UUID.randomUUID().toString,
			AuthHeader)

		"find individual user in getIndividualUser" in {
			val resp = route(app, FakeRequest(GET, s"/api/individual_users/$defaultUserUuid")
				.withHeaders(defaultHeaders: _*)).get

			status(resp) mustBe OK
			val responseJson = contentAsJson(resp)
			responseJson.isInstanceOf[JsObject] mustBe true
			val iuJson = responseJson.asInstanceOf[JsObject]
			iuJson("msisdn") mustBe expectedMsisdn
			iuJson("full_name") mustBe expectedFullName
		}

		"find individual user in by full_name with partial match" in {
			val resp = route(app, FakeRequest(GET, s"/api/individual_users?full_name=marcos&order_by=created_at")
				.withHeaders(defaultHeaders: _*)).get

			status(resp) mustBe OK

			val expectedResponse =
  s"""{
     |"total":3,
     |"results":[{
         |"id":"efe3b069-476e-4e36-8d22-53176438f55f",
         |"username":"+97123456789",
         |"tier":"basic",
         |"segment":"new",
         |"subscription":"standard",
         |"email":null,
         |"status":"waiting_for_activation",
         |"msisdn":"+97123456789",
         |"individual_user_type":"standard",
         |"alias":"marcos",
         |"full_name":"Test",
         |"gender":"M",
         |"person_id":"A1",
         |"document_number":null,
         |"document_type":null,
         |"document_model":null,
         |"birth_date":null,
         |"birth_place":null,
         |"nationality":null,
         |"occupation":null,
         |"company_name":null,
         |"employer":null,
         |"created_at":"2019-01-20T09:15:30Z",
         |"created_by":"wallet_api",
         |"updated_at":"2019-01-20T09:15:30Z",
         |"updated_by":"wallet_api",
         |"activated_at":null
         |},
         |{"id":"910f02a0-48ef-418d-ac0a-06eff7ac9c90",
         |"username":"+971544451674",
         |"tier":"basic",
         |"segment":"new",
         |"subscription":"standard",
         |"email":null,
         |"status":"waiting_for_activation",
         |"msisdn":"+971544451674",
         |"individual_user_type":"standard",
         |"alias":"marcos",
         |"full_name":"Test",
         |"gender":"M",
         |"person_id":"A1",
         |"document_number":null,
         |"document_type":null,
         |"document_model":null,
         |"birth_date":null,
         |"birth_place":null,
         |"nationality":null,
         |"occupation":null,
         |"company_name":null,
         |"employer":null,
         |"created_at":"2019-01-20T09:50:55Z",
         |"created_by":"wallet_api",
         |"updated_at":"2019-01-20T09:50:55Z",
         |"updated_by":"wallet_api",
         |"activated_at":null
         |},
         |{
         |"id":"7f66c98c-9c45-4995-8829-70a62181df86",
         |"username":"+971589721075",
         |"tier":"basic",
         |"segment":"new",
         |"subscription":"standard",
         |"email":null,
         |"status":"waiting_for_activation",
         |"msisdn":"+971589721075",
         |"individual_user_type":"standard",
         |"alias":"Maria",
         |"full_name":"imelda marcos",
         |"gender":"F",
         |"person_id":"A2",
         |"document_number":null,
         |"document_type":null,
         |"document_model":null,
         |"birth_date":null,
         |"birth_place":null,
         |"nationality":null,
         |"occupation":null,
         |"company_name":null,
         |"employer":null,
         |"created_at":"2019-01-20T12:43:15Z",
         |"created_by":"wallet_api",
         |"updated_at":"2019-01-20T12:43:15Z",
         |"updated_by":"wallet_api",
         |"activated_at":null
         |}],
     |"limit":null,
     |"offset":null}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
		}

		"fail to create account for non-active user" in {
			val passiveUserId = "bdf50f79-0f52-4b0a-86b9-2d30c5e57c8b"
			val jsonPayload =
				s"""
					 					|{"type":"standard_wallet",
					 					|"currency":"USD"}
        """.stripMargin
			val fakeRequest = FakeRequest(POST, s"/api/individual_users/$passiveUserId/accounts")
				.withBody(jsonPayload)
				.withHeaders(defaultHeaders: _*)
			val resp = route(app, fakeRequest).get

			status(resp) mustBe BAD_REQUEST

			contentAsString(resp).contains(s"Cannot create account for user $passiveUserId: User is deactivated") mustBe true
		}

		"activate individual user" in {
			val request = FakeRequest(PUT, s"/api/individual_users/$defaultUserUuid/activate")
				.withHeaders(defaultHeaders: _*)
			val resp = route(app, request).get

			status(resp) mustBe OK
			val jsonResponse = contentAsJson(resp)
			jsonResponse.isInstanceOf[JsObject] mustBe true
			jsonResponse.asInstanceOf[JsObject]("status") mustBe JsString(config.ActiveUserStatus)
		}

		"create account if correct AccountToCreate js format" in {
			val jsonPayload =
				s"""
					|{"type":"standard_wallet",
					|"currency":"INR"}
        """.stripMargin
			val fakeRequest = FakeRequest(POST, s"/api/individual_users/$defaultUserUuid/accounts")
				.withBody(jsonPayload)
				.withHeaders(defaultHeaders: _*)
			val resp = route(app, fakeRequest).get

			status(resp) mustBe CREATED
		}

		"create account should fail if accountToCreate json is missing something" in {
			val jsonRequest =
				s"""
					|{"customer_id":"$defaultUserUuid",
					|"type":"standard_wallet"}
        """.stripMargin

			val fakeRequest = FakeRequest(POST, s"/api/individual_users/$defaultUserUuid/accounts")
				.withBody(jsonRequest)
				.withHeaders(jsonHeaders)

			val resp = route(app, fakeRequest).get
			val expectedResponse =
				s"""
					 |{
					 |"id":"${mockRequestId}",
					 |"code":"MalformedRequest",
					 |"msg":"Malformed request to create individual_user account. Mandatory field is missing or value of a field is of wrong type."
					 |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

			contentAsString(resp) mustBe expectedResponse
		}

		"get individual users by criteria should return paginated result" in {
			val request = FakeRequest(GET, s"/api/individual_users?status=active&limit=1")
				.withHeaders(defaultHeaders: _*)
			val resp = route(app, request).get

			status(resp) mustBe OK
			val jsonResponse = contentAsJson(resp)
			jsonResponse.isInstanceOf[JsObject] mustBe true
			val collectionResponse = jsonResponse.asInstanceOf[JsObject]
			val totalUsers = collectionResponse("total").asInstanceOf[JsNumber].value.toIntExact
			totalUsers mustBe 2
			val foundUsers = collectionResponse("results").asInstanceOf[JsArray].value
			foundUsers.size mustBe 1
			val user = foundUsers.head.asInstanceOf[JsObject]
			user("status") mustBe JsString(config.ActiveUserStatus)
		}

		"get all individual users with matching msisdn as PaginatedResult" in {
			val msisdnLike = "97154"
			val request = FakeRequest(GET, s"/api/individual_users?msisdn=$msisdnLike&partial_match=msisdn&limit=1")
				.withHeaders(defaultHeaders: _*)
			val resp = route(app, request).get

			status(resp) mustBe OK
			val jsonResponse = contentAsJson(resp)
			jsonResponse.isInstanceOf[JsObject] mustBe true
			val collectionResponse = jsonResponse.asInstanceOf[JsObject]
			val totalUsers = collectionResponse("total").asInstanceOf[JsNumber].value.toIntExact
			totalUsers mustBe 4
			val foundUsers = collectionResponse("results").asInstanceOf[JsArray].value
			foundUsers.size mustBe 1
			val user = foundUsers.head.asInstanceOf[JsObject]
			user("msisdn").toString().contains(msisdnLike) mustBe true
		}

		"get all individual users with matching msisdn as PaginatedResult with order_by" in {
			val msisdnLike = "97154"
			val request = FakeRequest(GET, s"/api/individual_users?msisdn=$msisdnLike&partial_match=msisdn&order_by=msisdn")
				.withHeaders(defaultHeaders: _*)
			val resp = route(app, request).get

			status(resp) mustBe OK
			val jsonResponse = contentAsJson(resp)
			jsonResponse.isInstanceOf[JsObject] mustBe true
			val collectionResponse = jsonResponse.asInstanceOf[JsObject]
			val totalUsers = collectionResponse("total").asInstanceOf[JsNumber].value.toIntExact
			totalUsers mustBe 4
			val foundUsers = collectionResponse("results").asInstanceOf[JsArray].value
			foundUsers.size mustBe 4
			val users = foundUsers.map(_.asInstanceOf[JsObject]("msisdn").toString())
			users mustBe Seq("\"+971544451674\"", "\"+971544451680\"", "\"+971544451683\"", "\"+971544550982\"")
		}


		"get all individual users with matching user_id as PaginatedResult" in {
			val userIdLike = "910f02a0"
			val request = FakeRequest(GET, s"/api/individual_users?user_id=$userIdLike&partial_match=user_id&limit=1")
				.withHeaders(defaultHeaders: _*)
			val resp = route(app, request).get

			status(resp) mustBe OK
			val jsonResponse = contentAsJson(resp)
			jsonResponse.isInstanceOf[JsObject] mustBe true
			val collectionResponse = jsonResponse.asInstanceOf[JsObject]
			val totalUsers = collectionResponse("total").asInstanceOf[JsNumber].value.toIntExact
			totalUsers mustBe 1
			val foundUsers = collectionResponse("results").asInstanceOf[JsArray].value
			foundUsers.size mustBe 1
			val user = foundUsers.head.asInstanceOf[JsObject]
			user("id").toString().contains(userIdLike) mustBe true
		}

		"return deactivated individual user after deactivateIndividualUser" in {
			val request = FakeRequest(DELETE, s"/api/individual_users/$defaultUserUuid")
				.withHeaders(defaultHeaders: _*)
			val resp = route(app, request).get

			status(resp) mustBe OK
			val jsonResponse = contentAsJson(resp)
			jsonResponse.isInstanceOf[JsObject] mustBe true
			jsonResponse.asInstanceOf[JsObject]("status") mustBe JsString(config.PassiveUserStatus)
		}

		/*"return updated individual user after updateIndividualUser" in {
			val mockCustomerId = UUID.randomUUID()
			val mockIndividualUserToUpdate = IndividualUserToUpdate(msisdn = "971544451679")
			val expectedUpdatedIndividualUser = IndividualUser.getEmpty.copy(id = mockCustomerId, msisdn = Msisdn(mockIndividualUserToUpdate.msisdn))
			val mockBackofficeUserWhoUpdated = doneBy
			val timeUpdated = doneAt.toLocalDateTimeUTC

			(customerUpdate.updateIndividualUser(_: UUID, _: DomainIndividualUserToUpdate, _: String, _: LocalDateTime))
				.when(mockCustomerId, mockIndividualUserToUpdate.asDomain, mockBackofficeUserWhoUpdated, timeUpdated)
				.returns(Future.successful(Right(expectedUpdatedIndividualUser)))

			val resp = route(app, FakeRequest(PUT, s"/api/individual_users/${mockCustomerId.toString}")
				.withBody(mockIndividualUserToUpdate.toJson)
				.withHeaders(Headers(Seq(
					"Content-type" → "application/json",
					requestDateHeaderKey → doneAt.toString, requestFromHeaderKey → doneBy, "request-id" → UUID.randomUUID().toString): _*))).get

			status(resp) mustBe OK
			contentAsString(resp) mustBe expectedUpdatedIndividualUser.asApi.toJson
		}

		"return an individual user's accounts in getIndividualUserAccounts" in {

			val mockCustomerId = UUID.randomUUID()
			val expected = Seq(
				Account.getEmpty.copy(customerId = mockCustomerId, currency = Currency.getInstance("USD")),
				Account.getEmpty.copy(customerId = mockCustomerId, currency = Currency.getInstance("AED")),
				Account.getEmpty.copy(customerId = mockCustomerId, currency = Currency.getInstance("KES")))

			(customerAccount.getAccountsByCriteria(_: Option[UUID], _: Option[Boolean], _: Option[String], _: Option[String], _: Option[String], _: Seq[(String, String)], _: Option[Int], _: Option[Int]))
				.when(Option(mockCustomerId), None, None, None, None, Seq(), None, None)
				.returns(Future.successful(Right(expected)))

			val resp = route(app, FakeRequest(GET, s"/api/individual_users/${mockCustomerId}/accounts")
				.withHeaders(Headers(Seq(requestDateHeaderKey → doneAt.toString, requestFromHeaderKey → doneBy, "request-id" → UUID.randomUUID().toString): _*))).get

			status(resp) mustBe OK

			contentAsString(resp) mustBe expected.map(_.asApi).toJson
		}

		"return a specific account of an individual user in getIndividualUserAccount" in {

			val mockCustomerId = UUID.randomUUID()
			val mockAccountId = UUID.randomUUID()
			val expected = Seq(
				Account.getEmpty.copy(customerId = mockCustomerId, currency = Currency.getInstance("USD")),
				Account.getEmpty.copy(customerId = mockCustomerId, currency = Currency.getInstance("AED")),
				Account.getEmpty.copy(id = mockAccountId, customerId = mockCustomerId, currency = Currency.getInstance("KES")))

			(customerAccount.getAccountsByCriteria(_: Option[UUID], _: Option[Boolean], _: Option[String], _: Option[String], _: Option[String], _: Seq[(String, String)], _: Option[Int], _: Option[Int]))
				.when(Option(mockCustomerId), None, None, None, None, Seq(), None, None)
				.returns(Future.successful(Right(expected)))

			val resp = route(app, FakeRequest(GET, s"/api/individual_users/${mockCustomerId}/accounts/${mockAccountId}")
				.withHeaders(Headers(Seq(requestDateHeaderKey → doneAt.toString, requestFromHeaderKey → doneBy, "request-id" → UUID.randomUUID().toString): _*))).get

			status(resp) mustBe OK

			contentAsString(resp) mustBe expected.find(_.id == mockAccountId).map(_.asApi).get.toJson
		}

		"activate an inactive account in activateIndividualUserAccount" in {

			val mockCustomerId = UUID.randomUUID()
			val mockAccountId = UUID.randomUUID()
			val activatedAt = doneAt.toLocalDateTimeUTC
			val expectedActiveAccount = Account.getEmpty.copy(
				customerId = mockCustomerId,
				currency = Currency.getInstance("USD"), accountStatus = AccountStatus("Active"))
			(customerAccount.activateIndividualUserAccount(_: UUID, _: UUID, _: String, _: LocalDateTime))
				.when(mockCustomerId, mockAccountId, doneBy, activatedAt)
				.returns(Future.successful(Right(expectedActiveAccount)))

			val resp = route(app, FakeRequest(PUT, s"/api/individual_users/${mockCustomerId}/accounts/${mockAccountId}/activate")
				.withHeaders(Headers(Seq(requestDateHeaderKey → doneAt.toString, requestFromHeaderKey → doneBy, "request-id" → UUID.randomUUID().toString): _*))).get

			status(resp) mustBe OK

			contentAsString(resp) mustBe expectedActiveAccount.asApi.toJson
		}

		"close an active account if no balance remain in closeIndividualUserAccount" in {

			val mockCustomerId = UUID.randomUUID()
			val mockAccountId = UUID.randomUUID()
			val closedAt = doneAt.toLocalDateTimeUTC
			val expectedClosedAccount = Account.getEmpty.copy(
				customerId = mockCustomerId,
				currency = Currency.getInstance("USD"), accountStatus = AccountStatus("CLOSED"))
			(customerAccount.closeIndividualUserAccount(_: UUID, _: UUID, _: String, _: LocalDateTime))
				.when(mockCustomerId, mockAccountId, doneBy, closedAt)
				.returns(Future.successful(Right(expectedClosedAccount)))

			val resp = route(app, FakeRequest(DELETE, s"/api/individual_users/${mockCustomerId}/accounts/${mockAccountId}/close")
				.withHeaders(Headers(Seq(requestDateHeaderKey → doneAt.toString, requestFromHeaderKey → doneBy, "request-id" → UUID.randomUUID().toString): _*))).get

			status(resp) mustBe OK

			contentAsString(resp) mustBe expectedClosedAccount.asApi.toJson
		}*/

	}
}