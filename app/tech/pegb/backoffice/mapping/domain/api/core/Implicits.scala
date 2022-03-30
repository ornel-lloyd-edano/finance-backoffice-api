package tech.pegb.backoffice.mapping.domain.api.core

import tech.pegb.backoffice.core.integration.dto.{ReverseTxnRequest, TxnCancellationRequest}
import tech.pegb.backoffice.domain.transaction.dto.{TxnCancellation, TxnReversal}

object Implicits {

  implicit class CancelTxnAdapter(val arg: TxnCancellation) extends AnyVal {
    def asCoreApi = TxnCancellationRequest(
      id = arg.txnId,
      updatedBy = arg.cancelledBy,
      lastUpdatedAt = arg.lastUpdatedAt,
      reason = arg.reason)
  }

  implicit class ReverseTxnAdapter(val arg: TxnReversal) extends AnyVal {
    def asCoreApi = ReverseTxnRequest(
      id = arg.txnId,
      isFeeReversed = arg.isFeeReversed,
      updatedBy = arg.reversedBy,
      lastUpdatedAt = arg.lastUpdatedAt,
      reason = arg.reason.getOrElse(""))
  }

}
