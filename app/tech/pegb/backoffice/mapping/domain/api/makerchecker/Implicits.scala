package tech.pegb.backoffice.mapping.domain.api.makerchecker

import tech.pegb.backoffice.domain.makerchecker.model.MakerCheckerTask
import tech.pegb.backoffice.domain.makerchecker.model.RoleLevels._
import tech.pegb.backoffice.api.makerchecker.dto
import tech.pegb.backoffice.api.makerchecker.dto.TaskDetailToRead
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class MakerCheckerTaskDomainApiAdapter(val arg: MakerCheckerTask) extends AnyVal {
    def asApi(backOfficeUserName: String, backOfficeUserLevel: Int, backOfficeUserBusinessUnit: String) = {
      dto.TaskToRead(
        id = arg.id.toString,
        module = arg.module,
        action = arg.actionRequired,
        status = arg.status.toString,
        reason = arg.reason,
        createdAt = arg.maker.createdAt.toZonedDateTimeUTC,
        createdBy = arg.maker.createdBy,
        checkedAt = arg.checker.map(_.checkedAt.toZonedDateTimeUTC),
        checkedBy = arg.checker.map(_.checkedBy),
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        isReadOnly = !arg.isCheckerAllowed(backOfficeUserName, backOfficeUserLevel.asDomain, backOfficeUserBusinessUnit))
    }

    def asApiDetail(
      backOfficeUserName: String,
      backOfficeUserLevel: Int,
      backOfficeUserBusinessUnit: String): TaskDetailToRead = {
      dto.TaskDetailToRead(
        id = arg.id.toString,
        module = arg.module,
        action = arg.actionRequired,
        status = arg.status.toString,
        reason = arg.reason,
        createdAt = arg.maker.createdAt.toZonedDateTimeUTC,
        createdBy = arg.maker.createdBy,
        checkedAt = arg.checker.map(_.checkedAt.toZonedDateTimeUTC),
        checkedBy = arg.checker.map(_.checkedBy),
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        change = arg.change,
        originalValue = arg.original,
        isReadOnly = !arg.isCheckerAllowed(backOfficeUserName, backOfficeUserLevel.asDomain, backOfficeUserBusinessUnit),
        stale = arg.isStale)
    }

    def asApiCreated = {
      dto.TaskCreated(
        id = arg.id.toString,
        link = s"/tasks/${arg.id.toString}")
    }
  }

}
