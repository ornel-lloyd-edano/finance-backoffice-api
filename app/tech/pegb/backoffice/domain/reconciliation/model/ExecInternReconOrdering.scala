package tech.pegb.backoffice.domain.reconciliation.model

import tech.pegb.backoffice.domain.model.Order

case class ExecInternReconOrdering(
    accountId: Order,
    txnId: Order,
    sequence: Order,
    txnCreatedAt: Order)
