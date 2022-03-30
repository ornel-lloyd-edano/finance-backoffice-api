package tech.pegb.backoffice.api.makerchecker.json

import play.api.libs.json.Json
import tech.pegb.backoffice.api.makerchecker.dto.{ApproveTaskRequest, RejectTaskRequest, TaskToCreate}

object Implicits {
  implicit val apiCreateTaskFormat = Json.format[TaskToCreate]

  implicit val apiApproveTaskFormat = Json.format[ApproveTaskRequest]

  implicit val apiRejectTaskFormat = Json.format[RejectTaskRequest]
}
