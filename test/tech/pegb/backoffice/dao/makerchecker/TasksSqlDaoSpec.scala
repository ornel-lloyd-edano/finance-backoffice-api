package tech.pegb.backoffice.dao.makerchecker

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.makerchecker.abstraction.TasksDao
import tech.pegb.backoffice.dao.makerchecker.dto.{MakerCheckerCriteria, TaskToInsert, TaskToUpdate}
import tech.pegb.backoffice.dao.makerchecker.entity.MakerCheckerTask
import tech.pegb.backoffice.dao.makerchecker.sql.TasksSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering, OrderingSet}
import tech.pegb.core.PegBTestApp

class TasksSqlDaoSpec extends PegBTestApp with MockFactory {

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
  val testUuid = UUID.randomUUID()

  override def initSql =
    s"""
       |INSERT INTO ${TasksSqlDao.TableName}
       |(id, uuid, module, action, verb, url, headers, body, status, maker_level,
       |maker_business_unit, created_at, created_by, checked_at, checked_by, reason, updated_at)
       |VALUES
       |('1', '925c8d2f-0501-4b62-8c6f-39d0fbaa48ab', 'accounts management', 'update balance', 'PUT',
       |'$$back_office_hostport/api/accounts/3932ea64-dea5-41d0-ba8b-78de080f94b6', '{"X-UserName":"Analyn"}',
       |'{"balance":999}', 'pending', '2', 'Finance', '2019-11-01 00:00:00', 'Analyn', null, null, null, '2019-11-01 00:00:00');
       |
       |INSERT INTO tasks
       |(id, uuid, module, action, verb, url, headers, body, status, maker_level, maker_business_unit, created_at, created_by, checked_at, checked_by, reason, updated_at)
       |VALUES
       |(2, '06d18f41-1abf-4507-afab-5f8e1c7a1601', 'strings', 'create i18n string', 'POST', '/api/strings', '{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}', '{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}', 'pending', '3', 'BackOffice', '2019-01-01 00:10:30', 'pegbuser', null, null, null, '2019-01-01 00:10:30'),
       |(3, 'e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8', 'strings', 'create i18n string', 'POST', '/api/strings', '{"X-UserName":"pegbuser","X-RoleLevel":"2","content-type":"application/json","X-BusinessUnit":"BackOffice"}', '{"text":"hola","locale":"es","explanation":"text for hello world hahah","key":"hello","platform":"web"}', 'pending', '2', 'BackOffice', '2019-01-02 00:10:30', 'pegbuser', null, null, null, '2019-01-02 00:10:30'),
       |(4, 'ecb907ae-ffaa-45da-abd2-3907fced637f', 'spreads', 'create currency rate spreads', 'POST', '/api/currency_exchanges/fff907ae-ffaa-45da-abd2-3907fced637f/spreads', '{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}', '{"institution":"Mashreq","channel":"atm","transaction_type":"currency_exchange","spread":0.01}', 'pending', '3', 'BackOffice', '2019-01-02 05:10:30', 'pegbuser', null, null, null, '2019-01-02 05:10:30'),
       |(5, '54403083-e0f1-4e80-bdd6-cfdaefd0646e', 'spreads', 'create currency rate spreads', 'POST', '/api/currency_exchanges/aaa907ae-ffaa-45da-abd2-3907fced637f/spreads', '{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}', '{"institution":"NBD","channel":"debit_card","transaction_type":"currency_exchange","spread":0.05}', 'approved', '3', 'BackOffice', '2019-01-02 11:10:30', 'pegbuser', '2019-01-05 15:20:30', 'george', null, '2019-01-05 15:20:30'),
       |(6, '2fb15dd8-97b4-4c19-9886-6b5912ccb4d8', 'strings', 'update i18n string', 'PUT', '/api/strings/bb15dd8-97b4-4c19-9886-cfdaefd0646e', '{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}', '{"updated_at":"2019-07-02T13:10:25.751Z","text":"adios","locale":"es","explanation":"text for bye","key":"bye","platform":"web"}', 'rejected', '3', 'BackOffice', '2019-01-03 15:50:30', 'pegbuser', '2019-01-05 15:50:30', 'george', null, '2019-01-05 15:50:30'),
       |(7, '882baf82-a5c6-47f3-9a9a-14191d14b918', 'strings', 'create i18n string', 'POST', '/api/strings', '{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Marketing"}', '{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}', 'pending', '3', 'Marketing', '2019-01-01 00:15:30', 'pegbuser', null, null, null, '2019-01-01 00:15:30');
    """.stripMargin

  override def cleanupSql: String =
    s"""
       |DELETE FROM tasks;
     """.stripMargin

  val tasksDao = inject[TasksDao]

  val mcAnalyn = MakerCheckerTask(
    id = 1,
    uuid = "925c8d2f-0501-4b62-8c6f-39d0fbaa48ab",
    module = "accounts management",
    action = "update balance",
    verb = "PUT",
    url = "$back_office_hostport/api/accounts/3932ea64-dea5-41d0-ba8b-78de080f94b6",
    headers = """{"X-UserName":"Analyn"}""",
    body = """{"balance":999}""".some,
    status = "pending",
    createdBy = "Analyn",
    createdAt = LocalDateTime.parse("2019-11-01T00:00:00", formatter),
    makerLevel = 2,
    makerBusinessUnit = "Finance",
    checkedBy = None,
    checkedAt = None,
    reason = None,
    updatedAt = LocalDateTime.parse("2019-11-01T00:00:00", formatter).some)

  val mc1 = MakerCheckerTask(
    id = 2,
    uuid = "06d18f41-1abf-4507-afab-5f8e1c7a1601",
    module = "strings",
    action = "create i18n string",
    verb = "POST",
    url = "/api/strings",
    headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}""",
    body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
    status = "pending",
    createdBy = "pegbuser",
    createdAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter),
    makerLevel = 3,
    makerBusinessUnit = "BackOffice",
    checkedBy = None,
    checkedAt = None,
    reason = None,
    updatedAt = LocalDateTime.parse("2019-01-01T00:10:30", formatter).some)
  val mc2 = MakerCheckerTask(
    id = 3,
    uuid = "e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
    module = "strings",
    action = "create i18n string",
    verb = "POST",
    url = "/api/strings",
    headers = """{"X-UserName":"pegbuser","X-RoleLevel":"2","content-type":"application/json","X-BusinessUnit":"BackOffice"}""",
    body = """{"text":"hola","locale":"es","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
    status = "pending",
    createdBy = "pegbuser",
    createdAt = LocalDateTime.parse("2019-01-02T00:10:30", formatter),
    makerLevel = 2,
    makerBusinessUnit = "BackOffice",
    checkedBy = None,
    checkedAt = None,
    reason = None,
    updatedAt = LocalDateTime.parse("2019-01-02T00:10:30", formatter).some)
  val mc3 = MakerCheckerTask(
    id = 4,
    uuid = "ecb907ae-ffaa-45da-abd2-3907fced637f",
    module = "spreads",
    action = "create currency rate spreads",
    verb = "POST",
    url = "/api/currency_exchanges/fff907ae-ffaa-45da-abd2-3907fced637f/spreads",
    headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}""",
    body = """{"institution":"Mashreq","channel":"atm","transaction_type":"currency_exchange","spread":0.01}""".some,
    status = "pending",
    createdBy = "pegbuser",
    createdAt = LocalDateTime.parse("2019-01-02T05:10:30", formatter),
    makerLevel = 3,
    makerBusinessUnit = "BackOffice",
    checkedBy = None,
    checkedAt = None,
    reason = None,
    updatedAt = LocalDateTime.parse("2019-01-02T05:10:30", formatter).some)
  val mc4 = MakerCheckerTask(
    id = 5,
    uuid = "54403083-e0f1-4e80-bdd6-cfdaefd0646e",
    module = "spreads",
    action = "create currency rate spreads",
    verb = "POST",
    url = "/api/currency_exchanges/aaa907ae-ffaa-45da-abd2-3907fced637f/spreads",
    headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}""",
    body = """{"institution":"NBD","channel":"debit_card","transaction_type":"currency_exchange","spread":0.05}""".some,
    status = "approved",
    createdBy = "pegbuser",
    createdAt = LocalDateTime.parse("2019-01-02T11:10:30", formatter),
    makerLevel = 3,
    makerBusinessUnit = "BackOffice",
    checkedBy = "george".some,
    checkedAt = LocalDateTime.parse("2019-01-05T15:20:30", formatter).some,
    reason = None,
    updatedAt = LocalDateTime.parse("2019-01-05T15:20:30", formatter).some)
  val mc5 = MakerCheckerTask(
    id = 6,
    uuid = "2fb15dd8-97b4-4c19-9886-6b5912ccb4d8",
    module = "strings",
    action = "update i18n string",
    verb = "PUT",
    url = "/api/strings/bb15dd8-97b4-4c19-9886-cfdaefd0646e",
    headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"}""",
    body = """{"updated_at":"2019-07-02T13:10:25.751Z","text":"adios","locale":"es","explanation":"text for bye","key":"bye","platform":"web"}""".some,
    status = "rejected",
    createdBy = "pegbuser",
    createdAt = LocalDateTime.parse("2019-01-03T15:50:30", formatter),
    makerLevel = 3,
    makerBusinessUnit = "BackOffice",
    checkedBy = "george".some,
    checkedAt = LocalDateTime.parse("2019-01-05T15:50:30", formatter).some,
    reason = None,
    updatedAt = LocalDateTime.parse("2019-01-05T15:50:30", formatter).some)
  val mc6 = MakerCheckerTask(
    id = 7,
    uuid = "882baf82-a5c6-47f3-9a9a-14191d14b918",
    module = "strings",
    action = "create i18n string",
    verb = "POST",
    url = "/api/strings",
    headers = """{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Marketing"}""",
    body = """{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"}""".some,
    status = "pending",
    createdBy = "pegbuser",
    createdAt = LocalDateTime.parse("2019-01-01T00:15:30", formatter),
    makerLevel = 3,
    makerBusinessUnit = "Marketing",
    checkedBy = None,
    checkedAt = None,
    reason = None,
    updatedAt = LocalDateTime.parse("2019-01-01T00:15:30", formatter).some)

  "TasksSqlDao" should {

    "return some(task) in get by id" in {

      val resp = tasksDao.selectTaskByUUID(mc1.uuid)

      val expected = mc1.some
      resp mustBe Right(expected)
    }

    "return None in get by id if not found" in {

      val resp = tasksDao.selectTaskByUUID(UUID.randomUUID().toString)

      val expected = None
      resp mustBe Right(expected)
    }

    "get all tasks " in {

      val criteria = MakerCheckerCriteria(
        id = None,
        status = None,
        module = None,
        createdAtFrom = None,
        createdAtTo = None,
        makerLevel = None,
        makerBusinessUnit = None)
      val orderingSet = OrderingSet(Ordering("id", Ordering.ASC)).some

      val resp = tasksDao.selectTasksByCriteria(criteria, orderingSet, None, None)

      val expected = Seq(mcAnalyn, mc1, mc2, mc3, mc4, mc5, mc6)
      resp mustBe Right(expected)
    }

    "get all tasks ignore specified order (created_at desc)" in {
      val criteria = MakerCheckerCriteria(
        id = None,
        status = None,
        module = None,
        createdAtFrom = None,
        createdAtTo = None,
        makerLevel = None,
        makerBusinessUnit = None)
      val orderingSet = OrderingSet(Ordering("created_at", Ordering.DESC)).some

      val resp = tasksDao.selectTasksByCriteria(criteria, orderingSet, None, None)

      val expected = Seq(mcAnalyn, mc5, mc4, mc3, mc2, mc6, mc1)
      resp mustBe Right(expected)
    }

    "get all tasks which meets criteria (pending)" in {
      val criteria = MakerCheckerCriteria(
        id = None,
        status = CriteriaField("", "pending").some,
        module = None,
        createdAtFrom = None,
        createdAtTo = None,
        makerLevel = None,
        makerBusinessUnit = None)
      val orderingSet = OrderingSet(Ordering("created_at", Ordering.DESC)).some

      val resp = tasksDao.selectTasksByCriteria(criteria, orderingSet, None, None)

      val expected = Seq(mcAnalyn, mc3, mc2, mc6, mc1)
      resp mustBe Right(expected)

    }

    "get all tasks which meets criteria (level == 3)" in {
      val criteria = MakerCheckerCriteria(
        id = None,
        status = None,
        module = None,
        createdAtFrom = None,
        createdAtTo = None,
        makerLevel = CriteriaField("", 3).some,
        makerBusinessUnit = None)
      val orderingSet = OrderingSet(Ordering("created_at", Ordering.DESC)).some

      val resp = tasksDao.selectTasksByCriteria(criteria, orderingSet, None, None)

      val expected = Seq(mc5, mc4, mc3, mc6, mc1)
      resp mustBe Right(expected)

    }

    "get all tasks which meets criteria (level > 2, businessUnit = BackOffice, 2019-01-02 to 2019-01-03)" in {
      val criteria = MakerCheckerCriteria(
        id = None,
        status = None,
        module = None,
        createdAtFrom = LocalDateTime.parse("2019-01-02T00:00:30", formatter).some,
        createdAtTo = LocalDateTime.parse("2019-01-03T23:59:59", formatter).some,
        makerLevel = CriteriaField("", 2, MatchTypes.GreaterOrEqual).some,
        makerBusinessUnit = CriteriaField("", "BackOffice").some)
      val orderingSet = OrderingSet(Ordering("created_at", Ordering.DESC)).some

      val resp = tasksDao.selectTasksByCriteria(criteria, orderingSet, None, None)

      val expected = Seq(mc5, mc4, mc3, mc2)
      resp mustBe Right(expected)

    }

    "get all tasks which meets criteria (level > 2, businessUnit = BackOffice, 2019-01-02 to 2019-01-03, module = strings)" in {
      val criteria = MakerCheckerCriteria(
        id = None,
        status = None,
        module = CriteriaField("", "str", MatchTypes.Partial).some,
        createdAtFrom = LocalDateTime.parse("2019-01-02T00:00:30", formatter).some,
        createdAtTo = LocalDateTime.parse("2019-01-03T23:59:59", formatter).some,
        makerLevel = CriteriaField("", 2, MatchTypes.GreaterOrEqual).some,
        makerBusinessUnit = CriteriaField("", "BackOffice").some)
      val orderingSet = OrderingSet(Ordering("created_at", Ordering.DESC)).some

      val resp = tasksDao.selectTasksByCriteria(criteria, orderingSet, None, None)

      val expected = Seq(mc5, mc2)
      resp mustBe Right(expected)

    }

    "get a subset of all tasks that meets limit and offset params" in {
      val criteria = MakerCheckerCriteria(
        id = None,
        status = None,
        module = None,
        createdAtFrom = LocalDateTime.parse("2019-01-02T00:00:30", formatter).some,
        createdAtTo = LocalDateTime.parse("2019-01-03T23:59:59", formatter).some,
        makerLevel = CriteriaField("", 2, MatchTypes.GreaterOrEqual).some,
        makerBusinessUnit = CriteriaField("", "BackOffice").some)
      val orderingSet = OrderingSet(Ordering("created_at", Ordering.DESC)).some

      val resp = tasksDao.selectTasksByCriteria(criteria, orderingSet, Some(2), Some(1))

      val expected = Seq(mc4, mc3)
      resp mustBe Right(expected)
    }

    "count all tasks" in {
      val criteria = MakerCheckerCriteria(
        id = None,
        status = None,
        module = None,
        createdAtFrom = None,
        createdAtTo = None,
        makerLevel = None,
        makerBusinessUnit = None)

      val resp = tasksDao.countTasks(criteria)

      val expected = 7
      resp mustBe Right(expected)

    }

    "count all tasks which meets criteria" in {
      val criteria = MakerCheckerCriteria(
        id = None,
        status = None,
        module = CriteriaField("", "str", MatchTypes.Partial).some,
        createdAtFrom = LocalDateTime.parse("2019-01-02T00:00:30", formatter).some,
        createdAtTo = LocalDateTime.parse("2019-01-03T23:59:59", formatter).some,
        makerLevel = CriteriaField("", 2, MatchTypes.GreaterOrEqual).some,
        makerBusinessUnit = CriteriaField("", "BackOffice").some)

      val resp = tasksDao.countTasks(criteria)

      val expected = 2
      resp mustBe Right(expected)
    }

    "save task" in {
      val dto = TaskToInsert(
        uuid = UUID.randomUUID().toString,
        module = "transaction",
        action = "create manual transaction",
        verb = "POST",
        url = "$backoffice_api_root_url/api/manual_transaction",
        headers = """{"X-UserName":"Lloyd","Date":"2018-10-01","From":"Backoffice User"}""",
        body = Option("""{"manual_transaction_request":"some manual txn "}"""),
        status = "pending",
        createdBy = "Backoffice User",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        makerLevel = 1,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None)
      val result: DaoResponse[MakerCheckerTask] = tasksDao.insertTask(dto)

      val expected = MakerCheckerTask(
        id = 8,
        uuid = dto.uuid,
        module = dto.module,
        action = dto.action,
        verb = dto.verb,
        url = dto.url,
        headers = dto.headers,
        body = dto.body,
        status = dto.status,
        createdBy = dto.createdBy,
        createdAt = dto.createdAt,
        makerLevel = dto.makerLevel,
        makerBusinessUnit = dto.makerBusinessUnit,
        checkedBy = dto.checkedBy,
        checkedAt = dto.checkedAt,
        reason = None,
        updatedAt = None)

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "save task with PUT verb" in {
      val body =
        """|{"metadata_id":"types",
           |"key":"communication_channels",
           |"explanation":"List of valid channels that may be used for notifications from the system",
           |"value":[{"id":126,"description":"SMS","name":"sms"},{"id":127,"description":"Email","name":"email"},
           |{"description":"Push","name":"push"}]}
        """.stripMargin.replace("\n", "").trim

      val valueToUpdate =
        """
          |{"metadata_id": "types",
          |"key": "communication_channels",
          |"explanation": "List of valid channels that may be used for notifications from the system",
          |"value": [{"id": 126,
          |"description": "SMS",
          |"name": "sms"
          |},
          |{
          |"id": 127,
          |"description": "Email",
          |"name": "email"
          |}]
          |}
        """.stripMargin.replace("\n", "").trim

      val dto = TaskToInsert(
        uuid = UUID.randomUUID().toString,
        module = "Parameter Management",
        action = "update types",
        verb = "PUT",
        url = "$backoffice_api_host/api/parameters/30303034-3a30-3030-3030-303030303339",
        headers = """{"X-UserName":"Lloyd","Date":"2018-10-01","From":"Backoffice User"}""",
        body = Option(body),
        valueToUpdate = Some(valueToUpdate),
        status = "pending",
        createdBy = "Backoffice User",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        makerLevel = 1,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None)
      val result: DaoResponse[MakerCheckerTask] = tasksDao.insertTask(dto)

      val expected = MakerCheckerTask(
        id = 9,
        uuid = dto.uuid,
        module = dto.module,
        action = dto.action,
        verb = dto.verb,
        url = dto.url,
        headers = dto.headers,
        body = dto.body,
        valueToUpdate = dto.valueToUpdate,
        status = dto.status,
        createdBy = dto.createdBy,
        createdAt = dto.createdAt,
        makerLevel = dto.makerLevel,
        makerBusinessUnit = dto.makerBusinessUnit,
        checkedBy = dto.checkedBy,
        checkedAt = dto.checkedAt,
        reason = None,
        updatedAt = None)

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "fail to save task if value of some fields are too big" in {

      val dto = TaskToInsert(
        uuid = UUID.randomUUID().toString,
        module = "transaction",
        action = "create manual transaction",
        verb = "POST",
        url =
          """$backoffice_api_root_url/api/manual_transaction/czxczxcc/asdasdasdadasdasdadsasd/dfafasdfafafasfafdafs
            |sdfadfafafasdftyirhtgdnshshdfksfskhfskhqwrioqweroqiwurqiurwuwqroqruqowruqoruqworeuqweroqr/sdadsadasdsasd
            |asdasdasdasdadadaswqeqweqweqeqewq/asdasdaklklfsadfalkdalsdalskdfalda/sdaldakdepasldkadsladkacakjsasmaskc
          """.stripMargin,
        headers = """{"X-UserName":"Lloyd","Date":"2018-10-01","From":"Backoffice User"}""",
        body = Option("""{"manual_transaction_request":"some manual txn "}"""),
        status = "pending",
        createdBy = "Backoffice User",
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        makerLevel = 1,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None)
      val result: DaoResponse[MakerCheckerTask] = tasksDao.insertTask(dto)

      result.isLeft mustBe true
      result.left.map(_.message) mustBe Left("Unable to insert task. One or more fields is too big for the defined column size.")
    }

    "update task" in {

      val dto = TaskToUpdate(
        module = Some("transaction"),
        action = Some("create manual transaction"),
        verb = Some("POST"),
        url = Some("$backoffice_api_root_url/api/manual_transaction/".stripMargin),
        headers = None,
        body = None,
        status = Some("reject"),
        createdBy = Some("Backoffice User"),
        createdAt = None,
        makerLevel = Some(1),
        makerBusinessUnit = Some("Finance"),
        checkedBy = None,
        checkedAt = None)

      val expectedResponse = MakerCheckerTask(
        id = 1,
        uuid = "925c8d2f-0501-4b62-8c6f-39d0fbaa48ab",
        module = "transaction",
        action = "create manual transaction",
        verb = "POST",
        url = "$backoffice_api_root_url/api/manual_transaction/",
        headers = """{"X-UserName":"Analyn"}""",
        body = Some("""{"balance":999}"""),
        status = "reject",
        createdBy = "Backoffice User",
        createdAt = LocalDateTime.of(2019, 11, 1, 0, 0),
        makerLevel = 1,
        makerBusinessUnit = "Finance",
        checkedBy = None,
        checkedAt = None,
        reason = None,
        updatedAt = LocalDateTime.of(2019, 11, 1, 0, 0).some)
      val result: DaoResponse[MakerCheckerTask] = tasksDao.updateTask("925c8d2f-0501-4b62-8c6f-39d0fbaa48ab", dto)

      result.isRight mustBe true
      result.right.get mustBe expectedResponse

    }

    "fail to update task if id was not found" in {
      val dto = TaskToUpdate(
        module = Some("transaction"),
        action = Some("create manual transaction"),
        verb = Some("POST"),
        url =
          Some(
            """$backoffice_api_root_url/api/manual_transaction
            """.stripMargin),
        headers = Some("""{"X-UserName":"Lloyd","Date":"2018-10-01","From":"Backoffice User"}"""),
        body = Option("""{"manual_transaction_request":"some manual txn "}"""),
        status = Some("reject"),
        createdBy = Some("Backoffice User"),
        createdAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0)),
        makerLevel = Some(1),
        makerBusinessUnit = Some("Finance"),
        checkedBy = None,
        checkedAt = None)
      val result: DaoResponse[MakerCheckerTask] = tasksDao.updateTask("uuid", dto)

      result.isLeft mustBe true
      result.left.map(_.message) mustBe Left("Task with id uuid was not found")
    }

    "fail to update task if value of some updated fields are too big" in {
      val dto = TaskToUpdate(
        module = Some("transaction"),
        action = Some("create manual transaction"),
        verb = Some("POST"),
        url =
          Some(
            """$backoffice_api_root_url/api/manual_transaction/czxczxcc/asdasdasdadasdasdadsasd/dfafasdfafafasfafdafs
              |sdfadfafafasdftyirhtgdnshshdfksfskhfskhqwrioqweroqiwurqiurwuwqroqruqowruqoruqworeuqweroqr/sdadsadasdsasd
              |asdasdasdasdadadaswqeqweqweqeqewq/asdasdaklklfsadfalkdalsdalskdfalda/sdaldakdepasldkadsladkacakjsasmaskc
            """.stripMargin),
        headers = None,
        body = None,
        status = Some("reject"),
        createdBy = Some("Backoffice User"),
        createdAt = None,
        makerLevel = Some(1),
        makerBusinessUnit = Some("Finance"),
        checkedBy = None,
        checkedAt = None)

      val result: DaoResponse[MakerCheckerTask] = tasksDao.updateTask("925c8d2f-0501-4b62-8c6f-39d0fbaa48ab", dto)

      result.isLeft mustBe true
      result.left.map(_.message) mustBe Left("Unable to update task. One or more fields is too big for the defined column size.")
    }

  }
}
