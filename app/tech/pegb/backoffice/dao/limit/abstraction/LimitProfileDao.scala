package tech.pegb.backoffice.dao.limit.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.Dao.{EntityId}
import tech.pegb.backoffice.dao.limit.dto.{LimitProfileCriteria, LimitProfileToInsert, LimitProfileToUpdate}
import tech.pegb.backoffice.dao.limit.entity.LimitProfile
import tech.pegb.backoffice.dao.limit.sql.LimitProfileSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[LimitProfileSqlDao])
trait LimitProfileDao extends Dao {

  def getLimitProfile(id: EntityId): DaoResponse[Option[LimitProfile]]

  def getLimitProfileByCriteria(criteria: LimitProfileCriteria, ordering: Option[OrderingSet] = None,
    limit: Option[Int] = None, offset: Option[Int] = None): DaoResponse[Seq[LimitProfile]]

  def countLimitProfileByCriteria(criteria: LimitProfileCriteria): DaoResponse[Int]

  def insertLimitProfile(dto: LimitProfileToInsert): DaoResponse[LimitProfile]

  //also used for soft deleting
  def updateLimitProfile(id: EntityId, updateDto: LimitProfileToUpdate): DaoResponse[Option[LimitProfile]]

}
