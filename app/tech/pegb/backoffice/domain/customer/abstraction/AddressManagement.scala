package tech.pegb.backoffice.domain.customer.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.customer.dto.{ContactAddressToCreate, ContactAddressToUpdate}
import tech.pegb.backoffice.domain.customer.implementation.AddressMgmtService
import tech.pegb.backoffice.domain.customer.model.ContactAddress
import tech.pegb.backoffice.domain.model

import scala.concurrent.Future

@ImplementedBy(classOf[AddressMgmtService])
trait AddressManagement {

  def getAddresses(
    userId: UUID,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[ContactAddress]]]

  def getAddressById(
    userId: UUID,
    addressId: UUID): Future[ServiceResponse[ContactAddress]]

  def insertAddress(addressToCreate: ContactAddressToCreate): Future[ServiceResponse[ContactAddress]]

  def updateAddress(userId: UUID, addressId: UUID, addressToUpdate: ContactAddressToUpdate): Future[ServiceResponse[ContactAddress]]

}
