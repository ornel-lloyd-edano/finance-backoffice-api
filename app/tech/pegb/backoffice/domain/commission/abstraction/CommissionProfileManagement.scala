package tech.pegb.backoffice.domain.commission.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.commission.dto.{CommissionProfileCriteria, CommissionProfileToCreate, CommissionProfileToUpdate}
import tech.pegb.backoffice.domain.commission.implementation.CommissionProfileMgmtService
import tech.pegb.backoffice.domain.commission.model.CommissionProfile
import tech.pegb.backoffice.domain.model

import scala.concurrent.Future

@ImplementedBy(classOf[CommissionProfileMgmtService])
trait CommissionProfileManagement {

  def createCommissionProfile(dto: CommissionProfileToCreate): Future[ServiceResponse[CommissionProfile]]

  def getCommissionProfile(id: UUID): Future[ServiceResponse[CommissionProfile]]

  def getCommissionProfileByCriteria(criteriaDto: CommissionProfileCriteria, ordering: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[CommissionProfile]]]

  def countCommissionProfileByCriteria(criteriaDto: CommissionProfileCriteria): Future[ServiceResponse[Int]]

  def updateCommissionProfile(id: UUID, dto: CommissionProfileToUpdate): Future[ServiceResponse[CommissionProfile]]

  def deleteCommissionProfile(id: UUID, deletedBy: String, deletedAt: LocalDateTime, updatedAt: Option[LocalDateTime]): Future[ServiceResponse[CommissionProfile]]
}
