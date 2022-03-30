package tech.pegb.backoffice.domain.report.implementation

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.libs.json.{JsArray, JsString, JsValue}
import tech.pegb.backoffice.dao.auth.abstraction.{PermissionDao, ScopeDao}
import tech.pegb.backoffice.dao.auth.dto.ScopeToInsert
import tech.pegb.backoffice.dao.report.abstraction.{ReportDao, ReportDefinitionDao}
import tech.pegb.backoffice.domain.report.abstraction.ReportManagement
import tech.pegb.backoffice.domain.report.dto.{ReportDefinitionCriteria, ReportDefinitionPermission, ReportDefinitionToCreate, ReportDefinitionToUpdate}
import tech.pegb.backoffice.domain.report.model.{Report, ReportDefinition}
import tech.pegb.backoffice.domain.{BaseService, ServiceError, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.report.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.report.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future

class ReportMgmtService @Inject() (
    executionContexts: WithExecutionContexts,
    appConfig: AppConfig,
    reportDefDao: ReportDefinitionDao,
    scopeDao: ScopeDao,
    permissionDao: PermissionDao,
    reportsDao: ReportDao) extends ReportManagement with BaseService {
  import ReportMgmtService._

  implicit val ec = executionContexts.blockingIoOperations

  def createReportDefinition(record: ReportDefinitionToCreate): Future[ServiceResponse[ReportDefinition]] = {

    val postParameters = record.parameters.getOrElse(JsArray.empty)
    logger.info("Parameters sent via record -→" + postParameters)

    // Generate parameters with default
    val params = generateParamsForSql(postParameters)

    logger.info("What is SQL now ??? " + record.sql + "    " + record.name)
    val validSql = if (!record.sql.isEmpty) s"${record.sql.stripSuffix(";")} LIMIT 1" else ""

    (for {
      //Check if the rawSql is correct
      _ ← EitherT.fromEither[Future](reportsDao.executeRawSql(validSql, postParameters, params)
        .leftMap(e ⇒ ServiceError.validationError(e.toString, e.id.some)))
      //Check if parent scope exists
      parentScopeIdOption ← EitherT.fromEither[Future](scopeDao.getScopeIdByName("reporting").asServiceResponse)
      parentScopeId ← EitherT.fromOption[Future](
        parentScopeIdOption,
        ServiceError.validationError(s"Parent UUID missing in 'scopes' table." +
          s" Run insert into scopes values (uuid(),null,'reporting','parent for all reporting',1,'admin','admin',now(),now()); "))

      //Create Report Definition and scope
      reportDefinitionResult ← EitherT.fromEither[Future](reportDefDao.createReportDefinition(
        reportDefinitionToInsert = record.asDao,
        scopeToInsert = ScopeToInsert(
          name = record.name,
          parentId = parentScopeId.some,
          description = record.description.some,
          isActive = 1,
          createdBy = record.createdBy,
          createdAt = record.createdAt)).asServiceResponse)

      reportDefinitionDomain ← EitherT.fromEither[Future](reportDefinitionResult.asDomain.toEither
        .leftMap(t ⇒ ServiceError.dtoMappingError(t.getMessage)))
    } yield {
      reportDefinitionDomain
    }).value
  }

  def countReportDefinitionByCriteria(criteriaDto: ReportDefinitionCriteria): Future[ServiceResponse[Int]] = Future {
    reportDefDao.countReportDefinitionByCriteria(criteriaDto.asDao).asServiceResponse
  }

  def getReportDefinitionByCriteria(
    criteriaDto: ReportDefinitionCriteria,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[ReportDefinition]]] = Future {
    reportDefDao.getReportDefinitionByCriteria(criteriaDto.asDao, ordering.asDao, limit, offset).map(_.flatMap(_.asDomain.toOption)).asServiceResponse
  }

  def getReportDefinitionById(id: UUID): Future[ServiceResponse[ReportDefinition]] = {
    (for {
      reportDefinitionOption ← EitherT.fromEither[Future](reportDefDao.getReportDefinitionById(id.toString).asServiceResponse)
      reportDefinitionResult ← EitherT.fromOption[Future](
        reportDefinitionOption, ServiceError.notFoundError(s"Report Definition with id [$id] not found"))
      reportDefinitionDomain ← EitherT.fromEither[Future](reportDefinitionResult.asDomain.toEither
        .leftMap(t ⇒ ServiceError.dtoMappingError(t.getMessage)))
    } yield {
      reportDefinitionDomain
    }).value
  }

  def deleteReportDefinitionById(id: UUID): Future[ServiceResponse[Boolean]] = {
    (for {
      toBeDeleted ← EitherT(getReportDefinitionById(id))
      scopeIdOption ← EitherT.fromEither[Future](scopeDao.getScopeIdByName(toBeDeleted.name).asServiceResponse)
      permissionIdList ← EitherT.fromEither[Future](
        scopeIdOption.fold(Seq[String]().asRight[ServiceError])(
          scopeId ⇒ permissionDao.getPermissionIdsByScopeId(scopeId).asServiceResponse))
      deleteResult ← EitherT.fromEither[Future](reportDefDao.deleteReportDefinitionById(id.toString, scopeIdOption, permissionIdList).asServiceResponse)
    } yield {
      deleteResult
    }).value
  }

  def updateReportDefinition(id: UUID, reportDefinitionToUpdate: ReportDefinitionToUpdate): Future[ServiceResponse[ReportDefinition]] = {
    val postParameters = reportDefinitionToUpdate.parameters.getOrElse(JsArray.empty)

    val params = generateParamsForSql(postParameters)

    val validSql =
      if (reportDefinitionToUpdate.sql.isEmpty) {
        ""
      } else if (reportDefinitionToUpdate.sql.toLowerCase.contains("limit")) {
        reportDefinitionToUpdate.sql
      } else {
        s"${reportDefinitionToUpdate.sql.stripSuffix(";")} LIMIT 1"
      }

    (for {
      //Check if the rawSql is correct
      _ ← EitherT.fromEither[Future](reportsDao.executeRawSql(validSql, postParameters, params)
        .leftMap(e ⇒ ServiceError.validationError(e.toString(), e.id.some)))

      updateResultOption ← EitherT.fromEither[Future](reportDefDao.updateReportDefinitionById(id.toString, reportDefinitionToUpdate.asDao).asServiceResponse)
      updateResult ← EitherT.fromOption[Future](updateResultOption, ServiceError.notFoundError(s"Report definition $id was not found"))
      reportDefinitionDomain ← EitherT.fromEither[Future](updateResult.asDomain.toEither
        .leftMap(t ⇒ ServiceError.dtoMappingError(t.getMessage)))
    } yield {
      reportDefinitionDomain
    }).value
  }

  def getReportData(id: UUID, queryParams: Map[String, String]): Future[ServiceResponse[Report]] = {
    (for {
      reportDefinition ← EitherT(getReportDefinitionById(id))
      report ← EitherT.fromEither[Future](reportsDao.executeRawSql(
        rawSql = reportDefinition.sql,
        reportDefinitionParams = reportDefinition.parameters.getOrElse(JsArray.empty),
        queryParams = queryParams).asServiceResponse)
    } yield {
      report.asDomain
    }).value
  }

  def getAvailableReportsForUser(backOfficeUserName: String): Future[ServiceResponse[Seq[ReportDefinitionPermission]]] = Future {
    reportDefDao.getReportDefinitionPermissionByBackOfficeUserName(backOfficeUserName)
      .bimap(_.asDomainError, _.map(_.asDomain))
  }
}

object ReportMgmtService {
  private[report] def mapTypeToDefaultValue(`type`: String, default: String): String = {
    if (!default.isEmpty) {
      default
    } else {
      `type` match {
        case "boolean" ⇒ "1"
        case "amount" ⇒ "1"
        case "number" | "percentage" | "ratio" ⇒ "1"
        case _ ⇒ ""
      }
    }
  }

  private def generateParamsForSql(parameters: JsArray): Map[String, String] = {
    val params = Map.newBuilder[String, String]
    parameters.as[Seq[JsValue]].foldLeft(params)((acc, head) ⇒ {
      val name: String = (head \ "name").as[JsString].value
      val `type` = (head \ "type").as[JsString].value
      val default = (head \ "default").getOrElse(JsString("")).as[JsString].value
      val paramValue = mapTypeToDefaultValue(`type`, default)
      acc += (name → paramValue)
      acc
    })

    params.result()
  }
}
