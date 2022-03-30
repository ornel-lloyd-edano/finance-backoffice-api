package tech.pegb.backoffice.domain.businessuserapplication.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplication
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes.{AccountConfig, ExternalAccount, TransactionConfig}

//TODO extend Validatable
case class BusinessUserApplicationConfigToCreate(
    applicationUUID: UUID,
    transactionConfig: Seq[TransactionConfig],
    accountConfig: Seq[AccountConfig],
    externalAccounts: Seq[ExternalAccount],
    createdBy: String,
    createdAt: LocalDateTime) {

  def validateTransactionConfig(expectedConfig: Seq[TransactionConfig]): ServiceResponse[Unit] = {
    BusinessUserApplication.validateTransactionConfig(this.transactionConfig, expectedConfig)
  }
}

