package tech.pegb.backoffice.domain.reconciliation.model

import java.time.LocalDateTime
import tech.pegb.backoffice.util.Implicits._

case class ExternReconResult(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    thirdParty: String,
    totalThirdPartyTxnCount: Int,
    totalThirdPartyTxnAmount: BigDecimal,
    totalTxnCount: Int,
    totalTxnAmount: BigDecimal,
    countDiff: Int,
    amountDiff: BigDecimal,
    missingThirdPartyTxnsOnUs: Seq[ThirdPartyTransaction],
    missingTxnsOnThirdParty: Seq[ReconTransaction]) {

  val (status, message) = (countDiff != 0, amountDiff != 0, missingThirdPartyTxnsOnUs.nonEmpty, missingTxnsOnThirdParty.nonEmpty) match {
    case (true, _, _, _) ⇒
      (ReconciliationStatuses.NOK,
        s"Our total transaction count do not match with $thirdParty. Their total count is ${if (totalThirdPartyTxnCount > totalTxnCount) "higher" else "lower"}")

    case (false, true, _, _) ⇒
      (ReconciliationStatuses.NOK,
        s"Our total transaction amount do not match with $thirdParty. Their total amount is ${if (totalThirdPartyTxnAmount > totalTxnAmount) "higher" else "lower"}")

    case (false, false, true, _) ⇒
      (ReconciliationStatuses.NOK,
        s"$thirdParty has transactions not in our system. ids: ${missingThirdPartyTxnsOnUs.map(_.id).defaultMkString}")

    case (false, false, false, true) ⇒
      (ReconciliationStatuses.NOK,
        s"The following transactions were not found on $thirdParty. ids: ${missingTxnsOnThirdParty.map(_.id).defaultMkString}")
    case _ ⇒
      (ReconciliationStatuses.OK, s"no discrepancies found with $thirdParty from $startDateTime to $endDateTime")
  }
}

case class ThirdPartyTransaction(
    id: String,
    refId: Option[String],
    amount: BigDecimal,
    dateTime: LocalDateTime)

case class ParsingError(
    code: String,
    description: String,
    unParsedString: String)
