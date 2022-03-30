package tech.pegb.backoffice.api.makerchecker

import play.api.mvc.Request
import tech.pegb.backoffice.api.RequiredHeaders
import tech.pegb.backoffice.util.AppConfig

import scala.util.Try

trait MakerCheckerRequiredHeaders extends RequiredHeaders {
  val appConfig: AppConfig

  def getRoleLevel[T](implicit ctx: Request[T]): Option[Int] = {
    ctx.headers.get(appConfig.HeaderKeys.RoleLevelKey).flatMap(role ⇒ Try(role.toInt).toOption)
  }

  def getBusinessUnitName[T](implicit ctx: Request[T]): Option[String] = {
    ctx.headers.get(appConfig.HeaderKeys.BusinessUnitKey)
      .collect {
        case bu if bu.trim.isEmpty ⇒ None
        case bu ⇒ Some(bu.trim)
      }
  }.flatten

}
