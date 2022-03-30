package tech.pegb.backoffice.dao.businessuserapplication.dto

import java.time.{LocalDate, LocalDateTime}

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.businessuserapplication.sql.BusinessUserApplicationSqlDao._

case class BusinessUserApplicationToUpdate(
    businessName: Option[String] = None,
    brandName: Option[String] = None,
    businessCategory: Option[String] = None,
    userTier: Option[String] = None,
    businessType: Option[String] = None,
    registrationNumber: Option[String] = None,
    taxNumber: Option[String] = None,
    registrationDate: Option[LocalDate] = None,
    stage: Option[String] = None,
    status: Option[String] = None,
    explanation: Option[String] = None,
    submittedBy: Option[String] = None,
    submittedAt: Option[LocalDateTime] = None,
    checkedBy: Option[String] = None,
    checkedAt: Option[LocalDateTime] = None,
    userId: Option[Int] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)
  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)

  businessName.foreach(x ⇒ append(cBusinessName → x))
  brandName.foreach(x ⇒ append(cBrandName → x))
  businessCategory.foreach(x ⇒ append(cBusinessCategory → x))
  userTier.foreach(x ⇒ append(cUserTier → x))
  businessType.foreach(x ⇒ append(cBusinessType → x))
  registrationNumber.foreach(x ⇒ append(cRegistrationNumber → x))
  taxNumber.foreach(x ⇒ append(cTaxNumber → x))
  registrationDate.foreach(x ⇒ append(cRegistrationDate → x))
  stage.foreach(x ⇒ append(cStage → x))
  status.foreach(x ⇒ append(cStatus → x))
  explanation.foreach(x ⇒ append(cExplanation → x))
  submittedBy.foreach(x ⇒ append(cSubmittedBy → x))
  submittedAt.foreach(x ⇒ append(cSubmittedAt → x))
  checkedBy.foreach(x ⇒ append(cCheckedBy → x))
  checkedAt.foreach(x ⇒ append(cCheckedAt → x))
  userId.foreach(x ⇒ append(cUserId → x))
}
