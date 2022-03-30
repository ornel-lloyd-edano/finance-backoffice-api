package tech.pegb.backoffice.core.integration.dto

import tech.pegb.backoffice.core.integration.abstraction.ManualTxnLinesToCreateCoreDto

case class ManualTxnLinesToCreate(
    primaryAccountId: Int,
    primaryExplanation: String,
    primaryDirection: String,
    secondaryAccountId: Int,
    secondaryExplanation: String,
    amount: BigDecimal) extends ManualTxnLinesToCreateCoreDto {

}
