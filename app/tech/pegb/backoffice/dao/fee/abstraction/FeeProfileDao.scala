package tech.pegb.backoffice.dao.fee.abstraction

import java.sql.Connection
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.Dao.{EntityId}
import tech.pegb.backoffice.dao.fee.dto._
import tech.pegb.backoffice.dao.fee.entity.{FeeProfile, FeeProfileRange}
import tech.pegb.backoffice.dao.fee.sql.FeeProfileSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[FeeProfileSqlDao])
trait FeeProfileDao extends Dao {

  def insertFeeProfile(dto: FeeProfileToInsert): DaoResponse[FeeProfile]

  def insertFeeProfileRange(feeProfileId: EntityId, dto: Seq[FeeProfileRangeToInsert])(implicit connectionOption: Option[Connection] = None): DaoResponse[Seq[FeeProfileRange]]

  def getFeeProfile(id: EntityId): DaoResponse[Option[FeeProfile]]

  def getFeeProfileByCriteria(criteria: FeeProfileCriteria, ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[FeeProfile]]

  def getFeeProfileRangesByFeeProfileId(feeProfileId: EntityId): DaoResponse[Seq[FeeProfileRange]]

  def countFeeProfileByCriteria(criteria: FeeProfileCriteria): DaoResponse[Int]

  def updateFeeProfile(id: EntityId, dto: FeeProfileToUpdate): DaoResponse[Option[FeeProfile]]

  def updateFeeProfileRange(rangeId: Int, dto: FeeProfileRangeToUpdate): DaoResponse[Option[FeeProfileRange]]

  def deleteFeeProfileRange(rangeId: Int)(implicit requestId: UUID): DaoResponse[Option[FeeProfileRange]]

}
