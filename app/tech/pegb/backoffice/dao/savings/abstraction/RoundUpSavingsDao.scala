package tech.pegb.backoffice.dao.savings.abstraction

import com.google.inject.ImplementedBy

import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.savings.dto.RoundUpSavingsCriteria
import tech.pegb.backoffice.dao.savings.entity.RoundUpSaving
import tech.pegb.backoffice.dao.savings.sql.{RoundUpSavingsSqlDao}

@ImplementedBy(classOf[RoundUpSavingsSqlDao])
trait RoundUpSavingsDao extends SavingOptionsDao[RoundUpSavingsCriteria, RoundUpSaving] {

  def getSavingOptionsByCriteria(
    filter: Option[RoundUpSavingsCriteria],
    ordering: Option[OrderingSet] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None): DaoResponse[Seq[RoundUpSaving]]

  def countSavingOptionsByCriteria(filter: Option[RoundUpSavingsCriteria]): DaoResponse[Int]
}
