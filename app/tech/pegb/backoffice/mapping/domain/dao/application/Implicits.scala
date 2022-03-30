package tech.pegb.backoffice.mapping.domain.dao.application

import tech.pegb.backoffice.dao.application.dto.WalletApplicationCriteria
import tech.pegb.backoffice.domain.application.dto.{WalletApplicationCriteria ⇒ DomainWalletApplicationCriteria}

object Implicits {

  implicit class WalletApplicationCriteriaAdapter(val arg: DomainWalletApplicationCriteria) extends AnyVal {
    def asDao(inactiveStatus: Set[String]): WalletApplicationCriteria = {
      WalletApplicationCriteria(
        customerId = arg.customerId,
        msisdn = arg.msisdn.map(_.underlying),
        name = arg.name,
        fullName = arg.fullName,
        nationalId = arg.nationalId,
        status = arg.status.map(_.underlying),
        inactiveStatuses = inactiveStatus,
        applicationStage = arg.applicationStage,
        checkedBy = arg.checkedBy,
        checkedAtStartingFrom = arg.checkedAtStartingFrom,
        checkedAtUpTo = arg.checkedAtUpTo,
        createdBy = arg.createdBy,
        createdAtStartingFrom = arg.createdAtStartingFrom,
        createdAtUpTo = arg.createdAtUpTo)
    }
  }

}
