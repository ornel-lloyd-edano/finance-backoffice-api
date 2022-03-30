package tech.pegb.backoffice.mapping.domain.dao.makerchecker

import java.util.UUID

import tech.pegb.backoffice.dao.makerchecker.dto.TaskToInsert
import tech.pegb.backoffice.dao.makerchecker.sql.TasksSqlDao
import tech.pegb.backoffice.domain.makerchecker.dto.{MakerCheckerCriteria, TaskToApprove, TaskToCreate, TaskToReject}
import tech.pegb.backoffice.dao.makerchecker.{dto ⇒ dao}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.makerchecker.model.{RoleLevel, Statuses}

object Implicits {

  implicit class TaskToCreateDomainToDaoAdapter(val arg: TaskToCreate) extends AnyVal {
    def asDao(requestId: UUID, status: String, valueToUpdate: Option[String]): TaskToInsert = {
      dao.TaskToInsert(
        uuid = requestId.toString,
        module = arg.module,
        action = arg.action,
        verb = arg.makerRequest.verb.toString,
        url = arg.makerRequest.rawUrl.toString,
        headers = arg.makerRequest.headers.toString,
        body = arg.makerRequest.body.map(_.toString),
        valueToUpdate = valueToUpdate,
        status = status,
        createdBy = arg.maker.createdBy,
        createdAt = arg.maker.createdAt,
        makerLevel = arg.maker.level.underlying,
        makerBusinessUnit = arg.maker.businessUnit,
        checkedAt = None,
        checkedBy = None)
    }
  }

  implicit class TaskToApproveDomainToDaoAdapter(val arg: TaskToApprove) extends AnyVal {
    def asDao = {
      dao.TaskToUpdate(
        reason = arg.maybeReason,
        checkedBy = Option(arg.approvedBy),
        checkedAt = Option(arg.approvedAt),
        status = Option(Statuses.Approved.toString))
    }
  }

  implicit class TaskToRejectDomainToDaoAdapter(val arg: TaskToReject) extends AnyVal {
    def asDao = {
      dao.TaskToUpdate(
        reason = Option(arg.rejectionReason),
        checkedBy = Option(arg.rejectedBy),
        checkedAt = Option(arg.rejectedAt),
        status = Option(Statuses.Rejected.toString))
    }
  }

  implicit class MakerCheckerCriteriaDomainToDaoAdapter(val arg: MakerCheckerCriteria) extends AnyVal {
    def asDao(requesterLevel: RoleLevel, requesterBusinessUnit: Option[String]) = {
      dao.MakerCheckerCriteria(
        uuid = arg.id.map(v ⇒ CriteriaField(TasksSqlDao.cUuid, v.underlying)),
        status = arg.status.map(v ⇒ CriteriaField(TasksSqlDao.cStatus, v.underlying)),
        module = arg.module.map(v ⇒ CriteriaField(TasksSqlDao.cModule, v)),
        createdAtFrom = arg.createdAtFrom,
        createdAtTo = arg.createdAtTo,
        makerLevel = Some(CriteriaField(TasksSqlDao.cMakerLevel, requesterLevel.underlying, MatchTypes.GreaterOrEqual)),
        makerBusinessUnit = requesterBusinessUnit.map(CriteriaField(TasksSqlDao.cMakerBu, _)))
    }
  }

}
