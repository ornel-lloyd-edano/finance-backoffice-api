package tech.pegb.backoffice.domain.fee.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.fee.dto._
import tech.pegb.backoffice.domain.fee.implementation.FeeProfileMgmtService
import tech.pegb.backoffice.domain.fee.model.{FeeProfile, FeeProfileRange}
import tech.pegb.backoffice.domain.{BaseService, model}

import scala.concurrent.Future

@ImplementedBy(classOf[FeeProfileMgmtService])
trait FeeProfileManagement extends BaseService {

  def createFeeProfile(createDto: FeeProfileToCreate)(implicit requestId: UUID): Future[ServiceResponse[FeeProfile]]

  def addFeeProfileRanges(id: UUID, ranges: Seq[FeeProfileRangeToCreate], addedBy: String, addedAt: LocalDateTime): Future[ServiceResponse[FeeProfile]]

  def getFeeProfile(id: UUID)(implicit requestId: UUID): Future[ServiceResponse[FeeProfile]]

  def getFeeProfileByCriteria(criteriaDto: FeeProfileCriteria, ordering: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[FeeProfile]]]

  def countFeeProfileByCriteria(criteriaDto: FeeProfileCriteria): Future[ServiceResponse[Int]]

  def updateFeeProfile(id: UUID, updateDto: FeeProfileToUpdate)(implicit requestId: UUID): Future[ServiceResponse[FeeProfile]]

  def updateFeeProfileRange(rangeId: Int, profileId: UUID, updateDto: FeeProfileRangeToUpdate)(implicit requestId: UUID): Future[ServiceResponse[FeeProfileRange]]

  def deleteFeeProfile(id: UUID, deletedBy: String, deletedAt: LocalDateTime, updatedAt: Option[LocalDateTime])(implicit requestId: UUID): Future[ServiceResponse[FeeProfile]]

  def deleteFeeProfileRange(rangeId: Int, feeProfileId: UUID)(implicit requestId: UUID): Future[ServiceResponse[FeeProfileRange]]
}
