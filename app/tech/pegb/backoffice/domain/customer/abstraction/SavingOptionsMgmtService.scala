package tech.pegb.backoffice.domain.customer.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.customer.dto.SavingOptionCriteria
import tech.pegb.backoffice.domain.customer.implementation
import tech.pegb.backoffice.domain.customer.model.GenericSavingOption

import scala.concurrent.Future

@ImplementedBy(classOf[implementation.SavingOptionsMgmtService])
trait SavingOptionsMgmtService extends BaseService {

  def getCustomerSavingOptions(
    customerId: UUID,
    criteria: Option[SavingOptionCriteria]): Future[ServiceResponse[Seq[GenericSavingOption]]]

  def getLatestVersion(savingOptions: Seq[GenericSavingOption]): Future[ServiceResponse[Option[String]]]

  def deactivateSavingOption(
    id: UUID,
    customerId: UUID,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime])(implicit requestId: UUID): Future[ServiceResponse[GenericSavingOption]]

}
