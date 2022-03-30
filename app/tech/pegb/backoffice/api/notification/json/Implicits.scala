package tech.pegb.backoffice.api.notification.json

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json._
import tech.pegb.backoffice.api.notification.dto.NotificationTemplateToUpdate

object Implicits {

  private implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

  implicit val notificationTemplateToUpdate = Json.format[NotificationTemplateToUpdate]

}
