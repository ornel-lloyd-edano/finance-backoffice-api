package tech.pegb.backoffice

import java.sql.Connection
import java.util.UUID

import anorm._
import cats.implicits._
import play.api.db.{DBApi, Database}
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{AnyContentAsJson, Headers}
import play.api.test.FakeRequest
import tech.pegb.backoffice.util.Logging
import tech.pegb.core.PegBTestAppWithServer

import scala.io.Source
import scala.util.Try

trait PlayIntegrationTest extends PegBTestAppWithServer with SuperAdminCreator with Logging {

  override lazy val port: Int = 4444

  protected val initSchemaSqlName = "init-schema.sql"
  protected val initDataSqlName = "init-data.sql"
  protected val dropSchemaSqlName = "drop-schema.sql"
  //protected val saUserName = "superadmin"
  protected var defaultUserId: Int = _
  protected var defaultUserUuid: UUID = _
  protected var defaultIndividualUserId: Int = _
  //protected var defaultBUId: UUID = _
  protected val defaultCreatedBy = "it"
  //protected val notificationsBuffer: ArrayBuffer[Notification] = mutable.ArrayBuffer.empty[Notification]

  val endpoint: String = "/api"
  val mayBeDbName: Option[String] = None

  override def jsonHeaders: Headers = super.jsonHeaders.add(AuthHeader)

  def executeCleanUp(db: Database): Unit = db.withTransaction { implicit conn ⇒
    cleanupSql.split(";").filterNot(_.trim.isEmpty).foreach { statement ⇒
      SQL(statement).execute()
    }
  }

  override def initDb(): Unit = ()

  override def beforeAll(): Unit = {
    super.beforeAll()
    val dbApi: DBApi = inject[DBApi]

    logger.info(s"Databases are:\n${dbApi.databases().map(db ⇒ s"${db.name}: ${db.url}").mkString("\n")}")
    
    runForGreenPlum(dbApi)
    runForMySql(dbApi)

  }


  private def runForMySql(dbApi: DBApi) = {
    (for {
      rawDropSql ← Try(Source.fromResource(dropSchemaSqlName).mkString).toEither.leftMap(_.getMessage)
      rawDataSql ← Try(Source.fromResource(initDataSqlName).mkString).toEither.leftMap(_.getMessage)
      db ← dbApi.database("backoffice").asRight[String]
      _ ← db.withTransaction { implicit connection ⇒
        for {
          _ ← runUpdateSql(rawDropSql)
          _ ← runUpdateSql(rawDataSql)
          _ ← runUpdateSql(initSql)
        } yield {
          defaultUserId = SQL("SELECT id FROM users WHERE username = '+971589721075' LIMIT 1;")
            .as(SqlParser.scalar[Int].single)
          defaultUserUuid = SQL("SELECT uuid FROM users WHERE id = {id} LIMIT 1;")
            .on("id" → defaultUserId)
            .as(SqlParser.scalar[UUID].single)
          defaultIndividualUserId = SQL("SELECT id FROM individual_users WHERE user_id = {user_id} LIMIT 1;")
            .on("user_id" → defaultUserId)
            .as(SqlParser.scalar[Int].single)
        }
      }
    } yield ())
      .leftMap(err ⇒ logger.error("Failed to prepare db: " + err))
  }


  private def runForGreenPlum(dbApi: DBApi) = {
    val schemaName: String = "pegb_wallet_dwh_it"
    val initDataSqlName: String = "init-data-green-plum.sql"
    val dropSchemaSqlName: String = "drop-schema-green-plum.sql"

    val dbApi = inject[DBApi]
    logger.info(s"Databases are:\n${dbApi.databases().map(db ⇒ s"${db.name}: ${db.url}").mkString("\n")}")
    (for {
      rawDropSql ← Try(Source.fromResource(dropSchemaSqlName).mkString).toEither.leftMap(_.getMessage)
      rawDataSql ← Try(Source.fromResource(initDataSqlName).mkString).toEither.leftMap(_.getMessage)
      db ← dbApi.databases().headOption.toRight("No database configured")
      _ ← db.withTransaction { implicit connection ⇒
        for {
          _ ← runUpdateSql(rawDropSql)
          _ ← runUpdateSql(rawDataSql)
        } yield {
          defaultUserId = SQL(s"SELECT id FROM $schemaName.users LIMIT 1;")
            .as(SqlParser.scalar[Int].single)
        }
      }
    } yield ())
      .leftMap(err ⇒ logger.error("Failed to prepare db: " + err))
  }


  protected def runUpdateSql(rawSql: String)(implicit connection: Connection): Either[String, Unit] = {
    Try {
      rawSql
        .split(";")
        .collect {
          case stmt if stmt.trim.nonEmpty ⇒
            //SQL(stmt).execute() NOTE: commented out because anorm doesnt like some characters present in report_definition
            connection.createStatement().execute(stmt)
        }
    }.toEither.bimap(
      exc ⇒ {
        logger.error(s"Failed to run SQL query.", exc)
        exc.getMessage
      },
      _ ⇒ ()
    )
  }

  protected def makePostJsonRequest(path: String, json: String): FakeRequest[AnyContentAsJson] = {
    makeJsonRequest("POST")(path, json)
  }

  protected def makePutJsonRequest(path: String, json: String): FakeRequest[AnyContentAsJson] = {
    makeJsonRequest("PUT")(path, json)
  }

  protected def makeDeleteJsonRequest(path: String, json: String): FakeRequest[AnyContentAsJson] = {
    makeJsonRequest("DELETE")(path, json)
  }

  private def makeJsonRequest(verb: String)(path: String, json: String): FakeRequest[AnyContentAsJson] = {
    val baseRequest = FakeRequest(verb, path)
    val jsonPayload = if (json.isEmpty) {
      JsString.apply(json)
    } else {
      Json.parse(json)
    }
    baseRequest.withJsonBody(jsonPayload)
  }
}
