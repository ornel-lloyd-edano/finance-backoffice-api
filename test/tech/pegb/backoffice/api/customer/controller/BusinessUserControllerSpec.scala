package tech.pegb.backoffice.api.customer.controller

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.customer.controllers.BusinessUserController
import tech.pegb.backoffice.api.customer.controllers.impl.{BusinessUserController ⇒ ControllerConstants}
import tech.pegb.backoffice.api.customer.dto.{CustomerExternalAccountToCreate, CustomerTxnConfigToCreate, ExternalAccountToUpdate, TxnConfigToUpdate}
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.abstraction.{AddressManagement, BusinessUserManagement, ContactManagement}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerStatus, Msisdn}
import tech.pegb.backoffice.domain.customer.model._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.account.abstraction.ExternalAccountManagement
import tech.pegb.backoffice.domain.account.model.ExternalAccount
import tech.pegb.backoffice.domain.businessuserapplication.model.AddressCoordinates
import tech.pegb.backoffice.domain.customer.dto.{ContactAddressToCreate, ContactAddressToUpdate, ContactToCreate, ContactToUpdate}
import tech.pegb.backoffice.domain.customer.model
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionConfigManagement
import tech.pegb.backoffice.domain.transaction.model.TxnConfig
import tech.pegb.backoffice.mapping.api.domain.account.Implicits._
import tech.pegb.backoffice.mapping.api.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.api.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.api.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future

class BusinessUserControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  private val businessUserManagement = stub[BusinessUserManagement]
  private val contactManagement = stub[ContactManagement]
  private val addressManagement = stub[AddressManagement]
  private val externalAccountMgmt = stub[ExternalAccountManagement]
  private val txnConfigMgmt = stub[TransactionConfigManagement]
  private val latestVersion = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[BusinessUserManagement].to(businessUserManagement),
      bind[ContactManagement].to(contactManagement),
      bind[AddressManagement].to(addressManagement),
      bind[ExternalAccountManagement].to(externalAccountMgmt),
      bind[TransactionConfigManagement].to(txnConfigMgmt),
      bind[LatestVersionService].to(latestVersion),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val endpoint = inject[BusinessUserController].getRoute
  val config = inject[AppConfig]
  "BusinessUserController" should {
    "return velocity portal users json in GET /business_users/{id}/velocity_portal_users" in {
      val vp1 = VelocityPortalUser(
        uuid = UUID.randomUUID(),
        name = "Henry",
        middleName = None,
        surname = "Sy",
        fullName = "Henry Sy",
        msisdn = Msisdn("+63911111"),
        email = Email("h.sy@gmail.com"),
        username = "h.sy@gmail.com",
        role = "admin",
        status = CustomerStatus("active"),
        lastLoginAt = now.some,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some)

      val vp2 = VelocityPortalUser(
        uuid = UUID.randomUUID(),
        name = "Atsi",
        middleName = "Sy".some,
        surname = "Tan",
        fullName = "Atsi Sy Tan",
        msisdn = Msisdn("+639222222"),
        email = Email("atsi@gmail.com"),
        username = "atsi_sy",
        role = "admin",
        status = CustomerStatus("active"),
        lastLoginAt = now.some,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some)

      val vp3 = VelocityPortalUser(
        uuid = UUID.randomUUID(),
        name = "Shobe",
        middleName = None,
        surname = "Sy",
        fullName = "Shobe Sy",
        msisdn = Msisdn("+639333333"),
        email = Email("shobe.sy@gmail.com"),
        username = "shobe_sy",
        role = "admin",
        status = CustomerStatus("active"),
        lastLoginAt = now.some,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some)

      val userId = UUID.randomUUID()

      (businessUserManagement.getVelocityUsers _).when(userId, Nil, None, None)
        .returns(Future.successful(Seq(vp1, vp2, vp3).asRight[ServiceError]))

      (businessUserManagement.countVelocityUsers _).when(userId)
        .returns(Future.successful(3.asRight[ServiceError]))

      val fakeRequest = FakeRequest(GET, s"/$endpoint/$userId/velocity_portal_users").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{
           |"total":3,
           |"results":[
           |{"id":"${vp1.uuid.toString}",
           |"name":"Henry",
           |"middle_name":null,
           |"surname":"Sy",
           |"full_name":"Henry Sy",
           |"msisdn":"+63911111",
           |"email":"h.sy@gmail.com",
           |"username":"h.sy@gmail.com",
           |"role":"admin",
           |"status":"active",
           |"last_login_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr}
           |},
           |{"id":"${vp2.uuid.toString}",
           |"name":"Atsi",
           |"middle_name":"Sy",
           |"surname":"Tan",
           |"full_name":"Atsi Sy Tan",
           |"msisdn":"+639222222",
           |"email":"atsi@gmail.com",
           |"username":"atsi_sy",
           |"role":"admin",
           |"status":"active",
           |"last_login_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr}
           |},
           |{"id":"${vp3.uuid.toString}",
           |"name":"Shobe",
           |"middle_name":null,
           |"surname":"Sy",
           |"full_name":"Shobe Sy",
           |"msisdn":"+639333333",
           |"email":"shobe.sy@gmail.com",
           |"username":"shobe_sy",
           |"role":"admin",
           |"status":"active",
           |"last_login_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr}
           |}],
           |"limit":null,
           |"offset":null}""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "return success on reset Velocity portal pin" in {
      val userId = UUID.randomUUID()
      val vpUserId = UUID.randomUUID()
      val reason = "password forgotten"
      val fakeUpdatedAt = LocalDateTime.now.toZonedDateTimeUTC

      val jsonRequest =
        s"""{
           |"reason":"$reason",
           |"updated_at":"$fakeUpdatedAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (businessUserManagement.resetVelocityUserPin _)
        .when(userId, vpUserId, reason, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, fakeUpdatedAt.toLocalDateTimeUTC.some)
        .returns(Future.successful(().asRight[ServiceError]))

      val fakeRequest = FakeRequest(PUT, s"/business_users/$userId/velocity_portal_users/$vpUserId/reset_pin",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe NO_CONTENT
    }

    "return velocity portal user json in GET /business_users/{id}/velocity_portal_users/{vp_user_id}" in {
      val vp1 = VelocityPortalUser(
        uuid = UUID.randomUUID(),
        name = "Henry",
        middleName = None,
        surname = "Sy",
        fullName = "Henry Sy",
        msisdn = Msisdn("+63911111"),
        email = Email("h.sy@gmail.com"),
        username = "h.sy@gmail.com",
        role = "admin",
        status = CustomerStatus("active"),
        lastLoginAt = now.some,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some)

      val userId = UUID.randomUUID()

      (businessUserManagement.getVelocityUsersById _).when(userId, vp1.uuid)
        .returns(Future.successful(vp1.asRight[ServiceError]))

      val fakeRequest = FakeRequest(GET, s"/$endpoint/$userId/velocity_portal_users/${vp1.uuid}").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":"${vp1.uuid.toString}",
           |"name":"Henry",
           |"middle_name":null,
           |"surname":"Sy",
           |"full_name":"Henry Sy",
           |"msisdn":"+63911111",
           |"email":"h.sy@gmail.com",
           |"username":"h.sy@gmail.com",
           |"role":"admin",
           |"status":"active",
           |"last_login_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson
      status(resp) mustBe OK
    }

    "return list of business_user contacts" in {
      val userId = UUID.randomUUID()

      val c1 = Contact(
        id = 1,
        uuid = UUID.randomUUID(),
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().some,
        userId = 1.some,
        userUUID = UUID.randomUUID().some,
        contactType = ContactTypes.BusinessOwner,
        name = "Henry",
        middleName = None,
        surname = "Sy",
        phoneNumber = Msisdn("+639111111"),
        email = Email("h.sy@gmail.com"),
        idType = "Not Available",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        vpUserId = 1.some,
        vpUserUUID = None,
        isActive = true)

      val c2 = Contact(
        id = 2,
        uuid = UUID.randomUUID(),
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().some,
        userId = 1.some,
        userUUID = UUID.randomUUID().some,
        contactType = ContactTypes.Associate,
        name = "Shobe",
        middleName = None,
        surname = "Sy",
        phoneNumber = Msisdn("+63922222"),
        email = Email("s.sy@gmail.com"),
        idType = "Not Available",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        vpUserId = None,
        vpUserUUID = None,
        isActive = true)

      (contactManagement.getContactInfo _).when(userId, Nil, None, None)
        .returns(Future.successful(Seq(c1, c2).asRight[ServiceError]))

      val fakeRequest = FakeRequest(GET, s"/$endpoint/$userId/contacts").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{"id":"${c1.uuid}",
           |"contact_type":"business_owner",
           |"name":"Henry",
           |"middle_name":null,
           |"surname":"Sy",
           |"phone_number":"+639111111",
           |"email":"h.sy@gmail.com",
           |"id_type":"Not Available",
           |"created_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr}
           |},
           |{"id":"${c2.uuid}",
           |"contact_type":"associate",
           |"name":"Shobe",
           |"middle_name":null,
           |"surname":"Sy",
           |"phone_number":"+63922222",
           |"email":"s.sy@gmail.com",
           |"id_type":"Not Available",
           |"created_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr}}],
           |"limit":null,
           |"offset":null}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson
      status(resp) mustBe OK

    }

    "return business_user contacts in get by id" in {
      val userId = UUID.randomUUID()

      val c1 = Contact(
        id = 1,
        uuid = UUID.randomUUID(),
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().some,
        userId = 1.some,
        userUUID = UUID.randomUUID().some,
        contactType = ContactTypes.BusinessOwner,
        name = "Henry",
        middleName = None,
        surname = "Sy",
        phoneNumber = Msisdn("+639111111"),
        email = Email("h.sy@gmail.com"),
        idType = "Not Available",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        vpUserId = 1.some,
        vpUserUUID = None,
        isActive = true)

      (contactManagement.getContactInfoById _).when(userId, c1.uuid)
        .returns(Future.successful(c1.asRight[ServiceError]))

      val fakeRequest = FakeRequest(GET, s"/$endpoint/$userId/contacts/${c1.uuid}").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""{"id":"${c1.uuid}",
          |"contact_type":"business_owner",
          |"name":"Henry",
          |"middle_name":null,
          |"surname":"Sy",
          |"phone_number":"+639111111",
          |"email":"h.sy@gmail.com",
          |"id_type":"Not Available",
          |"created_by":"pegbuser",
          |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
          |"updated_by":"pegbuser",
          |"updated_at":${now.toZonedDateTimeUTC.toJsonStr}
          |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson
      status(resp) mustBe OK

    }

    "return success on create contact" in {
      val userId = UUID.randomUUID()

      val jsonRequest = """{
                          |  "contact_type": "business_owner",
                          |  "name": "John",
                          |  "middle_name": "Smith",
                          |  "surname": "Doe",
                          |  "phone_number": "+97188888888",
                          |  "email": "j.doe@gmail.com",
                          |  "id_type": "national_id"
                          |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = ContactToCreate(
        uuid = mockRequestId,
        userUuid = userId,
        contactType = "business_owner",
        name = "John",
        middleName = "Smith".some,
        surname = "Doe",
        phoneNumber = Msisdn("+97188888888"),
        email = Email("j.doe@gmail.com"),
        idType = "national_id",
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        isActive = true)

      val contact = model.Contact(
        id = 1,
        uuid = mockRequestId,
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().some,
        userId = 1.some,
        userUUID = UUID.randomUUID().some,
        contactType = ContactTypes.BusinessOwner,
        name = "John",
        middleName = "Smith".some,
        surname = "Doe",
        phoneNumber = Msisdn("+97188888888"),
        email = Email("j.doe@gmail.com"),
        idType = "national_id",
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some,
        vpUserId = None,
        vpUserUUID = None,
        isActive = true)

      (contactManagement.insertContactInfo _)
        .when(dto)
        .returns(Future.successful(contact.asRight[ServiceError]))

      val fakeRequest = FakeRequest(POST, s"/business_users/$userId/contacts",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expected =
        s"""
           |{
           |"id":"$mockRequestId",
           |"contact_type":"business_owner",
           |"name":"John",
           |"middle_name":"Smith",
           |"surname":"Doe",
           |"phone_number":"+97188888888",
           |"email":"j.doe@gmail.com",
           |"id_type":"national_id",
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expected
    }

    "return success on update contact" in {
      val userId = UUID.randomUUID()
      val contactId = UUID.randomUUID()

      val jsonRequest = s"""{
                          |  "contact_type": "business_owner",
                          |  "name": "John",
                          |  "middle_name": "Smith",
                          |  "surname": "Doe",
                          |  "phone_number": "+97188888888",
                          |  "email": "j.doe@gmail.com",
                          |  "id_type": "national_id",
                          |  "updated_at": "${now.toZonedDateTimeUTC}"
                          |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = ContactToUpdate(
        contactType = "business_owner".some,
        name = "John".some,
        middleName = "Smith".some,
        surname = "Doe".some,
        phoneNumber = Msisdn("+97188888888").some,
        email = Email("j.doe@gmail.com").some,
        idType = "national_id".some,
        updatedBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = now.toZonedDateTimeUTC.toLocalDateTimeUTC.some)

      val contact = model.Contact(
        id = 1,
        uuid = contactId,
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().some,
        userId = 1.some,
        userUUID = UUID.randomUUID().some,
        contactType = ContactTypes.BusinessOwner,
        name = "John",
        middleName = "Smith".some,
        surname = "Doe",
        phoneNumber = Msisdn("+97188888888"),
        email = Email("j.doe@gmail.com"),
        idType = "national_id",
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some,
        vpUserId = None,
        vpUserUUID = None,
        isActive = true)

      (contactManagement.updateContactInfo _)
        .when(userId, contactId, dto)
        .returns(Future.successful(contact.asRight[ServiceError]))

      val fakeRequest = FakeRequest(PUT, s"/business_users/$userId/contacts/$contactId",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expected =
        s"""
           |{
           |"id":"$contactId",
           |"contact_type":"business_owner",
           |"name":"John",
           |"middle_name":"Smith",
           |"surname":"Doe",
           |"phone_number":"+97188888888",
           |"email":"j.doe@gmail.com",
           |"id_type":"national_id",
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "return list of business_user address" in {
      val userId = UUID.randomUUID()

      val c1 = ContactAddress(
        id = 1,
        uuid = UUID.randomUUID(),
        buApplicationId = 1.some,
        buApplicationUuid = UUID.randomUUID().some,
        userId = 1.some,
        userUuid = UUID.randomUUID().some,
        addressType = AddressTypes.PrimaryAddress,
        countryId = 1,
        countryName = "Kenya",
        city = "Nairobi",
        postalCode = "10100".some,
        address = "kinhasa road nairobi business center".some,
        coordinates = AddressCoordinates(-1.29044, 36.816472).some,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        isActive = true)

      val c2 = ContactAddress(
        id = 2,
        uuid = UUID.randomUUID(),
        buApplicationId = 1.some,
        buApplicationUuid = UUID.randomUUID().some,
        userId = 1.some,
        userUuid = UUID.randomUUID().some,
        addressType = AddressTypes.SecondaryAddress,
        countryId = 1,
        countryName = "UAE",
        city = "Dubai",
        postalCode = "00000".some,
        address = "silicon oasis".some,
        coordinates = None,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        isActive = true)

      (addressManagement.getAddresses _).when(userId, Nil, None, None)
        .returns(Future.successful(Seq(c1, c2).asRight[ServiceError]))

      val fakeRequest = FakeRequest(GET, s"/$endpoint/$userId/addresses").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{"id":"${c1.uuid}",
           |"address_type":"primary_address",
           |"country_name":"Kenya",
           |"city":"Nairobi",
           |"postal_code":"10100",
           |"address":"kinhasa road nairobi business center",
           |"coordinate_x":-1.29044,
           |"coordinate_y":36.816472,
           |"created_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"is_active":true},
           |{
           |"id":"${c2.uuid}",
           |"address_type":"secondary_address",
           |"country_name":"UAE",
           |"city":"Dubai",
           |"postal_code":"00000",
           |"address":"silicon oasis",
           |"coordinate_x":null,
           |"coordinate_y":null,
           |"created_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"is_active":true}],
           |"limit":null,
           |"offset":null}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson
      status(resp) mustBe OK

    }

    "return list of business_user address by id" in {
      val userId = UUID.randomUUID()
      val addressId = UUID.randomUUID()
      val c1 = ContactAddress(
        id = 1,
        uuid = addressId,
        buApplicationId = 1.some,
        buApplicationUuid = UUID.randomUUID().some,
        userId = 1.some,
        userUuid = userId.some,
        addressType = AddressTypes.PrimaryAddress,
        countryId = 1,
        countryName = "Kenya",
        city = "Nairobi",
        postalCode = "10100".some,
        address = "kinhasa road nairobi business center".some,
        coordinates = AddressCoordinates(-1.29044, 36.816472).some,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        isActive = true)

      (addressManagement.getAddressById _).when(userId, addressId)
        .returns(Future.successful(c1.asRight[ServiceError]))

      val fakeRequest = FakeRequest(GET, s"/$endpoint/$userId/addresses/$addressId").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""{"id":"${c1.uuid}",
           |"address_type":"primary_address",
           |"country_name":"Kenya",
           |"city":"Nairobi",
           |"postal_code":"10100",
           |"address":"kinhasa road nairobi business center",
           |"coordinate_x":-1.29044,
           |"coordinate_y":36.816472,
           |"created_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"is_active":true}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson
      status(resp) mustBe OK

    }

    "return success on create address" in {
      val userId = UUID.randomUUID()

      val jsonRequest = """{
                          |  "address_type": "primary_address",
                          |  "country": "Philippines",
                          |  "city": "Makati City",
                          |  "postal_code": "1229",
                          |  "address": "The Residences, Legaspi Village, Makati city, Philippines",
                          |  "coordinate_x": 14.55125,
                          |  "coordinate_y": 121.02009
                          |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = ContactAddressToCreate(
        uuid = mockRequestId,
        userUuid = userId,
        addressType = "primary_address",
        country = "Philippines",
        city = "Makati City",
        postalCode = "1229".some,
        address = "The Residences, Legaspi Village, Makati city, Philippines".some,
        coordinateX = BigDecimal("14.55125").some,
        coordinateY = BigDecimal("121.02009").some,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        isActive = true)

      val address = model.ContactAddress(
        id = 1,
        uuid = mockRequestId,
        buApplicationId = None,
        buApplicationUuid = None,
        userId = 1.some,
        userUuid = UUID.randomUUID().some,
        addressType = AddressTypes.PrimaryAddress,
        countryId = 1,
        countryName = "Philippines",
        city = "Makati City",
        postalCode = "1229".some,
        address = "The Residences, Legaspi Village, Makati city, Philippines".some,
        coordinates = AddressCoordinates(-1.29044, 36.816472).some,
        createdBy = "pegbuser",
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = "pegbuser".some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some,
        isActive = true)

      (addressManagement.insertAddress _)
        .when(dto)
        .returns(Future.successful(address.asRight[ServiceError]))

      val fakeRequest = FakeRequest(POST, s"/business_users/$userId/addresses",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expected =
        s"""
           |{
           |"id":"$mockRequestId",
           |"address_type":"primary_address",
           |"country_name":"Philippines",
           |"city":"Makati City",
           |"postal_code":"1229",
           |"address":"The Residences, Legaspi Village, Makati city, Philippines",
           |"coordinate_x":-1.29044,
           |"coordinate_y":36.816472,
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"is_active":true}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expected
    }

    "return success on update address" in {
      val userId = UUID.randomUUID()
      val addressId = UUID.randomUUID()

      val jsonRequest = s"""{
                           |  "address_type": "primary_address",
                           |  "country": "Philippines",
                           |  "city": "Makati City",
                           |  "postal_code": "1229",
                           |  "address": "The Residences, Legaspi Village, Makati city, Philippines",
                           |  "coordinate_x": 14.55125,
                           |  "coordinate_y": 121.02009,
                           |  "updated_at": "${now.toZonedDateTimeUTC}"
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = ContactAddressToUpdate(
        addressType = "primary_address".some,
        country = "Philippines".some,
        city = "Makati City".some,
        postalCode = "1229".some,
        address = "The Residences, Legaspi Village, Makati city, Philippines".some,
        coordinateX = BigDecimal("14.55125").some,
        coordinateY = BigDecimal("121.02009").some,
        updatedBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = now.toZonedDateTimeUTC.toLocalDateTimeUTC.some)

      val address = model.ContactAddress(
        id = 1,
        uuid = mockRequestId,
        buApplicationId = None,
        buApplicationUuid = None,
        userId = 1.some,
        userUuid = UUID.randomUUID().some,
        addressType = AddressTypes.PrimaryAddress,
        countryId = 1,
        countryName = "Philippines",
        city = "Makati City",
        postalCode = "1229".some,
        address = "The Residences, Legaspi Village, Makati city, Philippines".some,
        coordinates = AddressCoordinates(-1.29044, 36.816472).some,
        createdBy = "pegbuser",
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = "pegbuser".some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some,
        isActive = true)

      (addressManagement.updateAddress _)
        .when(userId, addressId, dto)
        .returns(Future.successful(address.asRight[ServiceError]))

      val fakeRequest = FakeRequest(PUT, s"/business_users/$userId/addresses/$addressId",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expected =
        s"""
           |{
           |"id":"$mockRequestId",
           |"address_type":"primary_address",
           |"country_name":"Philippines",
           |"city":"Makati City",
           |"postal_code":"1229",
           |"address":"The Residences, Legaspi Village, Makati city, Philippines",
           |"coordinate_x":-1.29044,
           |"coordinate_y":36.816472,
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"is_active":true}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "respond 200 OK with external account json in the body from GET /business_users/:id/external_accounts/:external_acc_id" in {
      val userId = UUID.randomUUID()
      val externalAccId = UUID.randomUUID()
      val expectedCriteria = (Option(externalAccId.toUUIDLike), Option(userId.toUUIDLike)).asDomain
      val mockResult = ExternalAccount(
        id = externalAccId,
        customerId = userId,
        externalProvider = "Mashreq",
        externalAccountNumber = "0198320001-101219",
        externalAccountHolder = "Mohamed",
        currency = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0)))
      (externalAccountMgmt.getExternalAccountByCriteria _)
        .when(expectedCriteria, Nil, None, None).returns(Future.successful(Right(Seq(mockResult))))

      val fakeRequest = FakeRequest(GET, s"/business_users/$userId/external_accounts/$externalAccId")
      val resp = route(app, fakeRequest).get
      val mockResultApi = mockResult.asApi
      val expected =
        s"""
           |{
           |"id":"${mockResultApi.id}",
           |"customer_id":"${mockResultApi.customerId}",
           |"provider":"${mockResultApi.provider}",
           |"account_number":"${mockResultApi.accountNumber}",
           |"account_holder":"${mockResultApi.accountHolder}",
           |"currency":"${mockResultApi.currency}",
           |"updated_at":${mockResultApi.updatedAt.get.toJsonStr}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "respond 404 NotFound with customer not found api error json in the body from GET /business_users/:id/external_accounts/:external_acc_id" in {
      val userId = UUID.randomUUID()
      val externalAccId = UUID.randomUUID()
      val expectedCriteria = (Option(externalAccId.toUUIDLike), Option(userId.toUUIDLike)).asDomain

      val mockResult = ServiceError.notFoundError(s"Customer with id [$userId] was not found")
      (externalAccountMgmt.getExternalAccountByCriteria _)
        .when(expectedCriteria, Nil, None, None).returns(Future.successful(Left(mockResult)))

      val fakeRequest = FakeRequest(GET, s"/business_users/$userId/external_accounts/$externalAccId").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      val expected =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"Customer with id [$userId] was not found",
           |"tracking_id":"${mockResult.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expected
    }

    "respond 404 NotFound with external account not found api error json in the body from GET /business_users/:id/external_accounts/:external_acc_id" in {
      val userId = UUID.randomUUID()
      val externalAccId = UUID.randomUUID()
      val expectedCriteria = (Option(externalAccId.toUUIDLike), Option(userId.toUUIDLike)).asDomain

      (externalAccountMgmt.getExternalAccountByCriteria _)
        .when(expectedCriteria, Nil, None, None).returns(Future.successful(Right(Nil)))

      val fakeRequest = FakeRequest(GET, s"/business_users/$userId/external_accounts/$externalAccId").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"External account with id [$externalAccId] was not found under customer with id [$userId]"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expected
    }

    "respond 200 OK with paginated external account json in the body and latestVersion in the headers from GET /business_users/:id/external_accounts" in {
      val userId = UUID.randomUUID()
      val expectedCriteria = (None, Some(userId.toUUIDLike), None, None, None, None, ControllerConstants.externalAccountValidPartialMatch.filterNot(_ == "disabled")).asDomain

      val latestVersion = ExternalAccount(
        id = UUID.randomUUID(),
        customerId = userId,
        externalProvider = "Emirates NBD",
        externalAccountNumber = "102932282-00019273",
        externalAccountHolder = "Abdullah",
        currency = "EUR",
        createdBy = "unit test",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.of(2019, 1, 3, 0, 0)))

      val mockResult = Seq(
        ExternalAccount(
          id = UUID.randomUUID(),
          customerId = userId,
          externalProvider = "Mashreq",
          externalAccountNumber = "0198320001-101219",
          externalAccountHolder = "Mohamed",
          currency = "KES",
          createdBy = "unit test",
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          updatedBy = Some("unit test"),
          updatedAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0))),

        ExternalAccount(
          id = UUID.randomUUID(),
          customerId = userId,
          externalProvider = "Dubai Islamic",
          externalAccountNumber = "01263801-101219",
          externalAccountHolder = "Akbar",
          currency = "USD",
          createdBy = "unit test",
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          updatedBy = Some("unit test"),
          updatedAt = latestVersion.updatedAt),

        ExternalAccount(
          id = UUID.randomUUID(),
          customerId = userId,
          externalProvider = "Emirates NBD",
          externalAccountNumber = "102932282-00019273",
          externalAccountHolder = "Abdullah",
          currency = "EUR",
          createdBy = "unit test",
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          updatedBy = Some("unit test"),
          updatedAt = Some(LocalDateTime.of(2019, 1, 2, 0, 0))))

      (externalAccountMgmt.count _).when(expectedCriteria).returns(Future.successful(Right(mockResult.size)))
      (externalAccountMgmt.getLatestVersion _).when(expectedCriteria).returns(Future.successful(Right(Some(latestVersion))))

      (externalAccountMgmt.getExternalAccountByCriteria _)
        .when(expectedCriteria, Nil, Some(config.PaginationLimit), None).returns(Future.successful(Right(mockResult)))

      val fakeRequest = FakeRequest(GET, s"/business_users/$userId/external_accounts")
      val resp = route(app, fakeRequest).get
      val mockResultApi = mockResult.map(_.asApi)
      val expected =
        s"""
           |{"total":3,
           |"results":[
           |{
           |"id":"${mockResultApi(0).id}",
           |"customer_id":"${mockResultApi(0).customerId}",
           |"provider":"${mockResultApi(0).provider}",
           |"account_number":"${mockResultApi(0).accountNumber}",
           |"account_holder":"${mockResultApi(0).accountHolder}",
           |"currency":"${mockResultApi(0).currency}",
           |"updated_at":${mockResultApi(0).updatedAt.get.toJsonStr}
           |},
           |{
           |"id":"${mockResultApi(1).id}",
           |"customer_id":"${mockResultApi(1).customerId}",
           |"provider":"${mockResultApi(1).provider}",
           |"account_number":"${mockResultApi(1).accountNumber}",
           |"account_holder":"${mockResultApi(1).accountHolder}",
           |"currency":"${mockResultApi(1).currency}",
           |"updated_at":${mockResultApi(1).updatedAt.get.toJsonStr}
           |},
           |{
           |"id":"${mockResultApi(2).id}",
           |"customer_id":"${mockResultApi(2).customerId}",
           |"provider":"${mockResultApi(2).provider}",
           |"account_number":"${mockResultApi(2).accountNumber}",
           |"account_holder":"${mockResultApi(2).accountHolder}",
           |"currency":"${mockResultApi(2).currency}",
           |"updated_at":${mockResultApi(2).updatedAt.get.toJsonStr}
           |}],
           |"limit":${config.PaginationLimit},
           |"offset":null}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
      headers(resp).get("x-version") mustBe latestVersion.updatedAt.map(_.toString)
    }

    "respond 404 NotFound with customer not found api error json in the body from GET /business_users/:id/external_accounts" in {
      val userId = UUID.randomUUID()
      val expectedCriteria = (None, Option(userId.toUUIDLike), None, None, None, None, ControllerConstants.externalAccountValidPartialMatch.filterNot(_ == "disabled")).asDomain

      val mockResult = ServiceError.notFoundError(s"Customer with id [$userId] was not found")

      (externalAccountMgmt.count _).when(expectedCriteria).returns(Future.successful(Left(mockResult)))
      (externalAccountMgmt.getLatestVersion _).when(expectedCriteria).returns(Future.successful(Left(mockResult)))
      (externalAccountMgmt.getExternalAccountByCriteria _)
        .when(expectedCriteria, Nil, None, None).returns(Future.successful(Left(mockResult)))

      val fakeRequest = FakeRequest(GET, s"/business_users/$userId/external_accounts").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      val expected =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"Customer with id [$userId] was not found",
           |"tracking_id":"${mockResult.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expected
    }

    "respond 201 Created with external account json in the body from POST /business_users/:id/external_accounts" in {
      val userId = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"provider":"Bank of the Philippine Islands",
           |"account_number":"019283726256389000013",
           |"account_holder":"Lloyd Edano",
           |"currency":"PHP"}
         """.stripMargin

      val expectedCreateDto = jsonRequest.as(classOf[CustomerExternalAccountToCreate]).
        get.asDomain(mockRequestId, userId, mockRequestFrom, mockRequestDate)

      val mockResult = ExternalAccount(
        id = UUID.randomUUID(),
        customerId = expectedCreateDto.customerId,
        externalProvider = expectedCreateDto.externalProvider,
        externalAccountNumber = expectedCreateDto.externalAccountNumber,
        externalAccountHolder = expectedCreateDto.externalAccountHolder,
        currency = expectedCreateDto.currency,
        createdBy = expectedCreateDto.createdBy,
        createdAt = expectedCreateDto.createdAt,
        updatedBy = None,
        updatedAt = None)

      (externalAccountMgmt.createExternalAccount _).when(expectedCreateDto).returns(Future.successful(Right(mockResult)))

      val fakeRequest = FakeRequest(POST, s"/business_users/$userId/external_accounts", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val mockResultApi = mockResult.asApi

      val expected =
        s"""
           |{"id":"${mockResultApi.id}",
           |"customer_id":"${mockResultApi.customerId}",
           |"provider":"${mockResultApi.provider}",
           |"account_number":"${mockResultApi.accountNumber}",
           |"account_holder":"${mockResultApi.accountHolder}",
           |"currency":"${mockResultApi.currency}",
           |"updated_at":${mockResultApi.updatedAt.toJsonStr}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expected
    }

    "respond 400 BadRequest with malformed request (missing field) error json in the body from POST /business_users/:id/external_accounts" in {
      val userId = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"provider":"Bank of the Philippine Islands",
           |"account_number":"019283726256389000013",
           |"account_owner":"Lloyd Edano",
           |"currency":"PHP"}
         """.stripMargin

      val fakeRequest = FakeRequest(POST, s"/business_users/$userId/external_accounts", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |{"id":"${mockRequestId}",
           |"code":"MalformedRequest",
           |"msg":"Required field [account_holder] was missing in the request."
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expected
    }

    "respond 400 BadRequest with malformed request (not json structure) error json in the body from POST /business_users/:id/external_accounts" in {
      val userId = UUID.randomUUID()

      val jsonRequest =
        s"""
           |"provider":"Bank of the Philippine Islands",
           |"account_number":"019283726256389000013",
           |"account_owner":"Lloyd Edano",
           |"currency":"PHP"
         """.stripMargin

      val fakeRequest = FakeRequest(POST, s"/business_users/$userId/external_accounts", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |{"id":"${mockRequestId}",
           |"code":"MalformedRequest",
           |"msg":"Structure of the request is not a valid json. Please confirm with a json validator."
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expected
    }

    "respond 400 BadRequest with validation api error json in the body from POST /business_users/:id/external_accounts" in {
      val userId = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"provider":"Bank of the Philippine Islands",
           |"account_number":"019283726256389000013",
           |"account_holder":"Lloyd Edano",
           |"currency":"PHP"}
         """.stripMargin

      val expectedCreateDto = jsonRequest.as(classOf[CustomerExternalAccountToCreate]).
        get.asDomain(mockRequestId, userId, mockRequestFrom, mockRequestDate)

      val mockResult = ServiceError.validationError("some domain layer validation error")

      (externalAccountMgmt.createExternalAccount _).when(expectedCreateDto).returns(Future.successful(Left(mockResult)))

      val fakeRequest = FakeRequest(POST, s"/business_users/$userId/external_accounts", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |{"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"${mockResult.message}",
           |"tracking_id":"${mockResult.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expected
    }

    "respond 200 OK with external account json in the body from PUT /business_users/:id/external_accounts/:external_account_id" in {
      val userId = UUID.randomUUID()
      val externalAccId = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"provider":"Metrobank",
           |"updated_at":"2019-04-01T00:00:00Z"}
         """.stripMargin

      val expectedUpdateDto = jsonRequest.as(classOf[ExternalAccountToUpdate], isStrict = false).get
        .asDomain(mockRequestFrom, mockRequestDate)

      val criteria = (Option(externalAccId.toUUIDLike), Option(userId.toUUIDLike)).asDomain

      val mockResult = ExternalAccount(
        id = externalAccId,
        customerId = userId,
        externalProvider = expectedUpdateDto.externalProvider.getOrElse("BDO"),
        externalAccountNumber = expectedUpdateDto.externalAccountNumber.getOrElse("0001-9999"),
        externalAccountHolder = expectedUpdateDto.externalAccountHolder.getOrElse("George Ogalo"),
        currency = expectedUpdateDto.currency.getOrElse("PHP"),
        createdBy = "unit test",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        updatedBy = expectedUpdateDto.updatedBy.some,
        updatedAt = expectedUpdateDto.updatedAt.some)

      (externalAccountMgmt.updateExternalAccount _)
        .when(criteria, expectedUpdateDto).returns(Future.successful(Right(mockResult)))

      val fakeRequest = FakeRequest(PUT, s"/business_users/$userId/external_accounts/$externalAccId", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val mockResultApi = mockResult.asApi
      val expected =
        s"""
           |{
           |"id":"${mockResultApi.id}",
           |"customer_id":"${mockResultApi.customerId}",
           |"provider":"Metrobank",
           |"account_number":"0001-9999",
           |"account_holder":"George Ogalo",
           |"currency":"PHP",
           |"updated_at":${mockResultApi.updatedAt.toJsonStr}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "respond 400 BadRequest with validation api error json in the body from PUT /business_users/:id/external_accounts" in {
      val userId = UUID.randomUUID()
      val externalAccId = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"account_holder":"David Salgado",
           |"updated_at":"2019-04-01T00:00:00Z"}
         """.stripMargin

      val expectedUpdateDto = jsonRequest.as(classOf[ExternalAccountToUpdate], isStrict = false).get
        .asDomain(mockRequestFrom, mockRequestDate)

      val criteria = (Option(externalAccId.toUUIDLike), Option(userId.toUUIDLike)).asDomain

      val mockResult = ServiceError.validationError("some validation error message")

      (externalAccountMgmt.updateExternalAccount _)
        .when(criteria, expectedUpdateDto).returns(Future.successful(Left(mockResult)))

      val fakeRequest = FakeRequest(PUT, s"/business_users/$userId/external_accounts/$externalAccId", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"some validation error message",
           |"tracking_id":"${mockResult.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expected
    }

    "respond 404 Not Found with customer not found api error json in the body from PUT /business_users/:id/external_accounts/:external_acc_id" in {
      val userId = UUID.randomUUID()
      val externalAccId = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"account_holder":"David Salgado",
           |"updated_at":"2019-04-01T00:00:00Z"}
         """.stripMargin

      val expectedUpdateDto = jsonRequest.as(classOf[ExternalAccountToUpdate], isStrict = false).get
        .asDomain(mockRequestFrom, mockRequestDate)

      val criteria = (Option(externalAccId.toUUIDLike), Option(userId.toUUIDLike)).asDomain

      val mockResult = ServiceError.notFoundError("some customer not found error message")

      (externalAccountMgmt.updateExternalAccount _)
        .when(criteria, expectedUpdateDto).returns(Future.successful(Left(mockResult)))

      val fakeRequest = FakeRequest(PUT, s"/business_users/$userId/external_accounts/$externalAccId", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"some customer not found error message",
           |"tracking_id":"${mockResult.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expected
    }

    "respond 200 OK from DELETE /business_users/:id/external_accounts/:external_acc_id" in {
      val userId = UUID.randomUUID()
      val externalAccId = UUID.randomUUID()
      val jsonRequest =
        s"""
           |{"updated_at":"2019-04-01T00:00:00Z"}
         """.stripMargin

      val criteria = (Option(externalAccId.toUUIDLike), Option(userId.toUUIDLike)).asDomain
      val expectedLastUpdatedAt = LocalDateTime.of(2019, 4, 1, 0, 0)
      val mockResult = ()
      (externalAccountMgmt.deleteExternalAccount _)
        .when(criteria, Some(expectedLastUpdatedAt)).returns(Future.successful(Right(mockResult)))

      val fakeRequest = FakeRequest(DELETE, s"/business_users/$userId/external_accounts/$externalAccId", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |{"id":"$externalAccId",
           |"status":"deleted"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "respond 404 Not Found with customer not found api error json in the body from DELETE /business_users/:id/external_accounts/:external_acc_id" in {
      val userId = UUID.randomUUID()
      val externalAccId = UUID.randomUUID()
      val jsonRequest =
        s"""
           |{"updated_at":"2019-04-01T00:00:00Z"}
         """.stripMargin

      val criteria = (Option(externalAccId.toUUIDLike), Option(userId.toUUIDLike)).asDomain
      val expectedLastUpdatedAt = LocalDateTime.of(2019, 4, 1, 0, 0)
      val mockResult = ServiceError.notFoundError("some customer not found error message")

      (externalAccountMgmt.deleteExternalAccount _)
        .when(criteria, Some(expectedLastUpdatedAt)).returns(Future.successful(Left(mockResult)))

      val fakeRequest = FakeRequest(DELETE, s"/business_users/$userId/external_accounts/$externalAccId", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"some customer not found error message",
           |"tracking_id":"${mockResult.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expected
    }

  }

  "respond 200 OK with txn config json in the body from GET /business_users/:id/txn_configs/:txn_config_id" in {
    val userId = UUID.randomUUID()
    val txnConfigId = UUID.randomUUID()
    val expectedCriteria = (txnConfigId, userId).asDomain

    val mockResult = TxnConfig(
      id = txnConfigId,
      customerId = userId,
      transactionType = "cashout",
      currency = "KES",
      createdBy = "unit test",
      createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
      updatedBy = Some("unit test"),
      updatedAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0)))
    (txnConfigMgmt.getTxnConfigByCriteria _)
      .when(expectedCriteria, Nil, None, None).returns(Future.successful(Right(Seq(mockResult))))

    val fakeRequest = FakeRequest(GET, s"/business_users/$userId/txn_configs/$txnConfigId")
    val resp = route(app, fakeRequest).get
    val mockResultApi = mockResult.asApi
    val expected =
      s"""
           |{
           |"id":"${mockResultApi.id}",
           |"customer_id":"${mockResultApi.customerId}",
           |"transaction_type":"${mockResultApi.transactionType}",
           |"currency":"${mockResultApi.currency}",
           |"updated_at":${mockResultApi.updatedAt.get.toJsonStr}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe OK
    contentAsString(resp) mustBe expected
  }

  "respond 404 NotFound with customer not found api error json in the body from GET /business_users/:id/txn_configs/:txn_config_id" in {
    val userId = UUID.randomUUID()
    val txnConfigId = UUID.randomUUID()
    val expectedCriteria = (txnConfigId, userId).asDomain

    val mockResult = ServiceError.notFoundError(s"Customer with id [$userId] was not found")
    (txnConfigMgmt.getTxnConfigByCriteria _)
      .when(expectedCriteria, Nil, None, None).returns(Future.successful(Left(mockResult)))

    val fakeRequest = FakeRequest(GET, s"/business_users/$userId/txn_configs/$txnConfigId").withHeaders(jsonHeaders)
    val resp = route(app, fakeRequest).get

    val expected =
      s"""
         |{
         |"id":"${mockRequestId}",
         |"code":"NotFound",
         |"msg":"Customer with id [$userId] was not found",
         |"tracking_id":"${mockResult.id}"
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe NOT_FOUND
    contentAsString(resp) mustBe expected
  }

  "respond 404 NotFound with txn config not found api error json in the body from GET /business_users/:id/txn_configs/:txn_config_id" in {
    val userId = UUID.randomUUID()
    val txnConfigId = UUID.randomUUID()
    val expectedCriteria = (txnConfigId, userId).asDomain

    (txnConfigMgmt.getTxnConfigByCriteria _)
      .when(expectedCriteria, Nil, None, None).returns(Future.successful(Right(Nil)))

    val fakeRequest = FakeRequest(GET, s"/business_users/$userId/txn_configs/$txnConfigId").withHeaders(jsonHeaders)
    val resp = route(app, fakeRequest).get
    val expected =
      s"""
         |{
         |"id":"${mockRequestId}",
         |"code":"NotFound",
         |"msg":"TxnConfig with id [$txnConfigId] was not found under customer with id [$userId]"
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe NOT_FOUND
    contentAsString(resp) mustBe expected
  }

  "respond 200 OK with paginated external account json in the body and latestVersion in the headers from GET /business_users/:id/txn_configs" in {
    val userId = UUID.randomUUID()
    val expectedCriteria = (None, Some(userId.toUUIDLike), None, None, None, ControllerConstants.txnConfigValidPartialMatch.filterNot(_ == "disabled")).asDomain

    val latestVersion = TxnConfig(
      id = UUID.randomUUID(),
      customerId = userId,
      transactionType = "cashout",
      currency = "EUR",
      createdBy = "unit test",
      createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
      updatedBy = Some("unit test"),
      updatedAt = Some(LocalDateTime.of(2019, 1, 3, 0, 0)))

    val mockResult = Seq(
      TxnConfig(
        id = UUID.randomUUID(),
        customerId = userId,
        transactionType = "cashin",
        currency = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0))),

      TxnConfig(
        id = UUID.randomUUID(),
        customerId = userId,
        transactionType = "international_remittance",
        currency = "USD",
        createdBy = "unit test",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        updatedBy = Some("unit test"),
        updatedAt = latestVersion.updatedAt),

      TxnConfig(
        id = UUID.randomUUID(),
        customerId = userId,
        transactionType = "p2p",
        currency = "EUR",
        createdBy = "unit test",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.of(2019, 1, 2, 0, 0))))

    (txnConfigMgmt.count _).when(expectedCriteria).returns(Future.successful(Right(mockResult.size)))
    (txnConfigMgmt.getLatestVersion _).when(expectedCriteria).returns(Future.successful(Right(Some(latestVersion))))

    (txnConfigMgmt.getTxnConfigByCriteria _)
      .when(expectedCriteria, Nil, Some(config.PaginationLimit), None).returns(Future.successful(Right(mockResult)))

    val fakeRequest = FakeRequest(GET, s"/business_users/$userId/txn_configs")
    val resp = route(app, fakeRequest).get
    val mockResultApi = mockResult.map(_.asApi)
    val expected =
      s"""
         |{"total":3,
         |"results":[
         |{
         |"id":"${mockResultApi(0).id}",
         |"customer_id":"${mockResultApi(0).customerId}",
         |"transaction_type":"${mockResultApi(0).transactionType}",
         |"currency":"${mockResultApi(0).currency}",
         |"updated_at":${mockResultApi(0).updatedAt.get.toJsonStr}
         |},
         |{
         |"id":"${mockResultApi(1).id}",
         |"customer_id":"${mockResultApi(1).customerId}",
         |"transaction_type":"${mockResultApi(1).transactionType}",
         |"currency":"${mockResultApi(1).currency}",
         |"updated_at":${mockResultApi(1).updatedAt.get.toJsonStr}
         |},
         |{
         |"id":"${mockResultApi(2).id}",
         |"customer_id":"${mockResultApi(2).customerId}",
         |"transaction_type":"${mockResultApi(2).transactionType}",
         |"currency":"${mockResultApi(2).currency}",
         |"updated_at":${mockResultApi(2).updatedAt.get.toJsonStr}
         |}],
         |"limit":${config.PaginationLimit},
         |"offset":null}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe OK
    contentAsString(resp) mustBe expected
    headers(resp).get("x-version") mustBe latestVersion.updatedAt.map(_.toString)
  }

  "respond 404 NotFound with customer not found api error json in the body from GET /business_users/:id/txn_configs" in {
    val userId = UUID.randomUUID()
    val expectedCriteria = (None, Some(userId.toUUIDLike), None, None, None, ControllerConstants.txnConfigValidPartialMatch.filterNot(_ == "disabled")).asDomain

    val mockResult = ServiceError.notFoundError(s"Customer with id [$userId] was not found")

    (txnConfigMgmt.count _).when(expectedCriteria).returns(Future.successful(Left(mockResult)))
    (txnConfigMgmt.getLatestVersion _).when(expectedCriteria).returns(Future.successful(Left(mockResult)))
    (txnConfigMgmt.getTxnConfigByCriteria _)
      .when(expectedCriteria, Nil, None, None).returns(Future.successful(Left(mockResult)))

    val fakeRequest = FakeRequest(GET, s"/business_users/$userId/txn_configs").withHeaders(jsonHeaders)
    val resp = route(app, fakeRequest).get

    val expected =
      s"""
         |{
         |"id":"${mockRequestId}",
         |"code":"NotFound",
         |"msg":"Customer with id [$userId] was not found",
         |"tracking_id":"${mockResult.id}"
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe NOT_FOUND
    contentAsString(resp) mustBe expected
  }

  "respond 201 Created with external account json in the body from POST /business_users/:id/txn_configs" in {
    val userId = UUID.randomUUID()

    val jsonRequest =
      s"""
         |{"transaction_type":"merchant_payment",
         |"currency":"PHP"}
       """.stripMargin

    val expectedCreateDto = jsonRequest.as(classOf[CustomerTxnConfigToCreate]).
      get.asDomain(mockRequestId, userId, mockRequestFrom, mockRequestDate)

    val mockResult = TxnConfig(
      id = UUID.randomUUID(),
      customerId = expectedCreateDto.customerId,
      transactionType = expectedCreateDto.transactionType,
      currency = expectedCreateDto.currency,
      createdBy = expectedCreateDto.createdBy,
      createdAt = expectedCreateDto.createdAt,
      updatedBy = None,
      updatedAt = None)

    (txnConfigMgmt.createTxnConfig _).when(expectedCreateDto).returns(Future.successful(Right(mockResult)))

    val fakeRequest = FakeRequest(POST, s"/business_users/$userId/txn_configs", jsonHeaders, jsonRequest)
    val resp = route(app, fakeRequest).get
    val mockResultApi = mockResult.asApi

    val expected =
      s"""
         |{"id":"${mockResultApi.id}",
         |"customer_id":"${mockResultApi.customerId}",
         |"transaction_type":"${mockResultApi.transactionType}",
         |"currency":"${mockResultApi.currency}",
         |"updated_at":${mockResultApi.updatedAt.toJsonStr}
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe CREATED
    contentAsString(resp) mustBe expected
  }

  "respond 400 BadRequest with malformed request (missing field) error json in the body from POST /business_users/:id/txn_configs" in {
    val userId = UUID.randomUUID()

    val jsonRequest =
      s"""
         |{"currency":"PHP"}
       """.stripMargin

    val fakeRequest = FakeRequest(POST, s"/business_users/$userId/txn_configs", jsonHeaders, jsonRequest)
    val resp = route(app, fakeRequest).get
    val expected =
      s"""
         |{"id":"${mockRequestId}",
         |"code":"MalformedRequest",
         |"msg":"Required field [transaction_type] was missing in the request."
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe BAD_REQUEST
    contentAsString(resp) mustBe expected
  }

  "respond 400 BadRequest with malformed request (not json structure) error json in the body from POST /business_users/:id/txn_configs" in {
    val userId = UUID.randomUUID()

    val jsonRequest =
      s"""
         |"transaction_type":"cashout",
         |"currency":"PHP"
       """.stripMargin

    val fakeRequest = FakeRequest(POST, s"/business_users/$userId/txn_configs", jsonHeaders, jsonRequest)
    val resp = route(app, fakeRequest).get
    val expected =
      s"""
         |{"id":"${mockRequestId}",
         |"code":"MalformedRequest",
         |"msg":"Structure of the request is not a valid json. Please confirm with a json validator."
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe BAD_REQUEST
    contentAsString(resp) mustBe expected
  }

  "respond 400 BadRequest with validation api error json in the body from POST /business_users/:id/txn_configs" in {
    val userId = UUID.randomUUID()

    val jsonRequest =
      s"""
         |{"transaction_type":"cashout",
         |"currency":"PHP"}
       """.stripMargin

    val expectedCreateDto = jsonRequest.as(classOf[CustomerTxnConfigToCreate]).
      get.asDomain(mockRequestId, userId, mockRequestFrom, mockRequestDate)

    val mockResult = ServiceError.validationError("some domain layer validation error")

    (txnConfigMgmt.createTxnConfig _).when(expectedCreateDto).returns(Future.successful(Left(mockResult)))

    val fakeRequest = FakeRequest(POST, s"/business_users/$userId/txn_configs", jsonHeaders, jsonRequest)
    val resp = route(app, fakeRequest).get
    val expected =
      s"""
         |{"id":"${mockRequestId}",
         |"code":"InvalidRequest",
         |"msg":"${mockResult.message}",
         |"tracking_id":"${mockResult.id}"
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe BAD_REQUEST
    contentAsString(resp) mustBe expected
  }

  "respond 200 OK with external account json in the body from PUT /business_users/:id/external_accounts/:txn_configs" in {
    val userId = UUID.randomUUID()
    val txnConfId = UUID.randomUUID()

    val jsonRequest =
      s"""
         |{"transaction_type":"p2p",
         |"currency":"INR",
         |"updated_at":"2019-04-01T00:00:00Z"}
       """.stripMargin

    val expectedUpdateDto = jsonRequest.as(classOf[TxnConfigToUpdate], isStrict = false).get
      .asDomain(mockRequestFrom, mockRequestDate)

    val criteria = (txnConfId, userId).asDomain

    val mockResult = TxnConfig(
      id = txnConfId,
      customerId = userId,
      transactionType = expectedUpdateDto.transactionType.getOrElse("Not Available"),
      currency = expectedUpdateDto.currency.getOrElse("Not Available"),
      createdBy = "unit test",
      createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
      updatedBy = expectedUpdateDto.updatedBy.some,
      updatedAt = expectedUpdateDto.updatedAt.some)

    (txnConfigMgmt.updateTxnConfig _)
      .when(criteria, expectedUpdateDto).returns(Future.successful(Right(mockResult)))

    val fakeRequest = FakeRequest(PUT, s"/business_users/$userId/txn_configs/$txnConfId", jsonHeaders, jsonRequest)
    val resp = route(app, fakeRequest).get
    val mockResultApi = mockResult.asApi
    val expected =
      s"""
         |{
         |"id":"${mockResultApi.id}",
         |"customer_id":"${mockResultApi.customerId}",
         |"transaction_type":"p2p",
         |"currency":"INR",
         |"updated_at":${mockResultApi.updatedAt.toJsonStr}
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe OK
    contentAsString(resp) mustBe expected
  }

  "respond 400 BadRequest with validation api error json in the body from PUT /business_users/:id/txn_configs" in {
    val userId = UUID.randomUUID()
    val txnConfId = UUID.randomUUID()

    val jsonRequest =
      s"""
         |{"currency":"AED",
         |"updated_at":"2019-04-01T00:00:00Z"}
       """.stripMargin

    val expectedUpdateDto = jsonRequest.as(classOf[TxnConfigToUpdate], isStrict = false).get
      .asDomain(mockRequestFrom, mockRequestDate)

    val criteria = (txnConfId, userId).asDomain

    val mockResult = ServiceError.validationError("some validation error message")

    (txnConfigMgmt.updateTxnConfig _)
      .when(criteria, expectedUpdateDto).returns(Future.successful(Left(mockResult)))

    val fakeRequest = FakeRequest(PUT, s"/business_users/$userId/txn_configs/$txnConfId", jsonHeaders, jsonRequest)
    val resp = route(app, fakeRequest).get
    val expected =
      s"""
         |{
         |"id":"${mockRequestId}",
         |"code":"InvalidRequest",
         |"msg":"some validation error message",
         |"tracking_id":"${mockResult.id}"
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe BAD_REQUEST
    contentAsString(resp) mustBe expected
  }

  "respond 404 Not Found with customer not found api error json in the body from PUT /business_users/:id/txn_configs/:txn_config_id" in {
    val userId = UUID.randomUUID()
    val txnConfId = UUID.randomUUID()

    val jsonRequest =
      s"""
         |{"currency":"AED",
         |"updated_at":"2019-04-01T00:00:00Z"}
       """.stripMargin

    val expectedUpdateDto = jsonRequest.as(classOf[TxnConfigToUpdate], isStrict = false).get
      .asDomain(mockRequestFrom, mockRequestDate)

    val criteria = (txnConfId, userId).asDomain

    val mockResult = ServiceError.notFoundError("some customer not found error message")

    (txnConfigMgmt.updateTxnConfig _)
      .when(criteria, expectedUpdateDto).returns(Future.successful(Left(mockResult)))

    val fakeRequest = FakeRequest(PUT, s"/business_users/$userId/txn_configs/$txnConfId", jsonHeaders, jsonRequest)
    val resp = route(app, fakeRequest).get
    val expected =
      s"""
         |{
         |"id":"${mockRequestId}",
         |"code":"NotFound",
         |"msg":"some customer not found error message",
         |"tracking_id":"${mockResult.id}"
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe NOT_FOUND
    contentAsString(resp) mustBe expected
  }

  "respond 200 OK from DELETE /business_users/:id/txn_configs/:txn_config_id" in {
    val userId = UUID.randomUUID()
    val txnConfId = UUID.randomUUID()
    val jsonRequest =
      s"""
         |{"updated_at":"2019-04-01T00:00:00Z"}
       """.stripMargin

    val criteria = (txnConfId, userId).asDomain
    val expectedLastUpdatedAt = LocalDateTime.of(2019, 4, 1, 0, 0)
    val mockResult = ()
    (txnConfigMgmt.deleteTxnConfig _)
      .when(criteria, Some(expectedLastUpdatedAt)).returns(Future.successful(Right(mockResult)))

    val fakeRequest = FakeRequest(DELETE, s"/business_users/$userId/txn_configs/$txnConfId", jsonHeaders, jsonRequest)
    val resp = route(app, fakeRequest).get
    val expected =
      s"""
         |{"id":"$txnConfId",
         |"status":"deleted"}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe OK
    contentAsString(resp) mustBe expected
  }

  "respond 404 Not Found with customer not found api error json in the body from DELETE /business_users/:id/txn_configs/:txn_config_id" in {
    val userId = UUID.randomUUID()
    val txnConfId = UUID.randomUUID()
    val jsonRequest =
      s"""
         |{"updated_at":"2019-04-01T00:00:00Z"}
       """.stripMargin

    val criteria = (txnConfId, userId).asDomain
    val expectedLastUpdatedAt = LocalDateTime.of(2019, 4, 1, 0, 0)
    val mockResult = ServiceError.notFoundError("some customer not found error message")

    (txnConfigMgmt.deleteTxnConfig _)
      .when(criteria, Some(expectedLastUpdatedAt)).returns(Future.successful(Left(mockResult)))

    val fakeRequest = FakeRequest(DELETE, s"/business_users/$userId/txn_configs/$txnConfId", jsonHeaders, jsonRequest)
    val resp = route(app, fakeRequest).get
    val expected =
      s"""
         |{
         |"id":"${mockRequestId}",
         |"code":"NotFound",
         |"msg":"some customer not found error message",
         |"tracking_id":"${mockResult.id}"
         |}
       """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe NOT_FOUND
    contentAsString(resp) mustBe expected
  }

}
