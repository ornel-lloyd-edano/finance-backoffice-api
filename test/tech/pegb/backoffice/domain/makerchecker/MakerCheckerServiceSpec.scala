package tech.pegb.backoffice.domain.makerchecker

import java.sql.Connection
import java.time._
import java.time.format.DateTimeFormatter
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import play.api.libs.json.{JsObject, JsValue, Json}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.dao.DaoError.GenericDbError
import tech.pegb.backoffice.dao.makerchecker.abstraction.{GetBackofficeUsersContactsDao, TasksDao}
import tech.pegb.backoffice.dao.makerchecker.entity.{MakerCheckerTask ⇒ DaoMakerCheckerTask}
import tech.pegb.backoffice.domain.{EmailClient, HttpClient, ServiceError}
import tech.pegb.backoffice.domain.makerchecker.abstraction.{MakerCheckerService, RequestCreator}
import tech.pegb.backoffice.domain.makerchecker.dto.{MakerCheckerCriteria, TaskToApprove, TaskToCreate, TaskToReject}
import tech.pegb.backoffice.domain.makerchecker.implementation.MakerCheckerServiceImpl
import tech.pegb.backoffice.domain.makerchecker.model.Statuses._
import tech.pegb.backoffice.domain.makerchecker.model._
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.makerchecker.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.makerchecker.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}
import MakerCheckerServiceImpl._
import tech.pegb.backoffice.dao.{DaoError, i18n}
import tech.pegb.backoffice.dao.i18n.abstraction.I18nStringDao
import tech.pegb.backoffice.dao.i18n.entity.I18nString
import tech.pegb.backoffice.dao.makerchecker.dto.BackofficeUserContact
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.domain.i18n.dto.I18nStringCriteria
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform}
import tech.pegb.backoffice.mapping.domain.dao.i18n.Implicits._

import scala.concurrent.Future

class MakerCheckerServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  val mockTasksDao = stub[TasksDao]
  val mockRequestCreator = stub[RequestCreator]
  val httpClient = stub[HttpClient]
  val mockGetBackofficeUsersContactsDao = stub[GetBackofficeUsersContactsDao]
  val mockI18nStringDao = stub[I18nStringDao]
  val emailClient = stub[EmailClient]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[TasksDao].to(mockTasksDao),
      bind[GetBackofficeUsersContactsDao].to(mockGetBackofficeUsersContactsDao),
      bind[RequestCreator].to(mockRequestCreator),
      bind[HttpClient].to(httpClient),
      bind[EmailClient].to(emailClient),
      bind[I18nStringDao].to(mockI18nStringDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val config = inject[AppConfig]
  val makerCheckerService = inject[MakerCheckerService]

  "MakerCheckerService" should {

    "create a task" in {
      val mockRequestId = UUID.randomUUID()
      val dto = TaskToCreate(
        maker = MakerDetails(
          createdBy = "George",
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          level = RoleLevels.DepartmentHead,
          businessUnit = "Technical Support"),
        makerRequest = MakerRequest(
          verb = "POST",
          url = "$backoffice_api_host/api/accounts",
          queryParams = None,
          body = Option(Json.parse("""{"account_number":"1.0001","user_id":10,"currency":"KES","balance":100,"type":"standard_wallet"}""").as[JsObject]),
          headers = Json.parse("""{"X-UserName":"George","X-Role-Level":1,"X-Business-Unit":"Technical Support"}""").as[JsObject]),
        module = "Accounts Management",
        action = "Create new account")

      val mockDaoResult = DaoMakerCheckerTask(
        id = 1,
        uuid = mockRequestId.toString,
        module = dto.module,
        action = dto.action,
        verb = dto.makerRequest.verb.toString,
        url = dto.makerRequest.rawUrl.toString,
        headers = dto.makerRequest.headers.toString(),
        body = dto.makerRequest.body.map(_.toString()),
        status = MakerCheckerTask.statusOnCreate,
        createdBy = dto.maker.createdBy,
        createdAt = dto.maker.createdAt,
        makerLevel = dto.maker.level.underlying,
        makerBusinessUnit = dto.maker.businessUnit,
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      val expectedInput = dto.asDao(mockRequestId, MakerCheckerTask.statusOnCreate, none)

      (mockTasksDao.insertTask _)
        .when(expectedInput)
        .returns(Right(mockDaoResult))

      val mockBackofficeUserContacts = Seq(
        BackofficeUserContact(backofficeUserId = UUID.randomUUID().toString, "loyd@pegb.tech", "0544451679"),
        BackofficeUserContact(backofficeUserId = UUID.randomUUID().toString, "ornel@pegb.tech", "0544451678"),
        BackofficeUserContact(backofficeUserId = UUID.randomUUID().toString, "edano@pegb.tech", "0544451677"))

      (mockI18nStringDao.getStringByCriteria(
        _: i18n.dto.I18nStringCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection]))
        .when(I18nStringCriteria(
          key = I18nKey("task_notification_subject").some,
          platform = I18nPlatform("web").some,
          locale = I18nLocale("en").some).asDao, None, None, None, None)
        .returns(
          Seq(I18nString(
            id = 1,
            key = "task_notification_subject",
            text = "A task is waiting for approval",
            locale = "en",
            platform = "web",
            `type` = None,
            explanation = None,
            createdAt = now,
            updatedAt = now.some)).asRight[DaoError])

      (mockI18nStringDao.getStringByCriteria(
        _: i18n.dto.I18nStringCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection]))
        .when(I18nStringCriteria(
          key = I18nKey("task_notification_body").some,
          platform = I18nPlatform("web").some,
          locale = I18nLocale("en").some).asDao, None, None, None, None)
        .returns(
          Seq(I18nString(
            id = 1,
            key = "task_notification_body",
            text = "message: {{ action }} {{ module }} ",
            locale = "en",
            platform = "web",
            `type` = None,
            explanation = None,
            createdAt = now,
            updatedAt = now.some)).asRight[DaoError])

      (mockGetBackofficeUsersContactsDao.getBackofficeUsersContactsByRoleLvlAndBusinessUnit _)
        .when(dto.maker.level.underlying, dto.maker.businessUnit)
        .returns(Right(mockBackofficeUserContacts))

      (emailClient.sendEmail _).when(
        mockBackofficeUserContacts.map(_.email), "task_notification_body",
        s"message: ${mockDaoResult.action} ${mockDaoResult.module}").returns(Right(()))

      val expected = MakerCheckerTask(
        id = UUID.fromString(mockDaoResult.uuid),
        module = dto.module,
        actionRequired = dto.action,
        maker = dto.maker,
        makerRequest = dto.makerRequest,
        status = Statuses.Pending,
        reason = None,
        checker = None,
        updatedAt = None,
        change = dto.makerRequest.body.map(_.toString().asJsNode),
        current = None)

      val result = makerCheckerService.createPendingTask(dto, mockRequestId)

      whenReady(result) {
        maybeCreatedPendingTask ⇒
          maybeCreatedPendingTask.isRight mustBe true
          maybeCreatedPendingTask.right.get mustBe expected
      }
    }

    "create a task for PUT verb" in {
      val mockRequestId = UUID.randomUUID()
      val body =
        s"""
           |{
           |		"metadata_id": "types",
           |		"key": "communication_channels",
           |		"explanation": "List of valid channels that may be used for notifications from the system",
           |		"value": [{
           |			"id": 126,
           |			"description": "SMS",
           |			"name": "sms"
           |		}, {
           |			"id": 127,
           |			"description": "Email",
           |			"name": "email"
           |		}, {
           |			"description": "Push",
           |			"name": "push"
           |		}]
           |	}
         """.stripMargin.trim
      val originalValue =
        s"""
           |{
           |		"metadata_id": "types",
           |		"key": "communication_channels",
           |		"explanation": "List of valid channels that may be used for notifications from the system",
           |		"value": [{
           |			"id": 126,
           |			"description": "SMS",
           |			"name": "sms"
           |		}, {
           |			"id": 127,
           |			"description": "Email",
           |			"name": "email"
           |		}]
           |	}
         """.stripMargin.trim

      val dto = TaskToCreate(
        maker = MakerDetails(
          createdBy = "George",
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          level = RoleLevels.DepartmentHead,
          businessUnit = "Technical Support"),
        makerRequest = MakerRequest(
          verb = "PUT",
          url = "$backoffice_api_host/api/parameters/30303034-3a30-3030-3030-303030303339",
          queryParams = None,
          body = Option(Json.parse(body).as[JsObject]),
          headers = Json.parse("""{"X-UserName":"George","X-Role-Level":1,"X-Business-Unit":"Technical Support"}""").as[JsObject]),
        module = "Parameter Management",
        action = "update types")

      val mockDaoResult = DaoMakerCheckerTask(
        id = 1,
        uuid = mockRequestId.toString,
        module = dto.module,
        action = dto.action,
        verb = dto.makerRequest.verb.toString,
        url = dto.makerRequest.rawUrl.toString,
        headers = dto.makerRequest.headers.toString(),
        body = dto.makerRequest.body.map(_.toString()),
        valueToUpdate = originalValue.some,
        status = MakerCheckerTask.statusOnCreate,
        createdBy = dto.maker.createdBy,
        createdAt = dto.maker.createdAt,
        makerLevel = dto.maker.level.underlying,
        makerBusinessUnit = dto.maker.businessUnit,
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)
      val response = HttpResponse(success = true, statusCode = 200, body = originalValue.some)

      val expectedInput = dto.asDao(mockRequestId, MakerCheckerTask.statusOnCreate, originalValue.some)

      (mockTasksDao.insertTask _)
        .when(expectedInput)
        .returns(Right(mockDaoResult))

      (httpClient.request(_: String, _: String, _: Map[String, String], _: Map[String, String], _: Option[JsValue], _: UUID))
        .when("GET", "http://localhost:9000/api/parameters/30303034-3a30-3030-3030-303030303339", Map[String, String](), Map[String, String](), None, mockRequestId)
        .returns(Future.successful(response))

      val mockBackofficeUserContacts = Seq(
        BackofficeUserContact(backofficeUserId = UUID.randomUUID().toString, "loyd@pegb.tech", "0544451679"),
        BackofficeUserContact(backofficeUserId = UUID.randomUUID().toString, "ornel@pegb.tech", "0544451678"),
        BackofficeUserContact(backofficeUserId = UUID.randomUUID().toString, "edano@pegb.tech", "0544451677"))

      (mockGetBackofficeUsersContactsDao.getBackofficeUsersContactsByRoleLvlAndBusinessUnit _)
        .when(dto.maker.level.underlying, dto.maker.businessUnit)
        .returns(Right(mockBackofficeUserContacts))

      (mockI18nStringDao.getStringByCriteria(
        _: i18n.dto.I18nStringCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection]))
        .when(I18nStringCriteria(
          key = I18nKey("task_notification_subject").some,
          platform = I18nPlatform("web").some,
          locale = I18nLocale("en").some).asDao, None, None, None, None)
        .returns(
          Nil.asRight[DaoError])

      (mockI18nStringDao.getStringByCriteria(
        _: i18n.dto.I18nStringCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection]))
        .when(I18nStringCriteria(
          key = I18nKey("task_notification_body").some,
          platform = I18nPlatform("web").some,
          locale = I18nLocale("en").some).asDao, None, None, None, None)
        .returns(
          Nil.asRight[DaoError])

      (emailClient.sendEmail _).when(
        mockBackofficeUserContacts.map(_.email), "A task is waiting for approval",
        s"""The following request is awaiting your approval: ${mockDaoResult.action} ${mockDaoResult.module}""".stripMargin).returns(Right(()))

      val expected = MakerCheckerTask(
        id = UUID.fromString(mockDaoResult.uuid),
        module = dto.module,
        actionRequired = dto.action,
        maker = dto.maker,
        makerRequest = dto.makerRequest,
        status = Statuses.Pending,
        reason = None,
        checker = None,
        updatedAt = None,
        change = dto.makerRequest.body.map(_.toString().asJsNode),
        original = originalValue.asJsNode.some)

      val result = makerCheckerService.createPendingTask(dto, mockRequestId)

      whenReady(result) {
        maybeCreatedPendingTask ⇒
          maybeCreatedPendingTask.isRight mustBe true
          maybeCreatedPendingTask.right.get mustBe expected
      }
    }

    "return Right(Seq(MakerCheckerTask)) in getTasksByCriteria" in {
      implicit val requestId = UUID.randomUUID()

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 3,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)
      val mc2 = DaoMakerCheckerTask(
        id = 2,
        uuid = "e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"2","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hola","locale":"es","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-02T00:10:30", formatter),
        makerLevel = 2,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      val makerCheckerCriteria = MakerCheckerCriteria(
        id = UUIDLike(UUID.randomUUID().toString).some,
        module = "spreads".some,
        status = "pending".asDomain.some,
        createdAtFrom = LocalDateTime.parse("2019-01-01T00:00:00", formatter).some,
        createdAtTo = LocalDateTime.parse("2019-01-02T23:59:59", formatter).some,
        partialMatchFields = Set.empty)

      val ordering = Seq(Ordering("created_at", Ordering.DESCENDING))

      val requesterLevel = RoleLevels(2)
      val requesterBusinessUnit = "Finance"
      //val requesterLevelCriteria = CriteriaField(TasksSqlDao.cMakerLevel, requesterLevel, MatchTypes.GreaterOrEqual)
      //val requesterBusinessUnitCriteria = CriteriaField(TasksSqlDao.cMakerBu, requesterBusinessUnit)

      (mockTasksDao.selectTasksByCriteria _).when(
        makerCheckerCriteria.asDao(requesterLevel, Some(requesterBusinessUnit)),
        ordering.asDao,
        None,
        None).returns(Right(Seq(mc1, mc2)))

      val result = makerCheckerService.getTasksByCriteria(makerCheckerCriteria, requesterLevel, requesterBusinessUnit, ordering, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(mc1, mc2).map(_.asDomain().get))
      }
    }

    "return Right(Seq(MakerCheckerTask)) in getTasksByCriteria [CEO LEVEL]" in {
      implicit val requestId = UUID.randomUUID()

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 3,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)
      val mc2 = DaoMakerCheckerTask(
        id = 2,
        uuid = "e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"2","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hola","locale":"es","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-02T00:10:30", formatter),
        makerLevel = 2,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      val makerCheckerCriteria = MakerCheckerCriteria(
        id = UUIDLike(UUID.randomUUID().toString).some,
        module = "spreads".some,
        status = "pending".asDomain.some,
        createdAtFrom = LocalDateTime.parse("2019-01-01T00:00:00", formatter).some,
        createdAtTo = LocalDateTime.parse("2019-01-02T23:59:59", formatter).some,
        partialMatchFields = Set.empty)

      val ordering = Seq(Ordering("created_at", Ordering.DESCENDING))

      val requesterLevel = RoleLevels.CEO
      val requesterBusinessUnit = "Finance"
      //val requesterLevelCriteria = CriteriaField(TasksSqlDao.cMakerLevel, requesterLevel, MatchTypes.GreaterOrEqual)
      //val requesterBusinessUnitCriteria = CriteriaField(TasksSqlDao.cMakerBu, requesterBusinessUnit)

      (mockTasksDao.selectTasksByCriteria _).when(
        makerCheckerCriteria.asDao(requesterLevel, None),
        ordering.asDao,
        None,
        None).returns(Right(Seq(mc1, mc2)))

      val result = makerCheckerService.getTasksByCriteria(makerCheckerCriteria, requesterLevel, requesterBusinessUnit, ordering, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(mc1, mc2).map(_.asDomain().get))
      }
    }

    "return count by criteria" in {
      val makerCheckerCriteria = MakerCheckerCriteria(
        id = UUIDLike(UUID.randomUUID().toString).some,
        module = "spreads".some,
        status = "pending".asDomain.some,
        createdAtFrom = LocalDateTime.parse("2019-01-01T00:00:00", formatter).some,
        createdAtTo = LocalDateTime.parse("2019-01-02T23:59:59", formatter).some,
        partialMatchFields = Set.empty)

      val requesterLevel = RoleLevels(2)
      val requesterBusinessUnit = "Finance"
      //val requesterLevelCriteria = CriteriaField(TasksSqlDao.cMakerLevel, requesterLevel, MatchTypes.GreaterOrEqual)
      //val requesterBusinessUnitCriteria = CriteriaField(TasksSqlDao.cMakerBu, requesterBusinessUnit)

      (mockTasksDao.countTasks _).when(makerCheckerCriteria.asDao(requesterLevel, Some(requesterBusinessUnit)))
        .returns(Right(2))

      val result = makerCheckerService.countTasksByCriteria(makerCheckerCriteria, requesterLevel, requesterBusinessUnit)

      whenReady(result) { actual ⇒
        actual mustBe Right(2)
      }
    }

    "approve a task" in {
      implicit val randomUuid = UUID.randomUUID()

      val taskToApprove = TaskToApprove(
        id = randomUuid,
        maybeReason = "because it's a valid task".some, //possible justification why not rejected
        approvedBy = "Ujali",
        approvedAt = LocalDateTime.of(2019, 1, 11, 1, 1),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 3,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(mc1.some))
      (mockRequestCreator.createRequest _)
        .when(mc1.asDomain(None).get, "$backoffice_api_host", "http://localhost:9000")
        .returns(Future.successful(Right(())))
      (mockTasksDao.updateTask _)
        .when(randomUuid.toString, taskToApprove.asDao)
        .returns(Right(mc1.copy(
          status = "approved",
          checkedBy = taskToApprove.approvedBy.some,
          checkedAt = taskToApprove.approvedAt.some,
          reason = taskToApprove.maybeReason)))

      val resultF = makerCheckerService.approvePendingTask(taskToApprove)
      whenReady(resultF) { result ⇒
        result mustBe Right(mc1.copy(
          status = "approved",
          checkedBy = taskToApprove.approvedBy.some,
          checkedAt = taskToApprove.approvedAt.some,
          reason = taskToApprove.maybeReason).asDomain().get)
      }
    }

    "return task in getTaskById" in {
      implicit val requestId: UUID = UUID.randomUUID()

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 3,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      (mockTasksDao.selectTaskByUUID _).when("06d18f41-1abf-4507-afab-5f8e1c7a1601")
        .returns(Right(mc1.some))

      val res = makerCheckerService.getTaskById(UUID.fromString("06d18f41-1abf-4507-afab-5f8e1c7a1601"))
      whenReady(res) { actual ⇒
        actual mustBe Right(mc1.asDomain().get)
      }
    }

    "return error in getStringById when dao returns None" in {
      implicit val requestId = UUID.randomUUID()

      (mockTasksDao.selectTaskByUUID _).when("eeee8f41-1abf-4507-afab-5f8e1c7a1601")
        .returns(Right(None))

      val res = makerCheckerService.getTaskById(UUID.fromString("eeee8f41-1abf-4507-afab-5f8e1c7a1601"))
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.notFoundError(s"Task id [eeee8f41-1abf-4507-afab-5f8e1c7a1601] not found", Some(requestId)))
      }
    }

    "approve a task regardless of business_unit when level is 0" in {
      implicit val randomUuid = UUID.randomUUID()

      val taskToApprove = TaskToApprove(
        id = randomUuid,
        maybeReason = "because it's a valid task".some, //possible justification why not rejected
        approvedBy = "Ujali",
        approvedAt = LocalDateTime.of(2019, 1, 11, 1, 1),
        checkerLevel = RoleLevels.apply(0),
        checkerBusinessUnit = "CA")

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 0,
        makerBusinessUnit = "OTHER_CA",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(mc1.some))
      (mockRequestCreator.createRequest _)
        .when(mc1.asDomain(None).get, "$backoffice_api_host", "http://localhost:9000")
        .returns(Future.successful(Right(())))
      (mockTasksDao.updateTask _)
        .when(randomUuid.toString, taskToApprove.asDao)
        .returns(Right(mc1.copy(
          status = "approved",
          checkedBy = taskToApprove.approvedBy.some,
          checkedAt = taskToApprove.approvedAt.some,
          reason = taskToApprove.maybeReason)))

      val resultF = makerCheckerService.approvePendingTask(taskToApprove)
      whenReady(resultF) { result ⇒
        result mustBe Right(mc1.copy(
          status = "approved",
          checkedBy = taskToApprove.approvedBy.some,
          checkedAt = taskToApprove.approvedAt.some,
          reason = taskToApprove.maybeReason).asDomain().get)
      }
    }

    "approve a task on same level and bu when level is 1" in {
      implicit val randomUuid = UUID.randomUUID()

      val taskToApprove = TaskToApprove(
        id = randomUuid,
        maybeReason = "because it's a valid task".some, //possible justification why not rejected
        approvedBy = "Ujali",
        approvedAt = LocalDateTime.of(2019, 1, 11, 1, 1),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 1,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(mc1.some))
      (mockRequestCreator.createRequest _)
        .when(mc1.asDomain(None).get, "$backoffice_api_host", "http://localhost:9000")
        .returns(Future.successful(Right(())))
      (mockTasksDao.updateTask _)
        .when(randomUuid.toString, taskToApprove.asDao)
        .returns(Right(mc1.copy(
          status = "approved",
          checkedBy = taskToApprove.approvedBy.some,
          checkedAt = taskToApprove.approvedAt.some,
          reason = taskToApprove.maybeReason)))

      val resultF = makerCheckerService.approvePendingTask(taskToApprove)
      whenReady(resultF) { result ⇒
        result mustBe Right(mc1.copy(
          status = "approved",
          checkedBy = taskToApprove.approvedBy.some,
          checkedAt = taskToApprove.approvedAt.some,
          reason = taskToApprove.maybeReason).asDomain().get)
      }
    }

    "fail to approve a task if no task id found " in {
      implicit val randomUuid = UUID.randomUUID()

      val taskToApprove = TaskToApprove(
        id = randomUuid,
        maybeReason = "because it's a valid task".some, //possible justification why not rejected
        approvedBy = "Ujali",
        approvedAt = LocalDateTime.of(2019, 1, 11, 1, 1),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      val expectedResponse = Left(s"Task with id $randomUuid not found")
      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(none))
      val resultF = makerCheckerService.approvePendingTask(taskToApprove)

      whenReady(resultF)(result ⇒ result.contains(expectedResponse))
    }

    "fail to approve a task due to error in Dao layer" in {
      implicit val randomUuid = UUID.randomUUID()

      val taskToApprove = TaskToApprove(
        id = randomUuid,
        maybeReason = "because it's a valid task".some, //possible justification why not rejected
        approvedBy = "Ujali",
        approvedAt = LocalDateTime.of(2019, 1, 11, 1, 1),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      val expectedResponse = Left(s"Task with id $randomUuid not found")
      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Left(GenericDbError(s"Failed to fetch Task $randomUuid")))
      val resultF = makerCheckerService.approvePendingTask(taskToApprove)

      whenReady(resultF)(result ⇒ result contains expectedResponse)
    }

    "fail to approve a task due to error in request layer" in {
      implicit val randomUuid = UUID.randomUUID()

      val taskToApprove = TaskToApprove(
        id = randomUuid,
        maybeReason = "because it's a valid task".some, //possible justification why not rejected
        approvedBy = "Ujali",
        approvedAt = LocalDateTime.of(2019, 1, 11, 1, 1),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")
      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 3,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      val expectedResponse = Left(ServiceError.dtoMappingError(s"received response 400 with message", Some(randomUuid)))
      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(mc1.some))
      (mockRequestCreator.createRequest _)
        .when(mc1.asDomain(None).get, "$backoffice_api_host", "http://localhost:9000")
        .returns(Future.successful(Left(ServiceError.dtoMappingError(s"received response 400 with message", Some(randomUuid)))))
      val resultF = makerCheckerService.approvePendingTask(taskToApprove)

      whenReady(resultF)(result ⇒ result contains expectedResponse)
    }

    "reject a task" in {
      implicit val randomUuid = UUID.randomUUID()

      val taskToReject = TaskToReject(
        id = randomUuid,
        rejectionReason = "test reason",
        rejectedBy = "Ujali",
        rejectedAt = LocalDateTime.of(2019, 1, 11, 1, 1),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 3,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = None)

      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(mc1.some))
      (mockRequestCreator.createRequest _)
        .when(mc1.asDomain(none).get, "$backoffice_api_host", "http://localhost:9000")
        .returns(Future.successful(Right(())))
      (mockTasksDao.updateTask _)
        .when(randomUuid.toString, taskToReject.asDao)
        .returns(Right(mc1.copy(
          status = "rejected",
          checkedBy = taskToReject.rejectedBy.some,
          checkedAt = taskToReject.rejectedAt.some,
          reason = taskToReject.rejectionReason.some)))

      val resultF = makerCheckerService.rejectPendingTask(taskToReject)
      whenReady(resultF) { result ⇒
        result mustBe Right(mc1.copy(
          status = "rejected  ",
          checkedBy = taskToReject.rejectedBy.some,
          checkedAt = taskToReject.rejectedAt.some,
          reason = taskToReject.rejectionReason.some).asDomain().get)
      }
    }

    "fail to reject a task if task id not found" in {
      implicit val randomUuid = UUID.randomUUID()

      val taskToReject = TaskToReject(
        id = randomUuid,
        rejectionReason = "test reason",
        rejectedBy = "Ujali",
        rejectedAt = LocalDateTime.of(2019, 1, 11, 1, 1),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      val expectedResponse = Left(s"Task with id $randomUuid not found")
      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(none))
      val resultF = makerCheckerService.rejectPendingTask(taskToReject)

      whenReady(resultF)(result ⇒ result.contains(expectedResponse))

    }

    "fail to reject a task due to error in Dao layer" in {

      implicit val randomUuid = UUID.randomUUID()

      val taskToReject = TaskToReject(
        id = randomUuid,
        rejectionReason = "test reason",
        rejectedBy = "Ujali",
        rejectedAt = LocalDateTime.of(2019, 1, 11, 1, 1),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      val expectedResponse = Left(s"Task with id $randomUuid not found")
      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Left(GenericDbError(s"Failed to fetch Task $randomUuid")))
      val resultF = makerCheckerService.rejectPendingTask(taskToReject)

      whenReady(resultF)(result ⇒ result contains expectedResponse)
    }

    "validate correct MakerCheckerTask for approve/reject" in {

      implicit val randomUuid = UUID.randomUUID()

      val taskToReject = TaskToReject(
        id = randomUuid,
        rejectionReason = "test reason",
        rejectedBy = "Ujali",
        rejectedAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = randomUuid.toString,
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 3,
        makerBusinessUnit = "Finance",
        checkedBy = none,
        checkedAt = none,
        reason = none,
        updatedAt = None)

      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(mc1.some))
      val result = makerCheckerService.validateCheckerProcess(randomUuid.toString, taskToReject.rejectedBy, taskToReject.checkerLevel, taskToReject.checkerBusinessUnit, "reject")

      result mustBe Right(mc1.asDomain(none).get)
    }

    "validate MakerCheckerTask for approve/reject if task id not found" in {

      implicit val randomUuid: UUID = UUID.randomUUID()

      val taskToReject = TaskToReject(
        id = randomUuid,
        rejectionReason = "test reason",
        rejectedBy = "Ujali",
        rejectedAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(none))
      val result = makerCheckerService.validateCheckerProcess(randomUuid.toString, taskToReject.rejectedBy, taskToReject.checkerLevel, taskToReject.checkerBusinessUnit, "reject")

      result.left.map(_.message) mustBe notFoundTaskId(randomUuid.toString).left.map(_.message)
    }

    "validate MakerCheckerTask for approve/reject if status is not pending" in {

      implicit val randomUuid = UUID.randomUUID()

      val taskToReject = TaskToReject(
        id = randomUuid,
        rejectionReason = "test reason",
        rejectedBy = "Ujali",
        rejectedAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = randomUuid.toString,
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 3,
        makerBusinessUnit = "Finance",
        checkedBy = none,
        checkedAt = none,
        reason = none,
        updatedAt = None)

      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(mc1.copy(
          status = "rejected",
          checkedBy = taskToReject.rejectedBy.some,
          checkedAt = taskToReject.rejectedAt.some,
          reason = taskToReject.rejectionReason.some).some))
      val result = makerCheckerService.validateCheckerProcess(randomUuid.toString, taskToReject.rejectedBy, taskToReject.checkerLevel, taskToReject.checkerBusinessUnit, "reject")

      result.left.map(_.message) mustBe validationErrors("reject", taskToReject.id.toString, NotPending).left.map(_.message)
    }

    "validate MakerCheckerTask for approve/reject if checker is same as maker" in {

      implicit val randomUuid = UUID.randomUUID()

      val taskToReject = TaskToReject(
        id = randomUuid,
        rejectionReason = "test reason",
        rejectedBy = "pegbuser",
        rejectedAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        checkerLevel = RoleLevels.apply(0),
        checkerBusinessUnit = "Finance")

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = randomUuid.toString,
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 0,
        makerBusinessUnit = "Finance",
        checkedBy = none,
        checkedAt = none,
        reason = none,
        updatedAt = None)

      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(mc1.some))
      val result = makerCheckerService.validateCheckerProcess(randomUuid.toString, taskToReject.rejectedBy, taskToReject.checkerLevel, taskToReject.checkerBusinessUnit, "reject")

      result.left.map(_.message) mustBe validationErrors("reject", taskToReject.id.toString, OwnTaskChecker).left.map(_.message)
    }

    "validate MakerCheckerTask for approve/reject if role level of checker is lower than maker's role level" in {

      implicit val randomUuid = UUID.randomUUID()

      val taskToReject = TaskToReject(
        id = randomUuid,
        rejectionReason = "test reason",
        rejectedBy = "Ujali",
        rejectedAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "Finance")

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = randomUuid.toString,
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 0,
        makerBusinessUnit = "Finance",
        checkedBy = none,
        checkedAt = none,
        reason = none,
        updatedAt = None)

      (mockTasksDao.selectTaskByUUID _).when(randomUuid.toString)
        .returns(Right(mc1.some))
      val result = makerCheckerService.validateCheckerProcess(randomUuid.toString, taskToReject.rejectedBy, taskToReject.checkerLevel, taskToReject.checkerBusinessUnit, "reject")

      result.left.map(_.message) mustBe validationErrors("reject", taskToReject.id.toString, LowerLevel).left.map(_.message)
    }

    "validate MakerCheckerTask for approve/reject if checker is from different dept than maker" in {

      implicit val resquestId = UUID.randomUUID()

      val taskToReject = TaskToReject(
        id = resquestId,
        rejectionReason = "test reason",
        rejectedBy = "Ujali",
        rejectedAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        checkerLevel = RoleLevels.apply(1),
        checkerBusinessUnit = "marketing")

      val mc1 = DaoMakerCheckerTask(
        id = 1,
        uuid = resquestId.toString,
        module = "strings",
        action = "create i18n string",
        verb = "POST",
        url = "/api/strings",
        headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Finance"}""",
        body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
        status = "pending",
        createdBy = "pegbuser",
        createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
        makerLevel = 1,
        makerBusinessUnit = "Finance",
        checkedBy = none,
        checkedAt = none,
        reason = none,
        updatedAt = None)

      (mockTasksDao.selectTaskByUUID _).when(resquestId.toString)
        .returns(Right(mc1.some))
      val result = makerCheckerService.validateCheckerProcess(resquestId.toString, taskToReject.rejectedBy, taskToReject.checkerLevel, taskToReject.checkerBusinessUnit, "reject")

      result.left.map(_.message) mustBe validationErrors("reject", taskToReject.id.toString, DiffBusinessUnit).left.map(_.message)
    }
  }

}
