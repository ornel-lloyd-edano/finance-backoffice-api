package tech.pegb.backoffice.domain.makerchecker.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.makerchecker.dto.{MakerCheckerCriteria, TaskToApprove, TaskToCreate, TaskToReject}
import tech.pegb.backoffice.domain.makerchecker.implementation.MakerCheckerServiceImpl
import tech.pegb.backoffice.domain.makerchecker.model.{MakerCheckerTask, RoleLevel}
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[MakerCheckerServiceImpl])
trait MakerCheckerService {

  def getTasksByCriteria(
    criteria: MakerCheckerCriteria,
    requesterLevel: RoleLevel,
    requesterBusinessUnit: String,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int])(implicit requestId: UUID): Future[ServiceResponse[Seq[MakerCheckerTask]]]

  def getTaskById(id: UUID)(implicit requestId: UUID): Future[ServiceResponse[MakerCheckerTask]]

  def countTasksByCriteria(
    criteria: MakerCheckerCriteria,
    requesterLevel: RoleLevel,
    requesterBusinessUnit: String): Future[ServiceResponse[Int]]

  def createPendingTask(dto: TaskToCreate, requestId: UUID): Future[ServiceResponse[MakerCheckerTask]]

  def approvePendingTask(dto: TaskToApprove): Future[ServiceResponse[MakerCheckerTask]]

  def rejectPendingTask(dto: TaskToReject): Future[ServiceResponse[MakerCheckerTask]]

  def validateCheckerProcess(id: String, requesterUsername: String, requestorLevel: RoleLevel, requestorBu: String, process: String): ServiceResponse[MakerCheckerTask]

  def notifyCheckers(task: MakerCheckerTask): Future[ServiceResponse[Unit]]

  def notifyMaker: Future[ServiceResponse[Unit]]
}
