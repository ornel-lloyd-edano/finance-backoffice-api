package tech.pegb.backoffice.mapping.api.domain.core

import tech.pegb.backoffice.core.integration.dto.ReversedTxnResponse

object Implicits {

  implicit class ReversedTxnCoreApiAdapter(val arg: ReversedTxnResponse) extends AnyVal {
    def asDomain: String = arg.reversalTransactionId.toString
  }

}
