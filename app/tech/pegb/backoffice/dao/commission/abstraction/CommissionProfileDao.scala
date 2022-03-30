package tech.pegb.backoffice.dao.commission.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.commission.dto.{CommissionProfileCriteria, CommissionProfileToInsert, CommissionProfileToUpdate}
import tech.pegb.backoffice.dao.commission.entity.{CommissionProfile, CommissionProfileRange}
import tech.pegb.backoffice.dao.commission.sql.CommissionProfileSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[CommissionProfileSqlDao])
trait CommissionProfileDao extends Dao {

  def insertCommissionProfile(dto: CommissionProfileToInsert): DaoResponse[CommissionProfile]

  def getCommissionProfileByCriteria(
    criteria: CommissionProfileCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[CommissionProfile]]

  def getCommissionProfileRangeByCommissionId(commissionId: Int): DaoResponse[Seq[CommissionProfileRange]]

  def countCommissionProfileByCriteria(criteria: CommissionProfileCriteria): DaoResponse[Int]

  def updateCommissionProfile(uuid: String, dto: CommissionProfileToUpdate): DaoResponse[Option[CommissionProfile]]
}
