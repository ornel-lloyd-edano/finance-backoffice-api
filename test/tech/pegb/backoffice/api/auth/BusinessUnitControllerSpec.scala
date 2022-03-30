package tech.pegb.backoffice.api.auth

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.dto.{BusinessUnitCriteria, BusinessUnitToCreate, BusinessUnitToRemove, BusinessUnitToUpdate}
import tech.pegb.backoffice.domain.auth.abstraction.BusinessUnitService
import tech.pegb.backoffice.domain.auth.model.BusinessUnit
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.ExecutionContext

class BusinessUnitControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec: ExecutionContext = TestExecutionContext.genericOperations

  val businessUnitService: BusinessUnitService = stub[BusinessUnitService]
  val latestVersionService: LatestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[BusinessUnitService].to(businessUnitService),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock: Clock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  private val requestDateFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

  "BusinessUnit routes" should {
    "fail to create new business unit because of domain validation failure" in {

      val mockResult = ServiceError.validationError("Failed creating business unit. Empty business unit name is not allowed.")
      (businessUnitService.create(_: BusinessUnitToCreate, _: Boolean))
        .when(BusinessUnitToCreate(name = "", createdBy = mockRequestFrom, createdAt = mockRequestDate.toLocalDateTimeUTC), false)
        .returns(Left(mockResult).toFuture)

      val jsonRequest =
        s"""
           |{"name":""}
         """.stripMargin
      val resp = route(app, FakeRequest(POST, s"/business_units", jsonHeaders, jsonRequest)).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Failed creating business unit. Empty business unit name is not allowed.",
           |"tracking_id":"${mockResult.id}"}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail to create new business unit because request is malformed" in {

      val jsonRequest =
        s"""
           |{"unknown_property":false}
         """.stripMargin
      val resp = route(app, FakeRequest(POST, s"/business_units", jsonHeaders, jsonRequest)).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"MalformedRequest",
           |"msg":"Malformed request to create business_unit. A mandatory field might be missing or its value is of wrong type."}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "create new business unit" in {

      val jsonRequest =
        s"""
           |{"name":"Legal Department"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val mockResult = BusinessUnit(id = UUID.randomUUID(), name = "Legal Department",
        createdBy = mockRequestFrom, createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = None, updatedAt = None)
      (businessUnitService.create(_: BusinessUnitToCreate, _: Boolean))
        .when(BusinessUnitToCreate(name = "Legal Department", createdBy = mockRequestFrom, createdAt = mockRequestDate.toLocalDateTimeUTC), false)
        .returns(Right(mockResult).toFuture)

      val resp = route(app, FakeRequest(POST, s"/business_units", jsonHeaders, jsonRequest)).get
      val expectedJson =
        s"""
           |{"id":"${mockResult.id}",
           |"name":"Legal Department",
           |"created_by":"pegbuser",
           |"updated_by":null,
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "find business unit by id" in {
      val randomUuid = UUID.randomUUID()

      val mockResult = BusinessUnit(id = UUID.randomUUID(), name = "Legal Department",
        createdBy = mockRequestFrom, createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = None, updatedAt = None)
      (businessUnitService.getAllActiveBusinessUnits(_: BusinessUnitCriteria, _: Seq[tech.pegb.backoffice.domain.model.Ordering], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(id = Some(randomUuid)), Nil, None, None)
        .returns(Right(Seq(mockResult)).toFuture)

      val resp = route(app, FakeRequest(GET, s"/business_units/$randomUuid")).get
      val expectedJson =
        s"""
           |{"id":"${mockResult.id}",
           |"name":"Legal Department",
           |"created_by":"pegbuser",
           |"updated_by":null,
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "not found business unit by id" in {
      val randomUuid = UUID.randomUUID()

      (businessUnitService.getAllActiveBusinessUnits(_: BusinessUnitCriteria, _: Seq[tech.pegb.backoffice.domain.model.Ordering], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(id = Some(randomUuid)), Nil, None, None)
        .returns(Right(Nil).toFuture)

      val resp = route(app, FakeRequest(GET, s"/business_units/$randomUuid").withHeaders(jsonHeaders)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"NotFound",
           |"msg":"Business unit with id [$randomUuid] was not found."}
           |""".stripMargin.replace(System.lineSeparator(), "")
      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expectedJson
    }

    "find all business units" in {
      import tech.pegb.backoffice.domain.model.Ordering._
      import tech.pegb.backoffice.util.Implicits._

      val mockLatestVerResult = BusinessUnit(id = UUID.randomUUID(), name = "Legal Department",
        createdBy = mockRequestFrom, createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = Some("pegbuser"), updatedAt = Some(LocalDateTime.now))

      (businessUnitService.getAllActiveBusinessUnits(_: BusinessUnitCriteria, _: Seq[tech.pegb.backoffice.domain.model.Ordering], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(), "-updated_at".asDomain, Some(1), None)
        .returns(Right(Seq(mockLatestVerResult)).toFuture)

      (businessUnitService.countAllActiveBusinessUnits _).when(BusinessUnitCriteria()).returns(Right(10).toFuture)

      val id1 = UUID.randomUUID()
      val id2 = UUID.randomUUID()
      val id3 = UUID.randomUUID()
      val mockResults = Seq(
        BusinessUnit.empty.copy(id = id1, name = "Accounting", createdAt = LocalDateTime.of(2018, 1, 1, 0, 0), updatedBy = None, updatedAt = None),
        BusinessUnit.empty.copy(id = id2, name = "Operations", createdAt = LocalDateTime.of(2018, 1, 1, 0, 0), updatedBy = None, updatedAt = None),
        BusinessUnit.empty.copy(id = id3, name = "Maintenance", createdAt = LocalDateTime.of(2018, 1, 1, 0, 0), updatedBy = None, updatedAt = None),
        mockLatestVerResult)

      (businessUnitService.getAllActiveBusinessUnits(_: BusinessUnitCriteria, _: Seq[tech.pegb.backoffice.domain.model.Ordering], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(), Nil, Some(4), None)
        .returns(Right(mockResults).toFuture)

      val resp = route(app, FakeRequest(GET, s"/business_units?limit=4")).get
      val expectedJson =
        s"""
           |{"total":10,
           |"results":[
           |{"id":"$id1",
           |"name":"Accounting",
           |"created_by":"pegbuser",
           |"updated_by":null,
           |"created_at":${LocalDateTime.of(2018, 1, 1, 0, 0).toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${LocalDateTime.of(2018, 1, 1, 0, 0).toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null},
           |{"id":"$id2",
           |"name":"Operations",
           |"created_by":"pegbuser",
           |"updated_by":null,
           |"created_at":${LocalDateTime.of(2018, 1, 1, 0, 0).toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${LocalDateTime.of(2018, 1, 1, 0, 0).toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null},
           |{"id":"$id3",
           |"name":"Maintenance",
           |"created_by":"pegbuser",
           |"updated_by":null,
           |"created_at":${LocalDateTime.of(2018, 1, 1, 0, 0).toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${LocalDateTime.of(2018, 1, 1, 0, 0).toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null},
           |{"id":"${mockLatestVerResult.id}",
           |"name":"Legal Department",
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${mockLatestVerResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockLatestVerResult.updatedAt.map(_.toZonedDateTimeUTC).get.toJsonStr},
           |"created_time":${mockLatestVerResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockLatestVerResult.updatedAt.map(_.toZonedDateTimeUTC).get.toJsonStr}}],
           |"limit":4,"offset":null}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVerResult.updatedAt.map(_.toString)
    }

    "get only latest version in X-Version response header" in {
      import tech.pegb.backoffice.domain.model.Ordering._
      import tech.pegb.backoffice.util.Implicits._

      val mockLatestVerResult = BusinessUnit(id = UUID.randomUUID(), name = "Legal Department",
        createdBy = mockRequestFrom, createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = Some("pegbuser"), updatedAt = Some(LocalDateTime.now))

      (businessUnitService.getAllActiveBusinessUnits(_: BusinessUnitCriteria, _: Seq[tech.pegb.backoffice.domain.model.Ordering], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(), "-updated_at".asDomain, Some(1), None)
        .returns(Right(Seq(mockLatestVerResult)).toFuture)

      (businessUnitService.countAllActiveBusinessUnits _).when(BusinessUnitCriteria()).returns(Right(10).toFuture)

      val id1 = UUID.randomUUID()
      val id2 = UUID.randomUUID()
      val id3 = UUID.randomUUID()
      val mockResults = Seq(
        BusinessUnit.empty.copy(id = id1, name = "Accounting", createdAt = LocalDateTime.of(2018, 1, 1, 0, 0), updatedBy = None, updatedAt = None),
        BusinessUnit.empty.copy(id = id2, name = "Operations", createdAt = LocalDateTime.of(2018, 1, 1, 0, 0), updatedBy = None, updatedAt = None),
        BusinessUnit.empty.copy(id = id3, name = "Maintenance", createdAt = LocalDateTime.of(2018, 1, 1, 0, 0), updatedBy = None, updatedAt = None),
        mockLatestVerResult)

      (businessUnitService.getAllActiveBusinessUnits(_: BusinessUnitCriteria, _: Seq[tech.pegb.backoffice.domain.model.Ordering], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(), Nil, Some(4), None)
        .returns(Right(mockResults).toFuture)

      val resp = route(app, FakeRequest(HEAD, s"/business_units?limit=4")).get
      val expectedJson =
        s"""
           |{"total":0,
           |"results":[],
           |"limit":4,"offset":null}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVerResult.updatedAt.map(_.toString)
    }

    "update business unit by id" in {
      val randomUuid = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"name":"new_department_name",
           |"updated_at":"2019-01-01T00:00:00Z"}
         """.stripMargin

      val mockResult = BusinessUnit(
        id = randomUuid,
        name = "new_department_name", createdBy = "pegbuser",
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0),
        updatedBy = Some(mockRequestFrom), updatedAt = Some(mockRequestDate.toLocalDateTimeUTC))

      val mockInput = BusinessUnitToUpdate(name = Some("new_department_name"), updatedBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC, lastUpdatedAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0, 0)))

      (businessUnitService.update(_: UUID, _: BusinessUnitToUpdate))
        .when(randomUuid, mockInput)
        .returns(Right(mockResult).toFuture)

      val resp = route(app, FakeRequest(PUT, s"/business_units/$randomUuid", jsonHeaders, jsonRequest)).get
      val expectedJson =
        s"""
           |{"id":"$randomUuid",
           |"name":"new_department_name",
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockResult.updatedAt.map(_.toZonedDateTimeUTC).get.toJsonStr},
           |"created_time":${mockResult.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockResult.updatedAt.map(_.toZonedDateTimeUTC).get.toJsonStr}}
           |""".stripMargin.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "fail to update business unit by id -malformed request" in {
      val randomUuid = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"unknown_property":"new_department_name",
           |"updated_at":"2019-01-01T00:00:00Z"}
         """.stripMargin

      val resp = route(app, FakeRequest(PUT, s"/business_units/$randomUuid", jsonHeaders, jsonRequest)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"MalformedRequest",
           |"msg":"Malformed request to update business_unit. A mandatory field might be missing or its value is of wrong type."}
           |""".stripMargin.replace(System.lineSeparator(), "")
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "fail to update business unit by id -domain error" in {
      val randomUuid = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"name":"",
           |"updated_at":"2019-01-01T00:00:00Z"}
         """.stripMargin

      val mockResult = ServiceError.validationError("empty name not allowed")
      (businessUnitService.update(_: UUID, _: BusinessUnitToUpdate))
        .when(randomUuid, BusinessUnitToUpdate(
          name = Some(""),
          updatedBy = mockRequestFrom,
          updatedAt = mockRequestDate.toLocalDateTimeUTC,
          lastUpdatedAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0, 0))))
        .returns(Left(mockResult).toFuture)

      val resp = route(app, FakeRequest(PUT, s"/business_units/$randomUuid", jsonHeaders, jsonRequest)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"${mockResult.message}",
           |"tracking_id":"${mockResult.id}"}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "delete business unit by id" in {
      import tech.pegb.backoffice.util.Constants._
      val randomUuid = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"updated_at":null}
         """.stripMargin
      (businessUnitService.remove(_: UUID, _: BusinessUnitToRemove))
        .when(randomUuid, BusinessUnitToRemove(
          removedBy = mockRequestFrom,
          removedAt = mockRequestDate.toLocalDateTimeUTC, lastUpdatedAt = None))
        .returns(Right(UnitInstance).toFuture)

      val resp = route(app, FakeRequest(DELETE, s"/business_units/$randomUuid", jsonHeaders, jsonRequest)).get

      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      status(resp) mustBe OK
      contentAsString(resp) mustBe randomUuid.toJsonStr
      //status(resp) mustBe NO_CONTENT
    }

    "delete business unit by id even without json payload for updated_at (backwards compatible)" in {
      import tech.pegb.backoffice.util.Constants._
      val randomUuid = UUID.randomUUID()

      val jsonRequest = ""
      (businessUnitService.remove(_: UUID, _: BusinessUnitToRemove))
        .when(randomUuid, BusinessUnitToRemove(
          removedBy = mockRequestFrom,
          removedAt = mockRequestDate.toLocalDateTimeUTC, lastUpdatedAt = None))
        .returns(Right(UnitInstance).toFuture)

      val resp = route(app, FakeRequest(DELETE, s"/business_units/$randomUuid", jsonHeaders, jsonRequest)).get

      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      status(resp) mustBe OK
      contentAsString(resp) mustBe randomUuid.toJsonStr
      //status(resp) mustBe NO_CONTENT
    }

    "fail to delete business unit by id -domain error" in {
      val randomUuid = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"updated_at":null}
         """.stripMargin

      val mockResult = ServiceError.unknownError("some unexpected error")
      (businessUnitService.remove(_: UUID, _: BusinessUnitToRemove))
        .when(randomUuid, BusinessUnitToRemove(
          removedBy = mockRequestFrom,
          removedAt = mockRequestDate.toLocalDateTimeUTC, lastUpdatedAt = None))
        .returns(Left(mockResult).toFuture)

      val resp = route(app, FakeRequest(DELETE, s"/business_units/$randomUuid", jsonHeaders, jsonRequest)).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"Unknown",
           |"msg":"${mockResult.message}",
           |"tracking_id":"${mockResult.id}"}
           |""".stripMargin.replace(System.lineSeparator(), "")
      status(resp) mustBe INTERNAL_SERVER_ERROR
      contentAsString(resp) mustBe expectedJson
    }

  }
}
