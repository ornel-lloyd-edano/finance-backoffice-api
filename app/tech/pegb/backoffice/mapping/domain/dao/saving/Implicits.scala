package tech.pegb.backoffice.mapping.domain.dao.saving

import tech.pegb.backoffice.dao.model.CriteriaField
import tech.pegb.backoffice.dao.savings.dto._
import tech.pegb.backoffice.domain.customer.dto.SavingOptionCriteria

object Implicits {

  implicit class DaoConverter(criteria: SavingOptionCriteria) {
    def asSavingGoalCriteriaDao: SavingGoalsCriteria =
      SavingGoalsCriteria(
        uuid = criteria.uuid.map(uuid ⇒ CriteriaField("", uuid.toString)),
        userUuid = criteria.userUuid.map(userUuid ⇒ CriteriaField("", userUuid.toString)),
        isActive = criteria.isActive.map(CriteriaField("", _)))

    def asAutoDeductCriteriaDao: AutoDeductSavingsCriteria =
      AutoDeductSavingsCriteria(
        uuid = criteria.uuid.map(uuid ⇒ CriteriaField("", uuid.toString)),
        userUuid = criteria.userUuid.map(userUuid ⇒ CriteriaField("", userUuid.toString)),
        isActive = criteria.isActive.map(CriteriaField("", _)))

    def asRoundUpCriteriaDao: RoundUpSavingsCriteria =
      RoundUpSavingsCriteria(
        uuid = criteria.uuid.map(uuid ⇒ CriteriaField("", uuid.toString)),
        userUuid = criteria.userUuid.map(userUuid ⇒ CriteriaField("", userUuid.toString)),
        isActive = criteria.isActive.map(CriteriaField("", _)))
  }

}
