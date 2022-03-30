package tech.pegb.backoffice.mapping.domain.dao.auth

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.auth.dto._
import tech.pegb.backoffice.dao.auth.dto.{BackOfficeUserCriteria ⇒ DaoBackOfficeUserCriteria, BackOfficeUserToInsert ⇒ DaoBackOfficeUserToCreate, BackOfficeUserToUpdate ⇒ DaoBackOfficeUserToUpdate, BusinessUnitCriteria ⇒ DaoBusinessUnitCriteria, BusinessUnitToInsert ⇒ DaoBusinessUnitToInsert, BusinessUnitToUpdate ⇒ DaoBusinessUnitToUpdate}
import tech.pegb.backoffice.dao.auth.sql.{BackOfficeUserSqlDao, BusinessUnitSqlDao}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class BusinessUnitCriteriaDaoAdapter(val arg: BusinessUnitCriteria) extends AnyVal {
    //TODO make isActive: Option[Boolean]
    def asDao(isActive: Boolean) = DaoBusinessUnitCriteria(
      id = arg.id.map(id ⇒ CriteriaField(BusinessUnitSqlDao.cId, id.toString)),
      name = arg.name.map(name ⇒ CriteriaField(BusinessUnitSqlDao.cName, name)),
      isActive = Some(CriteriaField(BusinessUnitSqlDao.cIsActive, isActive.toInt)),
      createdBy = None, createdAt = None, updatedBy = None, updatedAt = None)
  }

  implicit class BusinessUnitToUpdateDaoAdapter(val arg: BusinessUnitToUpdate) extends AnyVal {
    def asDao(isActive: Boolean, maybeMissingLastUpdatedAt: Option[LocalDateTime] = None) = DaoBusinessUnitToUpdate(
      name = arg.name,
      isActive = Some(if (isActive) 1 else 0),
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt,
      lastUpdatedAt = arg.lastUpdatedAt.orElse(maybeMissingLastUpdatedAt))
  }

  implicit class BusinessUnitToCreateDaoAdapter(val arg: BusinessUnitToCreate) extends AnyVal {
    def asDao = DaoBusinessUnitToInsert(
      name = arg.name,
      isActive = 1,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      updatedBy = None,
      updatedAt = None)
  }

  implicit class BackOfficeUserCriteriaDaoAdapter(val arg: BackOfficeUserCriteria) extends AnyVal {
    def asDao(isActive: Option[Boolean] = None, butNotThisId: Option[UUID] = None) = DaoBackOfficeUserCriteria(
      //TODO replace with actual reference to column name from BackOfficeSqlDao
      id = (arg.id, butNotThisId) match {
        case (_, Some(notThisId)) ⇒
          CriteriaField(BackOfficeUserSqlDao.cId, notThisId.toString, MatchTypes.NotSame).toOption
        case (Some(id), _) ⇒
          CriteriaField(BackOfficeUserSqlDao.cId, id.toString,
            if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact).toOption
        case _ ⇒ None
      },

      userName = arg.userName.map(id ⇒ CriteriaField(BackOfficeUserSqlDao.cUsername, id.toString,
        if (arg.partialMatchFields.contains("user_name")) MatchTypes.Partial else MatchTypes.Exact)),

      password = arg.hashedPassword.map(id ⇒ CriteriaField(BackOfficeUserSqlDao.cPassword, id.toString,
        if (arg.partialMatchFields.contains("password")) MatchTypes.Partial else MatchTypes.Exact)),

      firstName = arg.firstName.map(id ⇒ CriteriaField(BackOfficeUserSqlDao.cFName, id.toString,
        if (arg.partialMatchFields.contains("first_name")) MatchTypes.Partial else MatchTypes.Exact)),

      lastName = arg.lastName.map(id ⇒ CriteriaField(BackOfficeUserSqlDao.cLName, id.toString,
        if (arg.partialMatchFields.contains("last_name")) MatchTypes.Partial else MatchTypes.Exact)),

      email = arg.email.map(id ⇒ CriteriaField(BackOfficeUserSqlDao.cEmail, id.toString,
        if (arg.partialMatchFields.contains("email")) MatchTypes.Partial else MatchTypes.Exact)),

      phoneNumber = arg.phoneNumber.map(id ⇒ CriteriaField(BackOfficeUserSqlDao.cPhoneNumber, id.toString,
        if (arg.partialMatchFields.contains("phone_number")) MatchTypes.Partial else MatchTypes.Exact)),

      roleId = arg.roleId.map(buId ⇒ CriteriaField(BackOfficeUserSqlDao.cRoleId, buId.toString,
        if (arg.partialMatchFields.contains("role_id")) MatchTypes.Partial else MatchTypes.Exact)),

      businessUnitId = arg.businessUnitId.map(buId ⇒ CriteriaField(BackOfficeUserSqlDao.cBuId, buId.toString,
        if (arg.partialMatchFields.contains("business_unit_id")) MatchTypes.Partial else MatchTypes.Exact)),

      isActive = isActive.map(isActive ⇒ CriteriaField(BackOfficeUserSqlDao.cIsActive, isActive.toInt)))
  }

  implicit class BackOfficeUserToUpdateDaoAdapter(val arg: BackOfficeUserToUpdate) extends AnyVal {
    def asDao(isActive: Option[Boolean], maybeNewPassword: Option[String] = None, maybeMissingLastUpdatedAt: Option[LocalDateTime] = None) = DaoBackOfficeUserToUpdate(
      email = arg.email.map(_.value),
      password = maybeNewPassword,
      phoneNumber = arg.phoneNumber,
      description = arg.description,
      homePage = arg.homePage,
      activeLanguage = arg.activeLanguage,
      customData = arg.customData.map(_.toString()),
      roleId = arg.roleId.map(_.toString),
      businessUnitId = arg.businessUnitId.map(_.toString),
      isActive = isActive.map(_.toInt),
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt,
      lastUpdatedAt = arg.lastUpdatedAt.orElse(maybeMissingLastUpdatedAt))
  }

  implicit class BackOfficeUserToCreateDaoAdapter(val arg: BackOfficeUserToCreate) extends AnyVal {
    def asDao(generatedPassword: String) = DaoBackOfficeUserToCreate(
      userName = arg.userName,
      password = Some(generatedPassword),
      email = arg.email.value,
      phoneNumber = arg.phoneNumber,
      firstName = arg.firstName,
      middleName = arg.middleName,
      lastName = arg.lastName,
      description = arg.description,
      homePage = arg.homePage,
      activeLanguage = arg.activeLanguage,
      customData = arg.customData.map(_.toString()),
      lastLoginTimestamp = None,
      roleId = arg.roleId.toString,
      businessUnitId = arg.businessUnitId.toString,
      isActive = 1,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      updatedBy = arg.createdBy.toOption,
      updatedAt = arg.createdAt.toOption)
  }

}
