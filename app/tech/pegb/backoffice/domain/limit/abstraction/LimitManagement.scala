package tech.pegb.backoffice.domain.limit.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.limit.dto.{LimitProfileCriteria, LimitProfileToCreate, LimitProfileToUpdate}
import tech.pegb.backoffice.domain.limit.implementation.LimitMgmtService
import tech.pegb.backoffice.domain.limit.model.LimitProfile
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[LimitMgmtService])
trait LimitManagement extends BaseService {

  def getLimitProfile(id: UUID)(implicit requestId: UUID): Future[ServiceResponse[LimitProfile]]

  def getLimitProfileByCriteria(criteria: LimitProfileCriteria, ordering: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[LimitProfile]]]

  def countLimitProfileByCriteria(criteria: LimitProfileCriteria): Future[ServiceResponse[Int]]

  def createLimitProfile(createDto: LimitProfileToCreate)(implicit requestId: UUID): Future[ServiceResponse[LimitProfile]]

  def updateLimitProfileValues(
    id: UUID,
    updateDto: LimitProfileToUpdate)(
    implicit
    requestId: UUID): Future[ServiceResponse[LimitProfile]]

  def deleteLimitProfile(
    id: UUID,
    deletedBy: String,
    deletedAt: LocalDateTime,
    updatedAt: Option[LocalDateTime])(
    implicit
    requestId: UUID): Future[ServiceResponse[LimitProfile]]

}
