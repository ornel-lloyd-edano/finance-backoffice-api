package tech.pegb.backoffice.mapping.api.domain.makerchecker

import java.time.ZonedDateTime
import java.util.UUID

import tech.pegb.backoffice.api.makerchecker.dto.{ApproveTaskRequest, RejectTaskRequest, TaskToCreate}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.domain.makerchecker.dto.MakerCheckerCriteria
import tech.pegb.backoffice.domain.makerchecker.model.RoleLevels
import tech.pegb.backoffice.domain.{makerchecker ⇒ domain}
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

object Implicits {

  import domain.model.Statuses._

  implicit class TaskToCreateApiAdapter(val arg: TaskToCreate) extends AnyVal {
    def asDomain(doneBy: String, doneAt: ZonedDateTime, level: Int, businessUnit: String) = Try {
      domain.dto.TaskToCreate(
        maker = domain.model.MakerDetails(
          createdBy = doneBy,
          createdAt = doneAt.toLocalDateTimeUTC,
          level = RoleLevels(level),
          businessUnit = businessUnit),
        makerRequest = domain.model.MakerRequest(
          verb = arg.verb,
          url = arg.url,
          queryParams = None,
          body = arg.body,
          headers = arg.headers),
        module = arg.module,
        action = arg.action)
    }
  }

  implicit class TaskToApproveApiToDomainAdapter(val arg: ApproveTaskRequest) extends AnyVal {
    def asDomain(taskId: UUID, doneBy: String, doneAt: ZonedDateTime, level: Int, businessUnit: String) = Try {
      domain.dto.TaskToApprove(
        id = taskId,
        maybeReason = arg.maybeReason,
        approvedBy = doneBy,
        approvedAt = doneAt.toLocalDateTimeUTC,
        checkerLevel = RoleLevels(level),
        checkerBusinessUnit = businessUnit)
    }
  }

  implicit class TaskToRejectApiToDomainAdapter(val arg: RejectTaskRequest) extends AnyVal {
    def asDomain(taskId: UUID, doneBy: String, doneAt: ZonedDateTime, level: Int, businessUnit: String) = Try {
      domain.dto.TaskToReject(
        id = taskId,
        rejectionReason = arg.reason,
        rejectedBy = doneBy,
        rejectedAt = doneAt.toLocalDateTimeUTC,
        checkerLevel = RoleLevels(level),
        checkerBusinessUnit = businessUnit)
    }
  }

  implicit class QueryParamsToDomainCriteriaAdapter(val args: (Option[String], Option[String], Option[LocalDateTimeFrom], Option[LocalDateTimeTo], Option[Boolean], Set[String])) extends AnyVal {
    def asDomain = {
      Try(MakerCheckerCriteria(
        module = args._1,
        status = args._2.map(_.asDomain),
        createdAtFrom = args._3.map(_.localDateTime),
        createdAtTo = args._4.map(_.localDateTime),
        isAllowedToCheck = args._5.map(readOnly ⇒ !readOnly),
        partialMatchFields = args._6))
    }
  }
}
