package tech.pegb.backoffice.domain.report

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.cache.SyncCacheApi
import play.api.inject.{Binding, bind}
import play.api.libs.json.{JsArray, Json}
import tech.pegb.backoffice.application.{CommunicationService, KafkaDBSyncService, MockCommunicationServiceImpl, MockKafkaDbSyncServiceImpl}
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.auth.abstraction.{PermissionDao, ScopeDao}
import tech.pegb.backoffice.dao.auth.dto.ScopeToInsert
import tech.pegb.backoffice.dao.report.abstraction.{ReportDao, ReportDefinitionDao}
import tech.pegb.backoffice.dao.report.entity.{Report, ReportDefinition}
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.mock.InMemorySyncCacheApi
import tech.pegb.backoffice.domain.report.abstraction.{CashFlowReportService, ReportManagement}
import tech.pegb.backoffice.domain.report.dto.{ReportDefinitionCriteria, ReportDefinitionToCreate, ReportDefinitionToUpdate}
import tech.pegb.backoffice.mapping.dao.domain.report.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.report.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class ReportDefinitionMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val reportDao = stub[ReportDao]
  private val reportDefinitionDao = stub[ReportDefinitionDao]
  private val scopeDao = stub[ScopeDao]
  private val permissionDao = stub[PermissionDao]

  //Note: made all bindings from PegBNoDbTestApp and PegBTestApp here because I cant override
  //bind[ReportDefinitionDao].to[MockReportDefinitionDao] in PegBNoDbTestApp
  override def additionalBindings: Seq[Binding[_]] =
    Seq(
      bind[CommunicationService].to[MockCommunicationServiceImpl],
      bind[KafkaDBSyncService].to[MockKafkaDbSyncServiceImpl],
      bind[SyncCacheApi].to[InMemorySyncCacheApi],
      bind[CashFlowReportService].to(stub[CashFlowReportService]), //Note: prevent injection error in unit test
      bind[TypesDao].to(typesDao),
      bind[ReportDao].to(reportDao),
      bind[ReportDefinitionDao].to(reportDefinitionDao),
      bind[ScopeDao].to(scopeDao),
      bind[PermissionDao].to(permissionDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val reportDefinitionManagment = inject[ReportManagement]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  "ReportDefinitionManagement create ReportDefinition" should {
    "return created report definition" in {
      implicit val requestId = UUID.randomUUID()

      val dto = ReportDefinitionToCreate(
        name = "test_single_quotes",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = Json.parse("""[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""").as[JsArray].some,
        parameters = Json.parse("""[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""").as[JsArray].some,
        joins = None,
        grouping = Json.parse("[]").as[JsArray].some,
        ordering = Json.parse("""[{"name":"primaryAccountId","descending":true}]""").as[JsArray].some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = now)

      val expected = ReportDefinition(
        id = UUID.randomUUID().toString,
        name = "test_single_quotes",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = """[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""".some,
        parameters = """[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""".some,
        joins = None,
        grouping = "[]".some,
        ordering = """[{"name":"primaryAccountId","descending":true}]""".some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some)

      (reportDao.executeRawSql _)
        .when(s"${dto.sql} LIMIT 1", dto.parameters.get, Map("type" → ""))
        .returns(Report(
          count = 2L,
          result = Seq(
            Json.parse("""{"ID":"1","DESCRIPTION":"standard account type for individual users","CREATED_AT":"2019-10-09 14:33:56.994","ACCOUNT_TYPE_NAME":"WALLET","CREATED_BY":null,"UPDATED_AT":null,"IS_ACTIVE":"1","UPDATED_BY":null}"""),
            Json.parse("""{"ID":"2","DESCRIPTION":"standard account type for individual users","CREATED_AT":"2019-10-09 14:33:56.994","ACCOUNT_TYPE_NAME":"WALLET1","CREATED_BY":null,"UPDATED_AT":null,"IS_ACTIVE":"1","UPDATED_BY":null}"""))).asRight[DaoError])

      (scopeDao.getScopeIdByName _)
        .when("reporting")
        .returns("d40c2ce7-be7e-11e9-973e-000c297e3e45".some.asRight[DaoError])

      (reportDefinitionDao.createReportDefinition _)
        .when(
          dto.asDao,
          ScopeToInsert(
            parentId = "d40c2ce7-be7e-11e9-973e-000c297e3e45".some,
            name = dto.name,
            description = dto.description.some,
            isActive = 1,
            createdBy = "pegbuser",
            createdAt = now))
        .returns(expected.asRight[DaoError])

      val res = reportDefinitionManagment.createReportDefinition(dto)
      whenReady(res) { actual ⇒
        actual mustBe expected.asDomain.toEither
      }
    }
    "return count in countReportDefinitionByCriteria" in {
      val criteria = ReportDefinitionCriteria()
      (reportDefinitionDao.countReportDefinitionByCriteria _)
        .when(criteria.asDao)
        .returns(10.asRight[DaoError])

      val res = reportDefinitionManagment.countReportDefinitionByCriteria(criteria)

      whenReady(res) { actual ⇒
        actual mustBe Right(10)
      }

    }
    "return report definition list in getReportDefinitionByCriteria" in {
      val criteria = ReportDefinitionCriteria()
      val rd1 = ReportDefinition(
        id = UUID.randomUUID().toString,
        name = "test_single_quotes",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = """[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""".some,
        parameters = """[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""".some,
        joins = None,
        grouping = "[]".some,
        ordering = """[{"name":"primaryAccountId","descending":true}]""".some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some)
      val rd2 = ReportDefinition(
        id = UUID.randomUUID().toString,
        name = "test_single_quotes1",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = """[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""".some,
        parameters = """[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""".some,
        joins = None,
        grouping = "[]".some,
        ordering = """[{"name":"primaryAccountId","descending":true}]""".some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some)

      val expected = Seq(rd1, rd2)

      (reportDefinitionDao.getReportDefinitionByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(expected.asRight[DaoError])

      val res = reportDefinitionManagment.getReportDefinitionByCriteria(criteria, Nil, None, None)

      whenReady(res) { actual ⇒
        actual mustBe Right(expected.flatMap(_.asDomain.toOption))
      }
    }
    "return report definition in getReportDefinitionById" in {
      implicit val requestId = UUID.randomUUID()
      val id = UUID.randomUUID()

      val expected = ReportDefinition(
        id = id.toString,
        name = "test_single_quotes",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = """[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""".some,
        parameters = """[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""".some,
        joins = None,
        grouping = "[]".some,
        ordering = """[{"name":"primaryAccountId","descending":true}]""".some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some)

      (reportDefinitionDao.getReportDefinitionById _)
        .when(id.toString)
        .returns(expected.some.asRight[DaoError])

      val res = reportDefinitionManagment.getReportDefinitionById(id)

      whenReady(res) { actual ⇒
        actual mustBe expected.asDomain.toEither
      }

    }
    "return updated report definition in updateReportDefinition" in {
      implicit val requestId = UUID.randomUUID()
      val id = UUID.randomUUID()

      val dto = ReportDefinitionToUpdate(
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = Json.parse("""[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""").as[JsArray].some,
        parameters = Json.parse("""[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""").as[JsArray].some,
        joins = None,
        grouping = Json.parse("[]").as[JsArray].some,
        ordering = Json.parse("""[{"name":"primaryAccountId","descending":true}]""").as[JsArray].some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        updatedBy = "pegbuser",
        updatedAt = now)

      val expected = ReportDefinition(
        id = id.toString,
        name = "test_single_quotes",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = """[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""".some,
        parameters = """[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""".some,
        joins = None,
        grouping = "[]".some,
        ordering = """[{"name":"primaryAccountId","descending":true}]""".some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some)

      (reportDao.executeRawSql _)
        .when(s"${dto.sql} LIMIT 1", dto.parameters.get, Map("type" → ""))
        .returns(Report(
          count = 2L,
          result = Seq(
            Json.parse("""{"ID":"1","DESCRIPTION":"standard account type for individual users","CREATED_AT":"2019-10-09 14:33:56.994","ACCOUNT_TYPE_NAME":"WALLET","CREATED_BY":null,"UPDATED_AT":null,"IS_ACTIVE":"1","UPDATED_BY":null}"""),
            Json.parse("""{"ID":"2","DESCRIPTION":"standard account type for individual users","CREATED_AT":"2019-10-09 14:33:56.994","ACCOUNT_TYPE_NAME":"WALLET1","CREATED_BY":null,"UPDATED_AT":null,"IS_ACTIVE":"1","UPDATED_BY":null}"""))).asRight[DaoError])

      (scopeDao.getScopeIdByName _)
        .when("reporting")
        .returns("d40c2ce7-be7e-11e9-973e-000c297e3e45".some.asRight[DaoError])

      (reportDefinitionDao.updateReportDefinitionById _)
        .when(
          id.toString,
          dto.asDao)
        .returns(expected.some.asRight[DaoError])

      val res = reportDefinitionManagment.updateReportDefinition(id, dto)
      whenReady(res) { actual ⇒
        actual mustBe expected.asDomain.toEither
      }
    }
    "return true in deleteReportDefinitionById" in {
      implicit val requestId = UUID.randomUUID()
      val id = UUID.randomUUID()

      val expected = ReportDefinition(
        id = id.toString,
        name = "test_single_quotes",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = """[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""".some,
        parameters = """[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""".some,
        joins = None,
        grouping = "[]".some,
        ordering = """[{"name":"primaryAccountId","descending":true}]""".some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some)

      val scopeId = UUID.randomUUID()
      val permissionId = UUID.randomUUID()

      (reportDefinitionDao.getReportDefinitionById _)
        .when(id.toString)
        .returns(expected.some.asRight[DaoError])
      (scopeDao.getScopeIdByName _)
        .when(expected.name)
        .returns(scopeId.toString.some.asRight[DaoError])
      (permissionDao.getPermissionIdsByScopeId _)
        .when(scopeId.toString)
        .returns(Seq(permissionId.toString).asRight[DaoError])
      (reportDefinitionDao.deleteReportDefinitionById _)
        .when(id.toString, scopeId.toString.some, Seq(permissionId.toString))
        .returns(true.asRight[DaoError])

      val res = reportDefinitionManagment.deleteReportDefinitionById(id)

      whenReady(res) { actual ⇒
        actual mustBe Right(true)
      }

    }
  }
}
