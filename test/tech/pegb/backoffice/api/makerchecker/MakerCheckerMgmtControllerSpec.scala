package tech.pegb.backoffice.api.makerchecker

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Headers
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, contentAsString, route, _}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.makerchecker.controller.MakerCheckerMgmtController
import tech.pegb.backoffice.api.makerchecker.dto.TaskToCreate
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.makerchecker.abstraction.MakerCheckerService
import tech.pegb.backoffice.domain.makerchecker.dto.{MakerCheckerCriteria, TaskToApprove, TaskToCreate ⇒ TaskToCreateDomain}
import tech.pegb.backoffice.domain.makerchecker.implementation.MakerCheckerServiceImpl
import tech.pegb.backoffice.domain.makerchecker.model.RoleLevels.OtherLevel
import tech.pegb.backoffice.domain.makerchecker.model._
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.makerchecker.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class MakerCheckerMgmtControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  val makerCheckerService = stub[MakerCheckerService]
  val latestVersionService = stub[LatestVersionService]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[MakerCheckerService].to(makerCheckerService),
      bind[WithExecutionContexts].to(TestExecutionContext),
      bind[LatestVersionService].to(latestVersionService))

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  "MakerCheckerController" should {

    val apiKeyFromTrustedCaller = conf.get[String]("api-keys.backoffice-auth-api")

    "respond 200 with TaskDetailToRead json body in GET /tasks/:id" in {
      val mockId = UUID.randomUUID()
      val mockResult = MakerCheckerTask(
        id = mockId,
        module = "strings",
        actionRequired = "create i18n string",
        maker = MakerDetails(
          createdBy = "pegbuser",
          createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
          level = OtherLevel(3),
          businessUnit = "BackOffice"),
        makerRequest = MakerRequest(
          verb = "POST",
          url = "/strings",
          queryParams = None,
          body = Json.parse("""{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""").as[JsObject].some,
          headers = Json.parse("""{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}""").as[JsObject]),
        status = Statuses.Pending,
        reason = None,
        checker = None,
        updatedAt = None,
        change = None,
        current = None)

      (makerCheckerService.getTaskById(_: UUID)(_: UUID)).when(mockId, *).returns(Future.successful(Right(mockResult)))

      val requesterLevel = RoleLevels(2)
      val requesterBusinessUnit = "BackOffice"

      val resp = route(app, FakeRequest(GET, s"/tasks/$mockId")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.underlying.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      val expectedJson =
        s"""
           |{
           |"id":"$mockId",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":null,
           |"change":null,
           |"original_value":null,
           |"is_read_only":true,
           |"stale":false
           |}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "respond 403 No permission in GET /tasks/:id if task maker level is lower than role level in the request headers" ignore {

    }

    "respond 403 No permission in GET /tasks/:id if task maker business_unit is not the same business unit in the request headers " +
      "unless if the role is department head level" ignore {

      }

    "respond 200 with filtered by maker level TaskPaginatedResults json body in GET /tasks " +
      "if task maker level is lower than role level in the request headers" in {

        val mc1 = MakerCheckerTask(
          id = UUID.fromString("06d18f41-1abf-4507-afab-5f8e1c7a1601"),
          module = "strings",
          actionRequired = "create i18n string",
          maker = MakerDetails(
            createdBy = "pegbuser",
            createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
            level = OtherLevel(3),
            businessUnit = "BackOffice"),
          makerRequest = MakerRequest(
            verb = "POST",
            url = "/strings",
            queryParams = None,
            body = Json.parse("""{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""").as[JsObject].some,
            headers = Json.parse("""{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}""").as[JsObject]),
          status = Statuses.Pending,
          reason = None,
          checker = None,
          updatedAt = None,
          change = None,
          current = None)

        val mc2 = MakerCheckerTask(
          id = UUID.fromString("e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8"),
          module = "strings",
          actionRequired = "create i18n string",
          maker = MakerDetails(
            createdBy = "pegbuser",
            createdAt = LocalDateTime.parse("2019-01-02T00:10:30", formatter),
            level = OtherLevel(2),
            businessUnit = "BackOffice"),
          makerRequest = MakerRequest(
            verb = "POST",
            url = "/strings",
            queryParams = None,
            body = Json.parse("""{"text":"hola","locale":"es","explanation":"text for hello world hahah","key":"hello","platform":"web"}""").as[JsObject].some,
            headers = Json.parse("""{"X-UserName":"pegbuser","X-RoleLevel":"2","content-type":"application/json","X-BusinessUnit":"BackOffice"}""").as[JsObject]),
          status = Statuses.Pending,
          reason = None,
          checker = None,
          updatedAt = None,
          change = None,
          current = None)

        val expectedJson =
          s"""
             |{
             |"total":2,
             |"results":[{
             |"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
             |"module":"strings",
             |"action":"create i18n string",
             |"status":"pending",
             |"reason":null,
             |"created_at":"2019-01-01T00:10:30Z",
             |"created_by":"pegbuser",
             |"checked_at":null,
             |"checked_by":null,
             |"updated_at":null,
             |"is_read_only":true
             |},
             |{
             |"id":"e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
             |"module":"strings",
             |"action":"create i18n string",
             |"status":"pending",
             |"reason":null,
             |"created_at":"2019-01-02T00:10:30Z",
             |"created_by":"pegbuser",
             |"checked_at":null,
             |"checked_by":null,
             |"updated_at":null,
             |"is_read_only":true
             |}],"limit":null,"offset":null}""".stripMargin.replace(System.lineSeparator(), "")

        val makerCheckerCriteria = MakerCheckerCriteria(
          id = None,
          module = "strings".some,
          status = Statuses.Pending.some,
          createdAtFrom = LocalDateTime.parse("2019-01-01T00:00:00", formatter).some,
          createdAtTo = LocalDateTime.parse("2019-01-02T23:59:59", formatter).some,
          partialMatchFields = Constants.makerCheckerPartialMatchFields.filterNot(_ == "disabled"))

        val ordering = Seq(Ordering("created_at", Ordering.ASCENDING))

        val requesterLevel = RoleLevels(2)
        val requesterBusinessUnit = "BackOffice"
        val mockLatestVersion = LocalDateTime.now.toString

        (latestVersionService.getLatestVersion _).when(MakerCheckerServiceImpl.getValidDaoCriteria(makerCheckerCriteria, requesterLevel, requesterBusinessUnit))
          .returns(Right(mockLatestVersion.some).toFuture)

        (makerCheckerService.getTasksByCriteria(
          _: MakerCheckerCriteria,
          _: RoleLevel,
          _: String,
          _: Seq[Ordering],
          _: Option[Int],
          _: Option[Int])(_: UUID)).when(
            makerCheckerCriteria,
            requesterLevel,
            requesterBusinessUnit,
            ordering,
            None,
            None,
            *).returns(
            Future.successful(Right(Seq(mc1, mc2))))

        (makerCheckerService.countTasksByCriteria(
          _: MakerCheckerCriteria,
          _: RoleLevel,
          _: String)).when(makerCheckerCriteria, requesterLevel, requesterBusinessUnit)
          .returns(Future.successful(Right(2)))

        val resp = route(app, FakeRequest(GET, s"/tasks?module=strings&status=pending&date_from=2019-01-01&date_to=2019-01-02&order_by=created_at")
          .withHeaders(jsonHeaders.add(
            requestRoleLevelKey → requesterLevel.underlying.toString,
            requestHeaderBuKey → requesterBusinessUnit,
            requestHeaderApiKey → apiKeyFromTrustedCaller))).get

        contentAsString(resp) mustBe expectedJson

      }

    "respond 200 with filtered by status TaskPaginatedResults json body in GET /tasks?status=..." in {
      val mc1 = MakerCheckerTask(
        id = UUID.fromString("06d18f41-1abf-4507-afab-5f8e1c7a1601"),
        module = "strings",
        actionRequired = "create i18n string",
        maker = MakerDetails(
          createdBy = "pegbuser",
          createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
          level = OtherLevel(3),
          businessUnit = "BackOffice"),
        makerRequest = MakerRequest(
          verb = "POST",
          url = "/strings",
          queryParams = None,
          body = Json.parse("""{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""").as[JsObject].some,
          headers = Json.parse("""{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}""").as[JsObject]),
        status = Statuses.Rejected,
        reason = Option("expired request"),
        checker = CheckerDetails(
          checkedBy = "george",
          checkedAt = LocalDateTime.parse("2019-03-02T00:10:30", formatter),
          level = RoleLevels(2).some,
          businessUnit = "BackOffice".some).some,
        updatedAt = LocalDateTime.parse("2019-03-02T00:10:30", formatter).some,
        change = None,
        current = None)

      val mc2 = MakerCheckerTask(
        id = UUID.fromString("e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8"),
        module = "strings",
        actionRequired = "create i18n string",
        maker = MakerDetails(
          createdBy = "pegbuser",
          createdAt = LocalDateTime.parse("2019-01-02T00:10:30", formatter),
          level = OtherLevel(2),
          businessUnit = "BackOffice"),
        makerRequest = MakerRequest(
          verb = "POST",
          url = "/strings",
          queryParams = None,
          body = Json.parse("""{"text":"hola","locale":"es","explanation":"text for hello world hahah","key":"hello","platform":"web"}""").as[JsObject].some,
          headers = Json.parse("""{"X-UserName":"pegbuser","X-RoleLevel":"2","content-type":"application/json","X-BusinessUnit":"BackOffice"}""").as[JsObject]),
        status = Statuses.Rejected,
        reason = Option("expired request"),
        checker = CheckerDetails(
          checkedBy = "george",
          checkedAt = LocalDateTime.parse("2019-02-02T00:10:30", formatter),
          level = RoleLevels(2).some,
          businessUnit = "BackOffice".some).some,
        updatedAt = LocalDateTime.parse("2019-02-02T00:10:30", formatter).some,
        change = None,
        current = None)

      val expectedJson =
        s"""
           |{
           |"total":2,
           |"results":[{
           |"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"rejected",
           |"reason":"expired request",
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-03-02T00:10:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-03-02T00:10:30Z",
           |"is_read_only":true
           |},
           |{
           |"id":"e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"rejected",
           |"reason":"expired request",
           |"created_at":"2019-01-02T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-02-02T00:10:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-02-02T00:10:30Z",
           |"is_read_only":true
           |}],"limit":null,"offset":null}""".stripMargin.replace(System.lineSeparator(), "")

      val makerCheckerCriteria = MakerCheckerCriteria(
        id = None,
        module = None,
        status = Statuses.Rejected.some,
        createdAtFrom = None,
        createdAtTo = None,
        partialMatchFields = Constants.makerCheckerPartialMatchFields.filterNot(_ == "disabled"))

      val ordering = Seq(Ordering("updated_at", Ordering.DESCENDING))

      val requesterLevel = RoleLevels(2)
      val requesterBusinessUnit = "BackOffice"
      val mockLatestVersion = LocalDateTime.now.toString

      (latestVersionService.getLatestVersion _).when(MakerCheckerServiceImpl.getValidDaoCriteria(makerCheckerCriteria, requesterLevel, requesterBusinessUnit))
        .returns(Right(mockLatestVersion.some).toFuture)
      (makerCheckerService.getTasksByCriteria(
        _: MakerCheckerCriteria,
        _: RoleLevel,
        _: String,
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int])(_: UUID)).when(
          makerCheckerCriteria,
          requesterLevel,
          requesterBusinessUnit,
          ordering,
          None,
          None,
          *).returns(
          Future.successful(Right(Seq(mc1, mc2))))

      (makerCheckerService.countTasksByCriteria(
        _: MakerCheckerCriteria,
        _: RoleLevel,
        _: String)).when(makerCheckerCriteria, requesterLevel, requesterBusinessUnit)
        .returns(Future.successful(Right(2)))

      val resp = route(app, FakeRequest(GET, s"/tasks?status=rejected&order_by=-updated_at")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.underlying.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      contentAsString(resp) mustBe expectedJson

    }

    "respond 200 with filtered by module TaskPaginatedResults json body in GET /tasks?module=..." in {
      val mc1 = MakerCheckerTask(
        id = UUID.fromString("06d18f41-1abf-4507-afab-5f8e1c7a1601"),
        module = "strings",
        actionRequired = "create i18n string",
        maker = MakerDetails(
          createdBy = "pegbuser",
          createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
          level = OtherLevel(3),
          businessUnit = "BackOffice"),
        makerRequest = MakerRequest(
          verb = "POST",
          url = "/strings",
          queryParams = None,
          body = Json.parse("""{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""").as[JsObject].some,
          headers = Json.parse("""{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}""").as[JsObject]),
        status = Statuses.Rejected,
        reason = Option("expired request"),
        checker = CheckerDetails(
          checkedBy = "george",
          checkedAt = LocalDateTime.parse("2019-03-02T00:10:30", formatter),
          level = RoleLevels(2).some,
          businessUnit = "BackOffice".some).some,
        updatedAt = LocalDateTime.parse("2019-03-02T00:10:30", formatter).some,
        change = None,
        current = None)

      val mc2 = MakerCheckerTask(
        id = UUID.fromString("e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8"),
        module = "strings",
        actionRequired = "create i18n string",
        maker = MakerDetails(
          createdBy = "pegbuser",
          createdAt = LocalDateTime.parse("2019-01-02T00:10:30", formatter),
          level = OtherLevel(2),
          businessUnit = "BackOffice"),
        makerRequest = MakerRequest(
          verb = "POST",
          url = "/strings",
          queryParams = None,
          body = Json.parse("""{"text":"hola","locale":"es","explanation":"text for hello world hahah","key":"hello","platform":"web"}""").as[JsObject].some,
          headers = Json.parse("""{"X-UserName":"pegbuser","X-RoleLevel":"2","content-type":"application/json","X-BusinessUnit":"BackOffice"}""").as[JsObject]),
        status = Statuses.Rejected,
        reason = Option("expired request"),
        checker = CheckerDetails(
          checkedBy = "george",
          checkedAt = LocalDateTime.parse("2019-02-02T00:10:30", formatter),
          level = RoleLevels(2).some,
          businessUnit = "BackOffice".some).some,
        updatedAt = LocalDateTime.parse("2019-02-02T00:10:30", formatter).some,
        change = None,
        current = None)

      val expectedJson =
        s"""
           |{
           |"total":2,
           |"results":[{
           |"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"rejected",
           |"reason":"expired request",
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-03-02T00:10:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-03-02T00:10:30Z",
           |"is_read_only":true
           |},
           |{
           |"id":"e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"rejected",
           |"reason":"expired request",
           |"created_at":"2019-01-02T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-02-02T00:10:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-02-02T00:10:30Z",
           |"is_read_only":true
           |}],"limit":null,"offset":null}""".stripMargin.replace(System.lineSeparator(), "")

      val makerCheckerCriteria = MakerCheckerCriteria(
        id = None,
        module = "strings".some,
        status = None,
        createdAtFrom = None,
        createdAtTo = None,
        partialMatchFields = Constants.makerCheckerPartialMatchFields.filterNot(_ == "disabled"))

      val ordering = Seq(Ordering("updated_at", Ordering.DESCENDING))

      val requesterLevel = RoleLevels(2)
      val requesterBusinessUnit = "BackOffice"
      val mockLatestVersion = LocalDateTime.now.toString

      (latestVersionService.getLatestVersion _).when(MakerCheckerServiceImpl.getValidDaoCriteria(makerCheckerCriteria, requesterLevel, requesterBusinessUnit))
        .returns(Right(mockLatestVersion.some).toFuture)
      (makerCheckerService.getTasksByCriteria(
        _: MakerCheckerCriteria,
        _: RoleLevel,
        _: String,
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int])(_: UUID)).when(
          makerCheckerCriteria,
          requesterLevel,
          requesterBusinessUnit,
          ordering,
          None,
          None,
          *).returns(
          Future.successful(Right(Seq(mc1, mc2))))

      (makerCheckerService.countTasksByCriteria(
        _: MakerCheckerCriteria,
        _: RoleLevel,
        _: String)).when(makerCheckerCriteria, requesterLevel, requesterBusinessUnit)
        .returns(Future.successful(Right(2)))

      val resp = route(app, FakeRequest(GET, s"/tasks?module=strings&order_by=-updated_at")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.underlying.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      contentAsString(resp) mustBe expectedJson
    }

    "respond 200 with filtered by created_at TaskPaginatedResults json body in GET /tasks?date_from=...&date_to=..." in {

      val mc1 = MakerCheckerTask(
        id = UUID.fromString("06d18f41-1abf-4507-afab-5f8e1c7a1601"),
        module = "strings",
        actionRequired = "create i18n string",
        maker = MakerDetails(
          createdBy = "pegbuser",
          createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
          level = OtherLevel(3),
          businessUnit = "BackOffice"),
        makerRequest = MakerRequest(
          verb = "POST",
          url = "/strings",
          queryParams = None,
          body = Json.parse("""{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""").as[JsObject].some,
          headers = Json.parse("""{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}""").as[JsObject]),
        status = Statuses.Pending,
        reason = None,
        checker = None,
        updatedAt = None,
        change = None,
        current = None)

      val mc2 = MakerCheckerTask(
        id = UUID.fromString("e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8"),
        module = "strings",
        actionRequired = "create i18n string",
        maker = MakerDetails(
          createdBy = "pegbuser",
          createdAt = LocalDateTime.parse("2019-01-02T00:10:30", formatter),
          level = OtherLevel(2),
          businessUnit = "BackOffice"),
        makerRequest = MakerRequest(
          verb = "POST",
          url = "/strings",
          queryParams = None,
          body = Json.parse("""{"text":"hola","locale":"es","explanation":"text for hello world hahah","key":"hello","platform":"web"}""").as[JsObject].some,
          headers = Json.parse("""{"X-UserName":"pegbuser","X-RoleLevel":"2","content-type":"application/json","X-BusinessUnit":"BackOffice"}""").as[JsObject]),
        status = Statuses.Pending,
        reason = None,
        checker = None,
        updatedAt = None,
        change = None,
        current = None)

      val expectedJson =
        s"""
           |{
           |"total":2,
           |"results":[{
           |"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":null,
           |"is_read_only":true
           |},
           |{
           |"id":"e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-02T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":null,
           |"is_read_only":true
           |}],"limit":null,"offset":null}""".stripMargin.replace(System.lineSeparator(), "")

      val makerCheckerCriteria = MakerCheckerCriteria(
        id = None,
        module = None,
        status = None,
        createdAtFrom = LocalDateTime.parse("2019-01-01T00:00:00", formatter).some,
        createdAtTo = LocalDateTime.parse("2019-01-02T23:59:59", formatter).some,
        partialMatchFields = Constants.makerCheckerPartialMatchFields.filterNot(_ == "disabled"))

      val ordering = Seq(Ordering("created_at", Ordering.ASCENDING))

      val requesterLevel = RoleLevels(2)
      val requesterBusinessUnit = "BackOffice"
      val mockLatestVersion = LocalDateTime.now.toString

      (latestVersionService.getLatestVersion _).when(MakerCheckerServiceImpl.getValidDaoCriteria(makerCheckerCriteria, requesterLevel, requesterBusinessUnit))
        .returns(Right(mockLatestVersion.some).toFuture)
      (makerCheckerService.getTasksByCriteria(
        _: MakerCheckerCriteria,
        _: RoleLevel,
        _: String,
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int])(_: UUID)).when(
          makerCheckerCriteria,
          requesterLevel,
          requesterBusinessUnit,
          ordering,
          None,
          None,
          *).returns(
          Future.successful(Right(Seq(mc1, mc2))))

      (makerCheckerService.countTasksByCriteria(
        _: MakerCheckerCriteria,
        _: RoleLevel,
        _: String)).when(makerCheckerCriteria, requesterLevel, requesterBusinessUnit)
        .returns(Future.successful(Right(2)))

      val resp = route(app, FakeRequest(GET, s"/tasks?date_from=2019-01-01&date_to=2019-01-02&order_by=created_at")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.underlying.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      contentAsString(resp) mustBe expectedJson

    }

    "respond 200 with TaskToRead json body in PUT /tasks/:id/approve" in {
      val requestId = UUID.randomUUID()

      val requestHeaderFrom = "George"
      val requestHeaderDate = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)
      val requestHeaderRoleLevel = 1
      val requestHeaderBu = "Tech Support"

      val taskId = UUID.randomUUID()

      val expectedInput = TaskToApprove(
        id = taskId,
        maybeReason = None, //possible justification why not rejected
        approvedBy = requestHeaderFrom,
        approvedAt = requestHeaderDate.toLocalDateTime,
        checkerLevel = RoleLevels(requestHeaderRoleLevel),
        checkerBusinessUnit = "Tech Support")

      val mockResult = MakerCheckerTask(
        id = taskId,
        module = "some module",
        actionRequired = "some action",
        maker = MakerDetails(
          createdBy = "some maker",
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0),
          level = RoleLevels(2),
          businessUnit = requestHeaderBu),
        makerRequest = MakerRequest(
          verb = "POST",
          url = "/entity",
          queryParams = None,
          body = Json.parse("""{"field1":"value1"}""").as[JsObject].some,
          headers = Json.parse("""{"field1":"value1"}""").as[JsObject]),
        status = Statuses.Approved,
        reason = None,
        checker = Some(CheckerDetails(
          checkedBy = expectedInput.approvedBy,
          checkedAt = expectedInput.approvedAt,
          level = Some(expectedInput.checkerLevel),
          businessUnit = Some(expectedInput.checkerBusinessUnit))))

      (makerCheckerService.approvePendingTask _)
        .when(expectedInput).returns(Future.successful(Right(mockResult)))

      val jsonRequest =
        s"""{
           |"reason": null
           |}
         """.stripMargin

      val expectedJson =
        s"""
           |{"id":"$taskId",
           |"module":"some module","action":"some action",
           |"status":"approved",
           |"reason":null,
           |"created_at":"2019-01-01T00:00:00Z",
           |"created_by":"some maker",
           |"checked_at":"2019-01-01T00:00:00Z",
           |"checked_by":"George",
           |"updated_at":null,
           |"is_read_only":false
           |}

         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val makerCheckerHeaders = Headers(
        CONTENT_TYPE → JSON,
        requestIdHeaderKey → requestId.toString,
        requestDateHeaderKey → requestHeaderDate.toString,
        requestFromHeaderKey → requestHeaderFrom,
        requestRoleLevelKey → requestHeaderRoleLevel.toString,
        requestHeaderBuKey → requestHeaderBu,
        requestHeaderApiKey → apiKeyFromTrustedCaller)

      val fakeRequest = FakeRequest(PUT, s"/tasks/$taskId/approve",
        makerCheckerHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "accept reason json and respond 200 with TaskToRead json body in PUT /tasks/:id/reject" in {

    }

    "accept TaskToCreate json in POST /tasks and respond with 202 with TaskToRead json body" in {
      val requestId = UUID.randomUUID()
      val requestHeaderFrom = "George"
      val requestHeaderDate = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)
      val requestHeaderRoleLevel = 1
      val requestHeaderBu = "Tech Support"

      val expectedInput = TaskToCreate(
        verb = "POST",
        url = "$backoffice_api_host/manual_transactions",
        body = Option(Json.parse("""{"id":1,"account":"0001","direction":"debit","amount":500,"type":"ÉÑñsdþ للترحيب <b> yes </b> 你好 हेलो こんにちは Привет γεια σας"}""").as[JsObject]),
        headers = Json.parse("""{"X-UserName":"George","Date":"2019-01-01T00:00:00Z","X-RoleLevel":1,"X-BusinessUnit":"Support"}""").as[JsObject],
        module = "Manual Settlement",
        action = "create manual settlement").asDomain(
          doneBy = requestHeaderFrom,
          doneAt = requestHeaderDate,
          level = requestHeaderRoleLevel,
          businessUnit = requestHeaderBu).get

      val mockResult = MakerCheckerTask(
        id = UUID.randomUUID(),
        module = expectedInput.module,
        actionRequired = expectedInput.action,
        maker = MakerDetails(
          createdBy = expectedInput.maker.createdBy,
          createdAt = expectedInput.maker.createdAt,
          level = expectedInput.maker.level,
          businessUnit = expectedInput.maker.businessUnit),
        makerRequest = MakerRequest(
          verb = expectedInput.makerRequest.verb,
          rawUrl = expectedInput.makerRequest.rawUrl,
          body = expectedInput.makerRequest.body,
          headers = expectedInput.makerRequest.headers),
        status = Statuses.Pending,
        reason = None)

      val jsonRequest =
        s"""{
           |  "verb": "${mockResult.makerRequest.verb.toString}",
           |  "url": "${mockResult.makerRequest.rawUrl}",
           |  "body": ${mockResult.makerRequest.body.get},
           |  "headers": ${mockResult.makerRequest.headers},
           |  "module": "${mockResult.module}",
           |  "action": "${mockResult.actionRequired}"
           |}""".stripMargin

      (makerCheckerService.createPendingTask(_: TaskToCreateDomain, _: UUID)).when(expectedInput, requestId).returns(Future.successful(Right(mockResult)))

      val expectedJson =
        s"""{"id":"${mockResult.id}",
             |"link":"/tasks/${mockResult.id}"
             |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val makerCheckerHeaders = Headers(
        CONTENT_TYPE → JSON,
        requestIdHeaderKey → requestId.toString,
        requestDateHeaderKey → requestHeaderDate.toString,
        requestFromHeaderKey → requestHeaderFrom,
        requestRoleLevelKey → requestHeaderRoleLevel.toString,
        requestHeaderBuKey → requestHeaderBu,
        requestHeaderApiKey → apiKeyFromTrustedCaller)

      val fakeRequest = FakeRequest(POST, s"/tasks", makerCheckerHeaders, jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe ACCEPTED
      contentAsJson(resp).as[JsObject] mustBe Json.parse(expectedJson).as[JsObject]
    }

    "accept TaskToCreate json in POST /tasks for PUT and respond with 202 with TaskToRead json body" in {
      val requestId = UUID.randomUUID()
      val requestHeaderFrom = "George"
      val requestHeaderDate = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)
      val requestHeaderRoleLevel = 1
      val requestHeaderBu = "Tech Support"

      val body =
        """
          |{
          |"metadata_id": "types",
          |"key": "communication_channels",
          |"explanation": "List of valid channels that may be used for notifications from the system",
          |"value": [{
          |"id": 126,
          |"description": "SMS",
          |"name": "sms"
          |}, {
          |"id": 127,
          |"description": "Email",
          |"name": "email"
          |},{
          |"description": "Push",
          |"name": "push"
          |}]
          |	}
        """.stripMargin.trim.replaceAll("\n", "")

      val currentValue =
        """
          |{
          |"metadata_id": "types",
          |"key": "communication_channels",
          |"explanation": "List of valid channels that may be used for notifications from the system",
          |"value": [{
          |"id": 126,
          |"description": "SMS",
          |"name": "sms"
          |}, {
          |"id": 127,
          |"description": "Email",
          |"name": "email"
          |}]
          |	}
        """.stripMargin.trim.replaceAll("\n", "")

      val expectedInput = TaskToCreate(
        verb = "PUT",
        url = "$backoffice_api_host/parameters/30303034-3a30-3030-3030-303030303339",
        body = Option(Json.parse(body).as[JsObject]),
        headers = Json.parse("""{"X-UserName":"George","Date":"2019-01-01T00:00:00Z","X-RoleLevel":1,"X-BusinessUnit":"Support"}""").as[JsObject],
        module = "Parameter Management",
        action = "create types").asDomain(
          doneBy = requestHeaderFrom,
          doneAt = requestHeaderDate,
          level = requestHeaderRoleLevel,
          businessUnit = requestHeaderBu).get

      val mockResult = MakerCheckerTask(
        id = UUID.randomUUID(),
        module = expectedInput.module,
        actionRequired = expectedInput.action,
        maker = MakerDetails(
          createdBy = expectedInput.maker.createdBy,
          createdAt = expectedInput.maker.createdAt,
          level = expectedInput.maker.level,
          businessUnit = expectedInput.maker.businessUnit),
        makerRequest = MakerRequest(
          verb = expectedInput.makerRequest.verb,
          rawUrl = expectedInput.makerRequest.rawUrl,
          body = expectedInput.makerRequest.body,
          headers = expectedInput.makerRequest.headers),
        status = Statuses.Pending,
        reason = None,
        current = currentValue.asJsNode.some)

      val jsonRequest =
        s"""{
           |  "verb": "${mockResult.makerRequest.verb.toString}",
           |  "url": "${mockResult.makerRequest.rawUrl}",
           |  "body": ${mockResult.makerRequest.body.get},
           |  "headers": ${mockResult.makerRequest.headers},
           |  "module": "${mockResult.module}",
           |  "action": "${mockResult.actionRequired}"
           |}""".stripMargin

      (makerCheckerService.createPendingTask(_: TaskToCreateDomain, _: UUID)).when(expectedInput, requestId).returns(Future.successful(Right(mockResult)))

      val expectedJson =
        s"""{"id":"${mockResult.id}",
           |"link":"/tasks/${mockResult.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val makerCheckerHeaders = Headers(
        CONTENT_TYPE → JSON,
        requestIdHeaderKey → requestId.toString,
        requestDateHeaderKey → requestHeaderDate.toString,
        requestFromHeaderKey → requestHeaderFrom,
        requestRoleLevelKey → requestHeaderRoleLevel.toString,
        requestHeaderBuKey → requestHeaderBu,
        requestHeaderApiKey → apiKeyFromTrustedCaller)

      val fakeRequest = FakeRequest(POST, s"/tasks",
        makerCheckerHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe ACCEPTED
      contentAsJson(resp).as[JsObject] mustBe Json.parse(expectedJson).as[JsObject]
    }

    "respond 400 'Role level not allowed to create task' in POST /tasks if role level in the request header is zero or CEO equivalent" in {
      val requestId = UUID.randomUUID()
      val requestHeaderFrom = "George"
      val requestHeaderDate = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)
      val requestHeaderRoleLevel = 0
      val requestHeaderBu = "Tech Support"

      val expectedInput = TaskToCreate(
        verb = "POST",
        url = "$backoffice_api_host/manual_transactions",
        body = Option(Json.parse("""{"id":1,"account":"0001","direction":"debit","amount":500,"type":"asset"}""").as[JsObject]),
        headers = Json.parse("""{"X-UserName":"George","Date":"2019-01-01T00:00:00Z","X-RoleLevel":1,"X-BusinessUnit":"Support"}""").as[JsObject],
        module = "Manual Settlement",
        action = "create manual settlement").asDomain(
          doneBy = requestHeaderFrom,
          doneAt = requestHeaderDate,
          level = requestHeaderRoleLevel,
          businessUnit = requestHeaderBu).get

      val mockResult = ServiceError.validationError("Role level is not allowed to create a task", Some(UUID.randomUUID()))

      val jsonRequest =
        s"""{
           |  "verb": "${expectedInput.makerRequest.verb.toString}",
           |  "url": "${expectedInput.makerRequest.rawUrl}",
           |  "body": ${expectedInput.makerRequest.body.get},
           |  "headers": ${expectedInput.makerRequest.headers},
           |  "module": "${expectedInput.module}",
           |  "action": "${expectedInput.action}"
           |}""".stripMargin

      (makerCheckerService.createPendingTask(_: TaskToCreateDomain, _: UUID)).when(expectedInput, requestId).returns(Future.successful(Left(mockResult)))

      val expectedJson =
        s"""{"id":"$requestId",
            |"code":"InvalidRequest",
            |"msg":"Role level is not allowed to create a task",
            |"tracking_id":"${mockResult.id}"
            |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val makerCheckerHeaders = Headers(
        CONTENT_TYPE → JSON,
        requestIdHeaderKey → requestId.toString,
        requestDateHeaderKey → requestHeaderDate.toString,
        requestFromHeaderKey → requestHeaderFrom,
        requestRoleLevelKey → requestHeaderRoleLevel.toString,
        requestHeaderBuKey → requestHeaderBu,
        requestHeaderApiKey → apiKeyFromTrustedCaller)

      val fakeRequest = FakeRequest(POST, s"/tasks",
        makerCheckerHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "respond 401 Source of request is not trusted if api key in POST /tasks request header missing" in {
      val requestId = UUID.randomUUID()
      val requestHeaderFrom = "George"
      val requestHeaderDate = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)
      val requestHeaderRoleLevel = 0
      val requestHeaderBu = "Tech Support"

      val expectedInput = TaskToCreate(
        verb = "POST",
        url = "$backoffice_api_host/manual_transactions",
        body = Option(Json.parse("""{"id":1,"account":"0001","direction":"debit","amount":500,"type":"asset"}""").as[JsObject]),
        headers = Json.parse("""{"X-UserName":"George","Date":"2019-01-01T00:00:00Z","X-RoleLevel":1,"X-BusinessUnit":"Support"}""").as[JsObject],
        module = "Manual Settlement",
        action = "create manual settlement").asDomain(
          doneBy = requestHeaderFrom,
          doneAt = requestHeaderDate,
          level = requestHeaderRoleLevel,
          businessUnit = requestHeaderBu).get

      val jsonRequest =
        s"""{
           |  "verb": "${expectedInput.makerRequest.verb.toString}",
           |  "url": "${expectedInput.makerRequest.rawUrl}",
           |  "body": ${expectedInput.makerRequest.body.get},
           |  "headers": ${expectedInput.makerRequest.headers},
           |  "module": "${expectedInput.module}",
           |  "action": "${expectedInput.action}"
           |}""".stripMargin

      val expectedJson =
        s"""{"id":"$requestId",
           |"code":"NotAuthorized",
           |"msg":"${MakerCheckerMgmtController.ApiKeyMissingOrMismatch}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val makerCheckerHeaders = Headers(
        CONTENT_TYPE → JSON,
        requestIdHeaderKey → requestId.toString,
        requestDateHeaderKey → requestHeaderDate.toString,
        requestFromHeaderKey → requestHeaderFrom,
        requestRoleLevelKey → requestHeaderRoleLevel.toString,
        requestHeaderBuKey → requestHeaderBu /*,
        requestHeaderApiKey → apiKeyFromTrustedCaller*/ )

      val fakeRequest = FakeRequest(POST, s"/tasks",
        makerCheckerHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe UNAUTHORIZED
      contentAsString(resp) mustBe expectedJson
    }

    "respond 401 Source of request is not trusted if api key in POST /tasks request header incorrect" in {
      val requestId = UUID.randomUUID()
      val requestHeaderFrom = "George"
      val requestHeaderDate = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)
      val requestHeaderRoleLevel = 0
      val requestHeaderBu = "Tech Support"
      val wrongApiKey = "kjsdnfkljwh4tuiahnjcknzdckljuwbeiufns387rw8e9iufjidz"

      val expectedInput = TaskToCreate(
        verb = "POST",
        url = "$backoffice_api_host/manual_transactions",
        body = Option(Json.parse("""{"id":1,"account":"0001","direction":"debit","amount":500,"type":"asset"}""").as[JsObject]),
        headers = Json.parse("""{"X-UserName":"George","Date":"2019-01-01T00:00:00Z","X-RoleLevel":1,"X-BusinessUnit":"Support"}""").as[JsObject],
        module = "Manual Settlement",
        action = "create manual settlement").asDomain(
          doneBy = requestHeaderFrom,
          doneAt = requestHeaderDate,
          level = requestHeaderRoleLevel,
          businessUnit = requestHeaderBu).get

      val jsonRequest =
        s"""{
           |  "verb": "${expectedInput.makerRequest.verb.toString}",
           |  "url": "${expectedInput.makerRequest.rawUrl}",
           |  "body": ${expectedInput.makerRequest.body.get},
           |  "headers": ${expectedInput.makerRequest.headers},
           |  "module": "${expectedInput.module}",
           |  "action": "${expectedInput.action}"
           |}""".stripMargin

      val expectedJson =
        s"""{"id":"$requestId",
           |"code":"NotAuthorized",
           |"msg":"${MakerCheckerMgmtController.ApiKeyMissingOrMismatch}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val makerCheckerHeaders = Headers(
        CONTENT_TYPE → JSON,
        requestIdHeaderKey → requestId.toString,
        requestDateHeaderKey → requestHeaderDate.toString,
        requestFromHeaderKey → requestHeaderFrom,
        requestRoleLevelKey → requestHeaderRoleLevel.toString,
        requestHeaderBuKey → requestHeaderBu,
        requestHeaderApiKey → wrongApiKey)

      val fakeRequest = FakeRequest(POST, s"/tasks",
        makerCheckerHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe UNAUTHORIZED
      contentAsString(resp) mustBe expectedJson
    }

    "respond 400 Role level not found in request headers if role level in POST /tasks request header missing" in {
      val requestId = UUID.randomUUID()
      val requestHeaderFrom = "George"
      val requestHeaderDate = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)
      val requestHeaderRoleLevel = 0
      val requestHeaderBu = "Tech Support"

      val expectedInput = TaskToCreate(
        verb = "POST",
        url = "$backoffice_api_host/manual_transactions",
        body = Option(Json.parse("""{"id":1,"account":"0001","direction":"debit","amount":500,"type":"asset"}""").as[JsObject]),
        headers = Json.parse("""{"X-UserName":"George","Date":"2019-01-01T00:00:00Z","X-RoleLevel":1,"X-BusinessUnit":"Support"}""").as[JsObject],
        module = "Manual Settlement",
        action = "create manual settlement").asDomain(
          doneBy = requestHeaderFrom,
          doneAt = requestHeaderDate,
          level = requestHeaderRoleLevel,
          businessUnit = requestHeaderBu).get

      val jsonRequest =
        s"""{
           |  "verb": "${expectedInput.makerRequest.verb.toString}",
           |  "url": "${expectedInput.makerRequest.rawUrl}",
           |  "body": ${expectedInput.makerRequest.body.get},
           |  "headers": ${expectedInput.makerRequest.headers},
           |  "module": "${expectedInput.module}",
           |  "action": "${expectedInput.action}"
           |}""".stripMargin

      val expectedJson =
        s"""{"id":"$requestId",
           |"code":"InvalidRequest",
           |"msg":"${MakerCheckerMgmtController.RoleLevelMissing}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val makerCheckerHeaders = Headers(
        CONTENT_TYPE → JSON,
        requestIdHeaderKey → requestId.toString,
        requestDateHeaderKey → requestHeaderDate.toString,
        requestFromHeaderKey → requestHeaderFrom,
        /*requestRoleLevelKey → requestHeaderRoleLevel.toString,*/
        requestHeaderBuKey → requestHeaderBu,
        requestHeaderApiKey → apiKeyFromTrustedCaller)

      val fakeRequest = FakeRequest(POST, s"/tasks",
        makerCheckerHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "respond 400 Business unit not found in request headers if business unit in POST /tasks request header missing" in {
      val requestId = UUID.randomUUID()
      val requestHeaderFrom = "George"
      val requestHeaderDate = ZonedDateTime.of(LocalDateTime.of(2019, 1, 1, 0, 0), ZoneOffset.UTC)
      val requestHeaderRoleLevel = 0
      val requestHeaderBu = "Tech Support"

      val expectedInput = TaskToCreate(
        verb = "POST",
        url = "$backoffice_api_host/manual_transactions",
        body = Option(Json.parse("""{"id":1,"account":"0001","direction":"debit","amount":500,"type":"asset"}""").as[JsObject]),
        headers = Json.parse("""{"X-UserName":"George","Date":"2019-01-01T00:00:00Z","X-RoleLevel":1,"X-BusinessUnit":"Support"}""").as[JsObject],
        module = "Manual Settlement",
        action = "create manual settlement").asDomain(
          doneBy = requestHeaderFrom,
          doneAt = requestHeaderDate,
          level = requestHeaderRoleLevel,
          businessUnit = requestHeaderBu).get

      val jsonRequest =
        s"""{
           |  "verb": "${expectedInput.makerRequest.verb.toString}",
           |  "url": "${expectedInput.makerRequest.rawUrl}",
           |  "body": ${expectedInput.makerRequest.body.get},
           |  "headers": ${expectedInput.makerRequest.headers},
           |  "module": "${expectedInput.module}",
           |  "action": "${expectedInput.action}"
           |}""".stripMargin

      val expectedJson =
        s"""{"id":"$requestId",
           |"code":"InvalidRequest",
           |"msg":"${MakerCheckerMgmtController.BusinessUnitMissing}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val makerCheckerHeaders = Headers(
        CONTENT_TYPE → JSON,
        requestIdHeaderKey → requestId.toString,
        requestDateHeaderKey → requestHeaderDate.toString,
        requestFromHeaderKey → requestHeaderFrom,
        requestRoleLevelKey → requestHeaderRoleLevel.toString,
        /*requestHeaderBuKey → requestHeaderBu,*/
        requestHeaderApiKey → apiKeyFromTrustedCaller)

      val fakeRequest = FakeRequest(POST, s"/tasks",
        makerCheckerHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

  }

}
