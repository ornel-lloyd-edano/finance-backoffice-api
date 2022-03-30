package tech.pegb.backoffice.dao.contacts.dto
import java.time.LocalDateTime

case class ContactToInsert(
    uuid: String,
    userUuid: String,
    contactType: String,
    name: String,
    middleName: Option[String],
    surname: String,
    phoneNumber: String,
    email: String,
    idType: String,
    createdBy: String,
    createdAt: LocalDateTime,
    isActive: Boolean) {

}
