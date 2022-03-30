package tech.pegb.backoffice.domain.customer.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.customer.dto.{ContactToCreate, ContactToUpdate}
import tech.pegb.backoffice.domain.customer.implementation.ContactMgmtService
import tech.pegb.backoffice.domain.customer.model.Contact
import tech.pegb.backoffice.domain.model

import scala.concurrent.Future

@ImplementedBy(classOf[ContactMgmtService])
trait ContactManagement {

  def getContactInfo(
    userId: UUID,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Contact]]]

  def getContactInfoById(
    userId: UUID,
    contactId: UUID): Future[ServiceResponse[Contact]]

  def insertContactInfo(contactToCreate: ContactToCreate): Future[ServiceResponse[Contact]]

  def updateContactInfo(userId: UUID, contactId: UUID, contactToUpdate: ContactToUpdate): Future[ServiceResponse[Contact]]

}
