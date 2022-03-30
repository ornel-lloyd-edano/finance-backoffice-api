package tech.pegb.backoffice.domain.limit.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.{BaseService, ServiceError}
import tech.pegb.backoffice.domain.limit.implementation.LimitMgmtServiceValidation
import tech.pegb.backoffice.domain.limit.model.{LimitProfile, TimeInterval}

@ImplementedBy(classOf[LimitMgmtServiceValidation])
trait LimitManagementValidation extends BaseService {
  def validateCurrentLimitWithExistingLimit(
    interval: Option[TimeInterval],
    maxIntervalAmount: Option[BigDecimal],
    maxIntervalCount: Option[Int],
    matchingLimitProfiles: Seq[LimitProfile])(
    implicit
    requestId: UUID): Option[ServiceError]

}
