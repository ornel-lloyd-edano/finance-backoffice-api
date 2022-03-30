package tech.pegb.backoffice.dao.savings.abstraction

import com.google.inject.ImplementedBy

import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.savings.dto.SavingGoalsCriteria
import tech.pegb.backoffice.dao.savings.entity.SavingGoal
import tech.pegb.backoffice.dao.savings.sql.{SavingGoalsSqlDao}

@ImplementedBy(classOf[SavingGoalsSqlDao])
trait SavingGoalsDao extends SavingOptionsDao[SavingGoalsCriteria, SavingGoal] {

  def getSavingOptionsByCriteria(
    filter: Option[SavingGoalsCriteria],
    ordering: Option[OrderingSet] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None): DaoResponse[Seq[SavingGoal]]

  def countSavingOptionsByCriteria(filter: Option[SavingGoalsCriteria]): DaoResponse[Int]
}
