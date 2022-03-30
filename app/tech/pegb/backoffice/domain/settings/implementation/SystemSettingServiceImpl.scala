package tech.pegb.backoffice.domain.settings.implementation

import com.google.inject.Inject
import tech.pegb.backoffice.dao.settings.abstraction.SystemSettingsDao
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.settings.abstraction.SystemSettingService
import tech.pegb.backoffice.domain.settings.dto.SystemSettingsCriteria
import tech.pegb.backoffice.domain.settings.model.SystemSetting
import tech.pegb.backoffice.domain.{BaseService, ServiceError}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.setting.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.setting.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SystemSettingServiceImpl @Inject() (
    executionContexts: WithExecutionContexts,
    systemSettingsDao: SystemSettingsDao) extends SystemSettingService with BaseService {

  implicit val ec: ExecutionContext = executionContexts.genericOperations

  def getSystemSettingArrayValueByKey(key: String): Future[ServiceResponse[Seq[String]]] = Future {

    for {
      optionalSystemSetting ← systemSettingsDao.getSystemSettingByKey(key).asServiceResponse
      systemSetting ← optionalSystemSetting.fold[ServiceResponse[SystemSetting]](ServiceError.notFoundError(s"").toLeft)(_.asDomain.toRight)
      arrayValues ← Try {
        systemSetting.value
          .replaceAll("[\\[\\]]", "")
          .split(",")
          .filterNot(_ === "")
      }.toEither
        .fold(t ⇒
          Left(ServiceError.validationError(s"error while parsing system value of json array, reason ${t.getMessage}")), Right(_))
    } yield arrayValues
  }

  def getSystemSettingsByCriteria(
    criteria: Option[SystemSettingsCriteria],
    orderBy: Seq[Ordering] = Seq.empty,
    limit: Option[Int] = None,
    offset: Option[Int] = None): Future[ServiceResponse[Seq[SystemSetting]]] =
    Future {
      systemSettingsDao
        .getSystemSettingsByCriteria(criteria.map(_.asDao), orderBy.asDao, limit, offset)
        .map(_.map(_.asDomain)).asServiceResponse
    }
}
