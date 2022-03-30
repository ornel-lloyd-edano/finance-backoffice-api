package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.auth.sql.BackOfficeUserSqlDao._

case class BackOfficeUserToUpdate(
    userName: Option[String] = None,
    password: Option[String] = None,
    email: Option[String] = None,
    phoneNumber: Option[String] = None,
    firstName: Option[String] = None,
    middleName: Option[String] = None,
    lastName: Option[String] = None,
    description: Option[String] = None,
    homePage: Option[String] = None,
    activeLanguage: Option[String] = None,
    customData: Option[String] = None,
    lastLoginTimestamp: Option[Long] = None,
    roleId: Option[String] = None,
    businessUnitId: Option[String] = None,
    isActive: Option[Int] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime] = None) extends GenericUpdateSql {

  userName.foreach(name ⇒ append(cUsername → name))
  password.foreach(name ⇒ append(cPassword → name))
  email.foreach(name ⇒ append(cEmail → name))
  phoneNumber.foreach(name ⇒ append(cPhoneNumber → name))
  firstName.foreach(name ⇒ append(cFName → name))
  middleName.foreach(name ⇒ append(cMidName → name))
  lastName.foreach(name ⇒ append(cLName → name))
  description.foreach(name ⇒ append(cDesc → name))
  homePage.foreach(name ⇒ append(cHomePage → name))
  activeLanguage.foreach(name ⇒ append(cActiveLang → name))
  customData.foreach(name ⇒ append(cCustomData → name))
  lastLoginTimestamp.foreach(name ⇒ append(cLastLogin → name))
  roleId.foreach(name ⇒ append(cRoleId → name))
  businessUnitId.foreach(name ⇒ append(cBuId → name))
  isActive.foreach(isActive ⇒ append(cIsActive → isActive))

  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)
  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)
}

object BackOfficeUserToUpdate {
  val empty = new BackOfficeUserToUpdate(updatedBy = "", updatedAt = LocalDateTime.now)
}
