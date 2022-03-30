package tech.pegb.backoffice.dao.contacts.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.contacts.entity.Contact._

case class ContactToUpdate(
    contactType: Option[String] = None,
    name: Option[String] = None,
    middleName: Option[String] = None,
    surname: Option[String] = None,
    phoneNumber: Option[String] = None,
    email: Option[String] = None,
    idType: Option[String] = None,
    isActive: Option[Boolean] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  contactType.foreach(v ⇒ append(cContactType → v))
  name.foreach(v ⇒ append(cName → v))
  middleName.foreach(v ⇒ append(cMidName → v))
  surname.foreach(v ⇒ append(cSurName → v))
  phoneNumber.foreach(v ⇒ append(cPhoneNum → v))
  email.foreach(v ⇒ append(cEmail → v))
  idType.foreach(v ⇒ append(cIdType → v))
  isActive.foreach(v ⇒ append(cIsActive → v))
  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)
  lastUpdatedAt.foreach(paramsBuilder += cLastUpdatedAt → _)

}
