package tech.pegb.backoffice.dao.savings.abstraction

import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.savings.dto.SavingOptionCriteria
import tech.pegb.backoffice.dao.savings.entity.SavingOption

trait SavingOptionsDao[P <: SavingOptionCriteria, R <: SavingOption] extends Dao {
  def getSavingOptionsByCriteria(
    filter: Option[P],
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[R]]

  def countSavingOptionsByCriteria(filter: Option[P]): DaoResponse[Int]
}
