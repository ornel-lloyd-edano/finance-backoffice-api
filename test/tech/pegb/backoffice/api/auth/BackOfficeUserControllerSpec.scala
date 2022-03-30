package tech.pegb.backoffice.api.auth

import java.time.LocalDateTime
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.auth.controllers.impl.BackOfficeUserController
import tech.pegb.backoffice.api.auth.dto.BackOfficeUserToCreate
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.auth.abstraction.BackOfficeUserService
import tech.pegb.backoffice.domain.auth.dto.{BackOfficeUserCriteria, BackOfficeUserToRemove, BackOfficeUserToUpdate, BackOfficeUserToCreate ⇒ DomBackOfficeUserToCreate}
import tech.pegb.backoffice.domain.auth.model.{BackOfficeUser, BusinessUnit, Email, Role}
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.auth.backofficeuser.Implicits._
import tech.pegb.backoffice.util.Constants._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class BackOfficeUserControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  private val backOfficeUserService = stub[BackOfficeUserService]
  val latestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[BackOfficeUserService].to(backOfficeUserService),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "BackOfficeUserController" should {
    val mockId = UUID.randomUUID()

    "create a backoffice user in POST /back_office_users" in {
      val mockRoleId = UUID.randomUUID()
      val mockBUId = UUID.randomUUID()

      val mockInput = BackOfficeUserToCreate(
        userName = "Narek123",
        email = "narek@pegb.tech",
        phoneNumber = Some("+971544451679"),
        firstName = "Ornel Lloyd",
        middleName = Some("Pepito"),
        lastName = "Edano",
        description = None,
        homePage = None,
        activeLanguage = None,
        customData = None,
        roleId = mockRoleId.toString,
        businessUnitId = mockBUId.toString).asDomain(mockRequestFrom, mockRequestDate)

      val mockResult = BackOfficeUser(
        id = UUID.randomUUID(),
        userName = mockInput.userName,
        hashedPassword = Some("secret password"),
        role = Role(
          id = mockRoleId,
          name = "role1",
          level = 1,
          createdBy = "admin",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        businessUnit = BusinessUnit(
          id = mockBUId,
          name = "bu1",
          createdBy = "admin",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        permissions = Nil,
        email = mockInput.email,
        phoneNumber = mockInput.phoneNumber,
        firstName = mockInput.firstName,
        middleName = None,
        lastName = mockInput.lastName,
        description = None,
        homePage = None,
        activeLanguage = None,
        customData = None,
        lastLoginTimestamp = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = None,
        updatedAt = None)

      (backOfficeUserService.createBackOfficeUser(_: DomBackOfficeUserToCreate, _: Boolean))
        .when(mockInput, false)
        .returns(Right(mockResult).toFuture)

      val jsonRequest =
        s"""
           |{
           |"user_name":"Narek123",
           |"email":"narek@pegb.tech",
           |"phone_number":"+971544451679",
           |"first_name":"Ornel Lloyd",
           |"middle_name":"Pepito",
           |"last_name":"Edano",
           |"role_id":"${mockRoleId}",
           |"business_unit_id":"${mockBUId}"
           |}
         """.stripMargin
      val resp = route(app, FakeRequest(POST, s"/back_office_users",
        jsonHeaders.add(strictDeserializationKey → "false"),
        jsonRequest)).get

      val expectedJson =
        s"""
           |{"id":"${mockResult.id}",
           |"user_name":"Narek123",
           |"email":"narek@pegb.tech",
           |"phone_number":"+971544451679",
           |"first_name":"Ornel Lloyd",
           |"middle_name":null,
           |"last_name":"Edano",
           |"description":null,
           |"home_page":null,
           |"active_language":null,
           |"last_login_timestamp":null,
           |"custom_data":null,
           |"role":{
           |"id":${mockResult.role.id.toJsonStr},
           |"name":${mockResult.role.name.toJsonStr},
           |"level":${mockResult.role.level.toJsonStr},
           |"created_by":${mockResult.role.createdBy.toJsonStr},
           |"updated_by":${mockResult.role.updatedBy.toJsonStr},
           |"created_at":${mockResult.role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${mockResult.role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null
           |},
           |"business_unit":{
           |"id":${mockResult.businessUnit.id.toJsonStr},
           |"name":${mockResult.businessUnit.name.toJsonStr},
           |"created_by":${mockResult.businessUnit.createdBy.toJsonStr},
           |"updated_by":${mockResult.businessUnit.updatedBy.toJsonStr},
           |"created_at":${mockResult.businessUnit.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${mockResult.businessUnit.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null
           |},
           |"permissions":[],
           |"created_by":"${mockInput.createdBy}",
           |"updated_by":null,
           |"created_at":${mockInput.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${mockInput.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "fail to create a backoffice user in POST /back_office_users if some optional fields are not provided and strict deserialization is true" in {
      val jsonRequest =
        s"""
           |{
           |"user_name":"Narek123",
           |"email":"narek@pegb.tech",
           |"phone_number":"+971544451679",
           |"first_name":"Ornel Lloyd",
           |"middle_name":"Pepito",
           |"last_name":"Edano",
           |"role_id":"${UUID.randomUUID()}",
           |"business_unit_id":"${UUID.randomUUID()}"
           |}
         """.stripMargin
      val resp = route(app, FakeRequest(POST, s"/back_office_users",
        jsonHeaders.add(strictDeserializationKey → "true"),
        jsonRequest)).get

      status(resp) mustBe BAD_REQUEST
    }

    "fail to create a backoffice user if request is malformed" in {
      val jsonRequest =
        s"""
           |{
           |"user_name":"Narek123",
           |"email":"narek@pegb.tech",
           |"phone_number":"+971544451679",
           |"first_name":"Ornel Lloyd",
           |"middle_name":"Pepito",
           |"last_name":"Edano",
           |"role_id":"123456",
           |"business_unit_id":"${UUID.randomUUID()}"
           |}
         """.stripMargin
      val resp = route(app, FakeRequest(POST, s"/back_office_users",
        jsonHeaders.add(strictDeserializationKey → "true"),
        jsonRequest)).get

      status(resp) mustBe BAD_REQUEST
    }

    "get a backoffice user by id in GET /back_office_users/:id" in {
      val mockResult = BackOfficeUser(
        id = UUID.randomUUID(),
        userName = "Lloyd",
        hashedPassword = Some("secret password"),
        role = Role(
          id = UUID.randomUUID(),
          name = "role1",
          level = 1,
          createdBy = "admin",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        businessUnit = BusinessUnit(
          id = UUID.randomUUID(),
          name = "bu1",
          createdBy = "admin",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        permissions = Nil,
        email = Email("lloyd@pegb.tech"),
        phoneNumber = Some("+971544451679"),
        firstName = "Ornel Lloyd",
        middleName = None,
        lastName = "Edano",
        description = None,
        homePage = None,
        activeLanguage = None,
        customData = None,
        lastLoginTimestamp = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = None,
        updatedAt = None)

      (backOfficeUserService.getActiveBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(id = UUIDLike(mockId.toString).toOption).toOption, Nil, None, None)
        .returns(Right(Seq(mockResult)).toFuture)

      val resp = route(app, FakeRequest(GET, s"/back_office_users/$mockId")).get
      val expectedJson =
        s"""
           |{"id":"${mockResult.id}",
           |"user_name":"Lloyd",
           |"email":"lloyd@pegb.tech",
           |"phone_number":"+971544451679",
           |"first_name":"Ornel Lloyd",
           |"middle_name":null,
           |"last_name":"Edano",
           |"description":null,
           |"home_page":null,
           |"active_language":null,
           |"last_login_timestamp":null,
           |"custom_data":null,
           |"role":{
            |"id":${mockResult.role.id.toJsonStr},
            |"name":${mockResult.role.name.toJsonStr},
            |"level":${mockResult.role.level.toJsonStr},
            |"created_by":${mockResult.role.createdBy.toJsonStr},
            |"updated_by":${mockResult.role.updatedBy.toJsonStr},
            |"created_at":${mockResult.role.createdAt.toZonedDateTimeUTC.toJsonStr},
            |"updated_at":null,
            |"created_time":${mockResult.role.createdAt.toZonedDateTimeUTC.toJsonStr},
            |"updated_time":null
            |},
            |"business_unit":{
            |"id":${mockResult.businessUnit.id.toJsonStr},
            |"name":${mockResult.businessUnit.name.toJsonStr},
            |"created_by":${mockResult.businessUnit.createdBy.toJsonStr},
            |"updated_by":${mockResult.businessUnit.updatedBy.toJsonStr},
            |"created_at":${mockResult.businessUnit.createdAt.toZonedDateTimeUTC.toJsonStr},
            |"updated_at":null,
            |"created_time":${mockResult.businessUnit.createdAt.toZonedDateTimeUTC.toJsonStr},
            |"updated_time":null
            |},
            |"permissions":[],
           |"created_by":"${mockResult.createdBy}",
           |"updated_by":null,
           |"created_at":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "get all backoffice users in GET /back_office_users" in {
      val mockResult = BackOfficeUser(
        id = UUID.randomUUID(),
        userName = "Lloyd",
        hashedPassword = Some("secret password"),
        role = Role(
          id = UUID.randomUUID(),
          name = "role1",
          level = 1,
          createdBy = "admin",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        businessUnit = BusinessUnit(
          id = UUID.randomUUID(),
          name = "bu1",
          createdBy = "admin",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        permissions = Nil,
        email = Email("lloyd@pegb.tech"),
        phoneNumber = Some("+971544451679"),
        firstName = "Ornel Lloyd",
        middleName = None,
        lastName = "Edano",
        description = None,
        homePage = None,
        activeLanguage = None,
        customData = None,
        lastLoginTimestamp = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = None,
        updatedAt = None)

      val mockCriteria = BackOfficeUserCriteria(partialMatchFields = BackOfficeUserController.validPartialMatchFields.filterNot(_ == "disabled"))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(*).returns(Right(mockLatestVersion.toOption).toFuture)

      (backOfficeUserService.countActiveBackOfficeUsersByCriteria _)
        .when(mockCriteria.toOption)
        .returns(Right(10).toFuture)

      (backOfficeUserService.getActiveBackOfficeUsersByCriteria _)
        .when(mockCriteria.toOption, Nil, Some(1), Some(0))
        .returns(Right(Seq(mockResult)).toFuture)

      val resp = route(app, FakeRequest(GET, s"/back_office_users?limit=1&offset=0")).get
      val expectedJson =
        s"""{
           |"total":10,
           |"results":[
           |{"id":"${mockResult.id}",
           |"user_name":"Lloyd",
           |"email":"lloyd@pegb.tech",
           |"phone_number":"+971544451679",
           |"first_name":"Ornel Lloyd",
           |"middle_name":null,
           |"last_name":"Edano",
           |"description":null,
           |"home_page":null,
           |"active_language":null,
           |"last_login_timestamp":null,
           |"custom_data":null,
           |"role":{
          |"id":${mockResult.role.id.toJsonStr},
          |"name":${mockResult.role.name.toJsonStr},
          |"level":${mockResult.role.level.toJsonStr},
          |"created_by":${mockResult.role.createdBy.toJsonStr},
          |"updated_by":${mockResult.role.updatedBy.toJsonStr},
          |"created_at":${mockResult.role.createdAt.toZonedDateTimeUTC.toJsonStr},
          |"updated_at":null,
          |"created_time":${mockResult.role.createdAt.toZonedDateTimeUTC.toJsonStr},
          |"updated_time":null
          |},
          |"business_unit":{
          |"id":${mockResult.businessUnit.id.toJsonStr},
          |"name":${mockResult.businessUnit.name.toJsonStr},
          |"created_by":${mockResult.businessUnit.createdBy.toJsonStr},
          |"updated_by":${mockResult.businessUnit.updatedBy.toJsonStr},
          |"created_at":${mockResult.businessUnit.createdAt.toZonedDateTimeUTC.toJsonStr},
          |"updated_at":null,
          |"created_time":${mockResult.businessUnit.createdAt.toZonedDateTimeUTC.toJsonStr},
          |"updated_time":null
          |},
          |"permissions":[],
           |"created_by":"${mockResult.createdBy}",
           |"updated_by":null,
           |"created_at":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null}
           |],
           |"limit":1,"offset":0}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "get X-Version with empty results in HEAD /back_office_users" in {

      val criteria = BackOfficeUserCriteria(partialMatchFields = BackOfficeUserController.validPartialMatchFields.filterNot(_ == "disabled"))
      val mockLatestVersion = LocalDateTime.now.toString

      (latestVersionService.getLatestVersion _).when(criteria).returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(HEAD, s"/back_office_users")).get
      val expectedJson =
        s"""
           |{"total":0,
           |"results":[],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "update backoffice user by id in PUT /back_office_users/:id " in {
      val mockDto = BackOfficeUserToUpdate(
        email = Email("new_email@pegb.tech").toOption,
        phoneNumber = Some("+971544456781"),
        updatedBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC)

      val mockResult = BackOfficeUser(
        id = UUID.randomUUID(),
        userName = "Lloyd",
        hashedPassword = Some("secret password"),
        role = Role(
          id = UUID.randomUUID(),
          name = "role1",
          level = 1,
          createdBy = "admin",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        businessUnit = BusinessUnit(
          id = UUID.randomUUID(),
          name = "bu1",
          createdBy = "admin",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        permissions = Nil,
        email = mockDto.email.get,
        phoneNumber = mockDto.phoneNumber,
        firstName = "Ornel Lloyd",
        middleName = None,
        lastName = "Edano",
        description = None,
        homePage = None,
        activeLanguage = None,
        customData = None,
        lastLoginTimestamp = None,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockDto.updatedBy.toOption,
        updatedAt = mockDto.updatedAt.toOption)

      (backOfficeUserService.updateBackOfficeUser(_: UUID, _: BackOfficeUserToUpdate))
        .when(mockId, mockDto).returns(Right(mockResult).toFuture)

      val jsonRequest =
        s"""
           |{
           |"email":"new_email@pegb.tech",
           |"phone_number":"+971544456781"
           |}
         """.stripMargin

      val resp = route(app, FakeRequest(PUT, s"/back_office_users/$mockId",
        jsonHeaders.add(strictDeserializationKey → "false"),
        jsonRequest)).get

      val expectedJson =
        s"""
           |{"id":"${mockResult.id}",
           |"user_name":"Lloyd",
           |"email":"${mockDto.email.get.value}",
           |"phone_number":"${mockDto.phoneNumber.get}",
           |"first_name":"Ornel Lloyd",
           |"middle_name":null,
           |"last_name":"Edano",
           |"description":null,
           |"home_page":null,
           |"active_language":null,
           |"last_login_timestamp":null,
           |"custom_data":null,
            |"role":{
            |"id":${mockResult.role.id.toJsonStr},
            |"name":${mockResult.role.name.toJsonStr},
            |"level":${mockResult.role.level.toJsonStr},
            |"created_by":${mockResult.role.createdBy.toJsonStr},
            |"updated_by":${mockResult.role.updatedBy.toJsonStr},
            |"created_at":${mockResult.role.createdAt.toZonedDateTimeUTC.toJsonStr},
            |"updated_at":null,
            |"created_time":${mockResult.role.createdAt.toZonedDateTimeUTC.toJsonStr},
            |"updated_time":null
            |},
            |"business_unit":{
            |"id":${mockResult.businessUnit.id.toJsonStr},
            |"name":${mockResult.businessUnit.name.toJsonStr},
            |"created_by":${mockResult.businessUnit.createdBy.toJsonStr},
            |"updated_by":${mockResult.businessUnit.updatedBy.toJsonStr},
            |"created_at":${mockResult.businessUnit.createdAt.toZonedDateTimeUTC.toJsonStr},
            |"updated_at":null,
            |"created_time":${mockResult.businessUnit.createdAt.toZonedDateTimeUTC.toJsonStr},
            |"updated_time":null
            |},
            |"permissions":[],
           |"created_by":"${mockResult.createdBy}",
           |"updated_by":"${mockResult.updatedBy.get}",
           |"created_at":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockResult.updatedAt.get.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockResult.updatedAt.get.toZonedDateTimeUTC.toJsonStr}}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "fail to update backoffice user by id if optional request fields are not included and strict-deserialization=true" in {
      val jsonRequest =
        s"""
           |{
           |"email":"new_email@pegb.tech",
           |"phone_number":"+971544456781"
           |}
         """.stripMargin

      val resp = route(app, FakeRequest(PUT, s"/back_office_users/$mockId",
        jsonHeaders.add(strictDeserializationKey → "true"),
        jsonRequest)).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"MalformedRequest",
           |"msg":"Malformed request to update back_office_user. A mandatory field might be missing or its value is of wrong type."}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail to update backoffice user by id if request is malformed" in {
      val jsonRequest =
        s"""
           |{
           |"email":"+971544456781",
           |"phone_number":"+971544456781"
           |}
         """.stripMargin

      val resp = route(app, FakeRequest(PUT, s"/back_office_users/$mockId",
        jsonHeaders.add(strictDeserializationKey → "false"),
        jsonRequest)).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to back_office_user. A field might be in the wrong format or not among the expected values."}
         """.stripMargin.trim.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "delete a backoffice user by id in DELETE /back_office_users/:id" in {
      val jsonRequest =
        s"""
           |{
           |"updated_at":null
           |}
         """.stripMargin
      val mockInput = BackOfficeUserToRemove(
        removedBy = mockRequestFrom,
        removedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = None)
      (backOfficeUserService.removeBackOfficeUser(_: UUID, _: BackOfficeUserToRemove))
        .when(mockId, mockInput).returns(Right(UnitInstance).toFuture)

      val resp = route(app, FakeRequest(DELETE, s"/back_office_users/$mockId", jsonHeaders, jsonRequest)).get

      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      status(resp) mustBe OK
      contentAsString(resp) mustBe mockId.toJsonStr
      //status(resp) mustBe NO_CONTENT
    }

    "still delete a backoffice user by id even without json payload for updated_at (for backwards compatibility)" in {
      val jsonRequest = ""
      val mockInput = BackOfficeUserToRemove(
        removedBy = mockRequestFrom,
        removedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = None)
      (backOfficeUserService.removeBackOfficeUser(_: UUID, _: BackOfficeUserToRemove))
        .when(mockId, mockInput).returns(Right(UnitInstance).toFuture)

      val resp = route(app, FakeRequest(DELETE, s"/back_office_users/$mockId", jsonHeaders, jsonRequest)).get

      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      status(resp) mustBe OK
      contentAsString(resp) mustBe mockId.toJsonStr
      //status(resp) mustBe NO_CONTENT
    }

    "fail to delete a backoffice user by id without the updated_at request if strict-deserialization=true" in {
      val jsonRequest =
        s"""
           |{
           |"some_other_field":1234
           |}
         """.stripMargin
      val mockInput = BackOfficeUserToRemove(
        removedBy = mockRequestFrom,
        removedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = None)
      (backOfficeUserService.removeBackOfficeUser(_: UUID, _: BackOfficeUserToRemove))
        .when(mockId, mockInput).returns(Right(UnitInstance).toFuture)

      val resp = route(app, FakeRequest(DELETE, s"/back_office_users/$mockId",
        jsonHeaders.add(strictDeserializationKey → "true"), jsonRequest)).get

      status(resp) mustBe BAD_REQUEST
    }
  }

}
