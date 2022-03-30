package tech.pegb.backoffice.core.integration.dto

import tech.pegb.backoffice.core.integration.abstraction.ManualTxnLinesToCreateCoreDto

case class ManualTxnFxLinesToCreate(
    primaryAccountId: Int,
    primaryAmount: BigDecimal,
    primaryExplanation: String,
    secondaryAccountId: Int,
    secondaryAmount: BigDecimal,
    secondaryExplanation: String) extends ManualTxnLinesToCreateCoreDto {

}

