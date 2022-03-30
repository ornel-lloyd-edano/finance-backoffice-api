package tech.pegb.backoffice.domain.settings.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.settings.dto.SystemSettingsCriteria
import tech.pegb.backoffice.domain.settings.implementation.SystemSettingServiceImpl
import tech.pegb.backoffice.domain.settings.model.SystemSetting

import scala.concurrent.Future

@ImplementedBy(classOf[SystemSettingServiceImpl])
trait SystemSettingService {

  def getSystemSettingArrayValueByKey(key: String): Future[ServiceResponse[Seq[String]]]

  def getSystemSettingsByCriteria(
    criteria: Option[SystemSettingsCriteria],
    orderBy: Seq[Ordering] = Seq.empty,
    limit: Option[Int] = None,
    offset: Option[Int] = None): Future[ServiceResponse[Seq[SystemSetting]]]
}
