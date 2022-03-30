package tech.pegb.backoffice.dao.savings.abstraction

import com.google.inject.ImplementedBy

import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.savings.dto.AutoDeductSavingsCriteria
import tech.pegb.backoffice.dao.savings.entity.AutoDeductSaving
import tech.pegb.backoffice.dao.savings.sql.AutoDeductSavingsSqlDao

@ImplementedBy(classOf[AutoDeductSavingsSqlDao])
trait AutoDeductSavingsDao extends SavingOptionsDao[AutoDeductSavingsCriteria, AutoDeductSaving] {

  def getSavingOptionsByCriteria(
    filter: Option[AutoDeductSavingsCriteria],
    ordering: Option[OrderingSet] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None): DaoResponse[Seq[AutoDeductSaving]]

  def countSavingOptionsByCriteria(filter: Option[AutoDeductSavingsCriteria]): DaoResponse[Int]

}
