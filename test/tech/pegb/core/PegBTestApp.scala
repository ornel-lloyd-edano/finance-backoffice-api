package tech.pegb.core

import java.time._
import java.util.UUID

import net.sf.ehcache.CacheManager
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.cache.SyncCacheApi
import play.api.db.DBApi
import play.api.db.evolutions.EvolutionsReader
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Binding, bind}
import play.api.mvc.Headers
import play.api.test.Helpers._
import play.api.test.Injecting
import play.api.{Application, Configuration, Environment, Mode}
import tech.pegb.backoffice.application.{CommunicationService, KafkaDBSyncService, MockCommunicationServiceImpl, MockKafkaDbSyncServiceImpl}
import tech.pegb.backoffice.domain.mock.InMemorySyncCacheApi

import scala.util.Random

trait PegBTestApp extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll with Injecting {
  this: Suite ⇒

  def additionalBindings: Seq[Binding[_]] = Seq(
    bind[CommunicationService].to[MockCommunicationServiceImpl],
    bind[KafkaDBSyncService].to[MockKafkaDbSyncServiceImpl],
    bind[SyncCacheApi].to[InMemorySyncCacheApi])

  protected lazy val conf: Configuration = inject[Configuration]
  protected lazy val requestDateHeaderKey: String = conf.get[String]("http-header-keys.request-date")
  protected lazy val requestFromHeaderKey: String = conf.get[String]("http-header-keys.request-from")
  protected lazy val requestIdHeaderKey: String = conf.get[String]("http-header-keys.request-id")
  protected lazy val strictDeserializationKey: String = conf.get[String]("http-header-keys.strict-deserialization")
  protected lazy val versionHeaderKey: String = conf.get[String]("http-header-keys.latest-version")
  protected lazy val accessControlExposeHeaders: Seq[String] = conf.get[String]("http-header-keys.access-control-expose-headers").split(",")
  protected lazy val requestRoleLevelKey: String = conf.get[String]("http-header-keys.role-level")
  protected lazy val requestHeaderBuKey: String = conf.get[String]("http-header-keys.business-unit-name")
  protected lazy val requestHeaderApiKey: String = conf.get[String]("http-header-keys.api-key")
  protected lazy val apiKey: String = conf.get[String]("api-keys.backoffice-auth-api")
  protected lazy val backofficePlaceHolder = conf.get[String]("hosts.placeholder.main-backoffice-api")

  protected lazy val mockRequestId: UUID = UUID.randomUUID()
  protected lazy val mockRequestDate: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))
  protected lazy val mockRequestFrom = "pegbuser"
  protected lazy val jsonHeaders = Headers(
    CONTENT_TYPE → JSON,
    requestIdHeaderKey → mockRequestId.toString,
    requestDateHeaderKey → mockRequestDate.toString,
    requestFromHeaderKey → mockRequestFrom)

  protected lazy val jsonHeadersForProxy = Headers(
    requestHeaderApiKey → "some long unmemorizable string of characters",
    requestIdHeaderKey → mockRequestId.toString,
    requestDateHeaderKey → mockRequestDate.toString)

  protected lazy val jsonHeadersForProxyWithContentType = Headers(
    CONTENT_TYPE → JSON,
    requestHeaderApiKey → "some long unmemorizable string of characters",
    requestIdHeaderKey → mockRequestId.toString,
    requestDateHeaderKey → mockRequestDate.toString)

  protected val schemaName: String = "pegb_wallet_db_" + Random.alphanumeric.take(7).mkString
  protected val reportsSchema: String = "pegb_wallet_dwh"

  override def fakeApplication(): Application = {
    val baseConfig = Configuration.load(Environment.simple(mode = Mode.Test))
    val dbUrlConfigKey = "db.backoffice.url"
    val baseDbUrl = baseConfig.get[String](dbUrlConfigKey)
    val dbUrl = baseDbUrl.replace("pegb_wallet_db", schemaName)
    val config = baseConfig ++ Configuration(
      "db.backoffice.url" → dbUrl,
      "play.evolutions.schema" → schemaName,
      "schema" -> reportsSchema)

    val baseBindings = Seq(
      bind[EvolutionsReader].to[TestEvolutionsReader],
      bind[Configuration].toInstance(config))
    new GuiceApplicationBuilder()
      .overrides(baseBindings ++ additionalBindings)
      .build()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    initDb()
  }

  override def afterAll(): Unit = {
    CacheManager.getInstance().shutdown()
  }

  protected def initDb(): Unit = {
    val dbApi = inject[DBApi]
    val db = dbApi.database("backoffice")
    val connection = db.getConnection()
    connection.prepareStatement(prepareSql(cleanupSql)).executeUpdate()
    connection.commit()
    connection.prepareStatement(prepareSql(initSql)).executeUpdate()
    connection.commit()
  }

  protected def initSql: String = ""

  protected def cleanupSql: String = ""

  private def prepareSql(sql: String): String = {
    val prep1 = sql
      .replace(" UNSIGNED", "")
      .replace(" ON UPDATE CURRENT_TIMESTAMP", "")
    val prep2 = """(UNIQUE )(KEY .*)(\(.*\))""".r.replaceAllIn(prep1, "$1$3")
    " ENGINE=(.*)(;)".r.replaceAllIn(prep2, "$2")
  }

}
