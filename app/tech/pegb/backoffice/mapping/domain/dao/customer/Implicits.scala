package tech.pegb.backoffice.mapping.domain.dao.customer

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.dao.address.dto.{AddressCriteria, AddressToInsert, AddressToUpdate}
import tech.pegb.backoffice.dao.address.entity.Address
import tech.pegb.backoffice.dao.contacts.dto.{ContactToInsert, ContactToUpdate, ContactsCriteria}
import tech.pegb.backoffice.dao.contacts.entity.Contact
import tech.pegb.backoffice.dao.customer.dto.VelocityPortalUsersCriteria
import tech.pegb.backoffice.dao.customer.entity.VelocityPortalUser
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.customer.dto
import tech.pegb.backoffice.domain.customer.dto.{ContactAddressToCreate, ContactToCreate}
import tech.pegb.backoffice.mapping.domain.dao.AsDaoAdapter
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.{dao, domain}

object Implicits {

  implicit class IndividualUserCriteriaMapper(val arg: domain.customer.dto.IndividualUserCriteria) extends AnyVal {
    def asDao = dao.customer.dto.IndividualUserCriteria(
      userUuid = arg.userId.map(userId ⇒
        CriteriaField("uuid", userId.underlying,
          if (arg.partialMatchFields.contains("user_id")) MatchTypes.Partial else MatchTypes.Exact)),

      msisdn = arg.msisdnLike.map(msisdn ⇒
        CriteriaField("msisdn", msisdn.underlying,
          if (arg.partialMatchFields.contains("msisdn")) MatchTypes.Partial else MatchTypes.Exact)),

      tier = arg.tier.map(_.underlying),
      segment = arg.segment.map(_.underlying),
      subscription = arg.subscription.map(_.underlying),
      status = arg.status.map(_.underlying),
      individualUserType = arg.individualUserType.map(_.underlying),
      name = arg.name.map(_.underlying),

      fullName = arg.fullName.map(fullName ⇒
        CriteriaField("full_name", fullName.underlying,
          if (arg.partialMatchFields.contains("full_name")) MatchTypes.Partial else MatchTypes.Exact)),

      gender = arg.gender.map(_.underlying),
      personId = arg.personId.map(_.underlying),
      documentNumber = arg.documentNumber.map(_.underlying),
      documentType = arg.documentType.map(_.underlying),
      birthDate = arg.birthDate,
      birthPlace = arg.birthPlace.map(_.underlying),
      nationality = arg.nationality.map(_.underlying),
      occupation = arg.occupation.map(_.underlying),
      company = arg.companyName.map(_.underlying),
      employer = arg.employer.map(_.underlying),
      createdDateFrom = arg.createdDateFrom,
      createdDateTo = arg.createdDateTo,
      updatedDateFrom = arg.updatedDateFrom,
      updatedDateTo = arg.updatedDateTo)
  }

  implicit class IndividualUserToUpdateAdapter(arg: domain.customer.dto.IndividualUserToUpdate) {
    def asDao: dao.customer.dto.IndividualUserToUpdate = {
      dao.customer.dto.IndividualUserToUpdate(
        `type` = arg.individualUserType.map(_.underlying.toLowerCase),
        msisdn = arg.msisdn.map(_.underlying),
        name = arg.name.map(_.underlying),
        fullName = arg.fullName.map(_.underlying),
        gender = arg.gender.map(_.underlying),
        personId = arg.personId.map(_.underlying),
        documentNumber = arg.documentNumber.map(_.underlying),
        documentType = arg.documentType.map(_.underlying),
        company = arg.companyName.map(_.underlying),
        birthDate = arg.birthDate,
        birthPlace = arg.birthPlace.map(_.underlying),
        nationality = arg.nationality.map(_.underlying),
        occupation = arg.occupation.map(_.underlying),
        employer = arg.employer.map(_.underlying),
        updatedAt = None,
        updatedBy = None)
    }
  }

  implicit class GenericUserCriteriaMapper(val arg: domain.customer.dto.GenericUserCriteria) extends AnyVal {
    def asDao = dao.customer.dto.GenericUserCriteria(
      userUuid = arg.userId.map(userId ⇒
        CriteriaField("uuid", userId.underlying,
          if (arg.partialMatchFields.contains("user_id")) MatchTypes.Partial else MatchTypes.Exact)),

      msisdn = arg.msisdnLike.map(msisdn ⇒
        CriteriaField("msisdn", msisdn.underlying,
          if (arg.partialMatchFields.contains("msisdn")) MatchTypes.Partial else MatchTypes.Exact)),

      tier = arg.tier.map(x ⇒ CriteriaField("", x.underlying)),
      segment = arg.segment.map(x ⇒ CriteriaField("", x.underlying)),
      subscription = arg.subscription.map(x ⇒ CriteriaField("", x.underlying)),
      status = arg.status.map(x ⇒ CriteriaField("", x.underlying)),
      customerType = arg.customerType.map(x ⇒ CriteriaField("", x.underlying)),
      name = arg.name.map(x ⇒
        CriteriaField("name", x.underlying,
          if (arg.partialMatchFields.contains("name")) MatchTypes.Partial else MatchTypes.Exact)),
      fullName = arg.fullName.map(fullName ⇒
        CriteriaField("full_name", fullName.underlying,
          if (arg.partialMatchFields.contains("full_name")) MatchTypes.Partial else MatchTypes.Exact)),

      gender = arg.gender.map(x ⇒ CriteriaField("", x.underlying)),
      personId = arg.personId.map(x ⇒ CriteriaField("", x.underlying)),
      documentNumber = arg.documentNumber.map(x ⇒ CriteriaField("", x.underlying)),
      documentType = arg.documentType.map(x ⇒ CriteriaField("", x.underlying)),
      birthDate = arg.birthDate.map(x ⇒ CriteriaField("", x)),
      birthPlace = arg.birthPlace.map(x ⇒ CriteriaField("", x.underlying)),
      nationality = arg.nationality.map(x ⇒ CriteriaField("", x.underlying)),
      occupation = arg.occupation.map(x ⇒ CriteriaField("", x.underlying)),
      company = arg.companyName.map(x ⇒ CriteriaField("", x.underlying)),
      employer = arg.employer.map(x ⇒ CriteriaField("", x.underlying)),
      createdAt = (arg.createdDateFrom, arg.createdDateTo) match {
        case (Some(createdAtFrom), Some(createdAtTo)) ⇒
          CriteriaField[(LocalDateTime, LocalDateTime)]("created_at", (createdAtFrom.atStartOfDay(), createdAtTo.atEndOfDay), MatchTypes.InclusiveBetween).toOption
        case (Some(createdAtFrom), None) ⇒
          CriteriaField[LocalDateTime]("created_at", createdAtFrom.atStartOfDay(), MatchTypes.GreaterOrEqual).toOption
        case (None, Some(createdAtTo)) ⇒
          CriteriaField[LocalDateTime]("created_at", createdAtTo.atEndOfDay, MatchTypes.LesserOrEqual).toOption
        case _ ⇒ None
      },
      updatedAt = (arg.updatedDateFrom, arg.updatedDateTo) match {
        case (Some(updatedAtFrom), Some(updatedAtTo)) ⇒
          CriteriaField[(LocalDateTime, LocalDateTime)]("updated_at", (updatedAtFrom.atStartOfDay, updatedAtTo.atEndOfDay), MatchTypes.InclusiveBetween).toOption
        case (Some(updatedAtFrom), None) ⇒
          CriteriaField[LocalDateTime]("updated_at", updatedAtFrom.atStartOfDay, MatchTypes.GreaterOrEqual).toOption
        case (None, Some(updatedAtTo)) ⇒
          CriteriaField[LocalDateTime]("updated_at", updatedAtTo.atEndOfDay, MatchTypes.LesserOrEqual).toOption
        case _ ⇒ None
      },
      anyName = arg.anyName.map(x ⇒
        CriteriaField("", x.underlying,
          if (arg.partialMatchFields.contains("any_name")) MatchTypes.Partial else MatchTypes.Exact)))
  }

  implicit object UserIdToVelocityDaoCriteriaMapper extends AsDaoAdapter[Int, VelocityPortalUsersCriteria] {
    override def asDao(arg: Int): VelocityPortalUsersCriteria = {
      VelocityPortalUsersCriteria(
        userId = Some(CriteriaField(VelocityPortalUser.cUserId, arg)))
    }
  }

  implicit class UserIdToDaoCriteriaMapper(val arg: Int) extends AnyVal {
    def userIdToDaoCriteria[T](implicit asDaoAdapter: AsDaoAdapter[Int, T]): T = {
      asDaoAdapter.asDao(arg)
    }
  }

  implicit class UserUUIDToDaoCriteriaMapper(val arg: UUID) extends AnyVal {
    def userUUIDToDaoCriteria[T](implicit asDaoAdapter: AsDaoAdapter[UUID, T]): T = {
      asDaoAdapter.asDao(arg)
    }
  }

  implicit object UUIDToVelocityDaoCriteriaMapper extends AsDaoAdapter[UUID, VelocityPortalUsersCriteria] {
    override def asDao(arg: UUID): VelocityPortalUsersCriteria = {
      VelocityPortalUsersCriteria(
        uuid = Some(CriteriaField(VelocityPortalUser.cUuid, arg.toString)))
    }
  }

  implicit class UUIDToDaoCriteriaMapper(val arg: UUID) extends AnyVal {
    def asDao[T](implicit asDaoAdapter: AsDaoAdapter[UUID, T]): T = {
      asDaoAdapter.asDao(arg)
    }
  }

  implicit class ContactVpUserIdToDaoCriteriaMapper(val arg: dto.ContactsCriteria) extends AnyVal {
    def asDao: ContactsCriteria = {
      ContactsCriteria(
        uuid = arg.uuid.map(x ⇒ CriteriaField(Contact.cUuid, x.toString)),
        userUuid = arg.userUuid.map(x ⇒ CriteriaField("", x.toString)),
        buApplicUuid = arg.buApplicUuid.map(x ⇒ CriteriaField("", x.toString)),
        contactType = arg.contactType.map(x ⇒ CriteriaField(Contact.cContactType, x)),
        name = arg.name.map(x ⇒ CriteriaField(Contact.cName, x)),
        middleName = arg.middleName.map(x ⇒ CriteriaField(Contact.cMidName, x)),
        surname = arg.surname.map(x ⇒ CriteriaField(Contact.cSurName, x)),
        phoneNumber = arg.phoneNumber.map(x ⇒ CriteriaField(Contact.cPhoneNum, x)),
        email = arg.email.map(x ⇒ CriteriaField(Contact.cEmail, x)),
        idType = arg.idType.map(x ⇒ CriteriaField(Contact.cIdType, x)),
        isActive = arg.isActive.map(x ⇒ CriteriaField(Contact.cIsActive, x)),
        vpUserId = arg.vpUserId.map(x ⇒ CriteriaField(Contact.cVpUserId, x)),
        vpUserUUID = arg.vpUserUUID.map(x ⇒ CriteriaField("", x)))
    }
  }

  implicit class ContactToCreateDaoMapper(val arg: ContactToCreate) extends AnyVal {
    def asDao: ContactToInsert = {
      ContactToInsert(
        uuid = arg.uuid.toString,
        userUuid = arg.userUuid.toString,
        contactType = arg.contactType,
        name = arg.name,
        middleName = arg.middleName,
        surname = arg.surname,
        phoneNumber = arg.phoneNumber.underlying,
        email = arg.email.value,
        idType = arg.idType,
        createdBy = arg.createdBy,
        createdAt = arg.createdAt,
        isActive = arg.isActive)
    }
  }

  implicit class ContactToUpdateDaoMapper(val arg: dto.ContactToUpdate) extends AnyVal {
    def asDao: ContactToUpdate = {
      ContactToUpdate(
        contactType = arg.contactType,
        name = arg.name,
        middleName = arg.middleName,
        surname = arg.surname,
        phoneNumber = arg.phoneNumber.map(_.underlying),
        email = arg.email.map(_.value),
        idType = arg.idType,
        isActive = arg.isActive,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt,
        lastUpdatedAt = arg.lastUpdatedAt)
    }
  }

  implicit class AddressToDaoCriteriaMapper(val arg: dto.AddressCriteria) extends AnyVal {
    def asDao: AddressCriteria = {
      AddressCriteria(
        uuid = arg.uuid.map(x ⇒ CriteriaField(Address.cUuid, x.toString)),
        buApplicationUuid = arg.buApplicationUuid.map(x ⇒ CriteriaField(Address.cBuApplicId, x.toString)),
        userUuid = arg.userUuid.map(x ⇒ CriteriaField(Address.cUsrUuid, x.toString)),
        addressType = arg.addressType.map(x ⇒ CriteriaField(Address.cAddressType, x)),
        countryName = arg.countryName.map(x ⇒ CriteriaField(Address.cCountry, x)),
        city = arg.city.map(x ⇒ CriteriaField(Address.cCity, x)),
        postalCode = arg.postalCode.map(x ⇒ CriteriaField(Address.cPostalCode, x)),
        address = arg.address.map(x ⇒ CriteriaField(Address.cAddress, x)),
        coordinateX = arg.coordinateX.map(x ⇒ CriteriaField(Address.cCoordX, x)),
        coordinateY = arg.coordinateY.map(x ⇒ CriteriaField(Address.cCoordY, x)),
        createdBy = arg.createdBy.map(x ⇒ CriteriaField(Address.cCreatedBy, x)),
        createdAt = arg.createdAt.map(x ⇒ CriteriaField(Address.cCreatedAt, x)),
        updatedBy = arg.updatedBy.map(x ⇒ CriteriaField(Address.cUpdatedBy, x)),
        updatedAt = arg.updatedAt.map(x ⇒ CriteriaField(Address.cUpdatedAt, x)),
        isActive = arg.isActive.map(x ⇒ CriteriaField(Address.cIsActive, x)))
    }
  }

  implicit class ContactAddressToCreateDaoMapper(val arg: ContactAddressToCreate) extends AnyVal {
    def asDao: AddressToInsert = {
      AddressToInsert(
        uuid = arg.uuid.toString,
        userUuid = arg.userUuid.toString,
        addressType = arg.addressType,
        country = arg.country,
        city = arg.city,
        postalCode = arg.postalCode,
        address = arg.address,
        coordinateX = arg.coordinateX,
        coordinateY = arg.coordinateY,
        createdBy = arg.createdBy,
        createdAt = arg.createdAt,
        isActive = arg.isActive)
    }
  }

  implicit class ContactAddressToUpdateDaoMapper(val arg: dto.ContactAddressToUpdate) extends AnyVal {
    def asDao(countryId: Option[Int]): AddressToUpdate = {
      AddressToUpdate(
        addressType = arg.addressType,
        countryId = countryId,
        city = arg.city,
        postalCode = arg.postalCode,
        address = arg.address,
        coordinateX = arg.coordinateX,
        coordinateY = arg.coordinateY,
        isActive = arg.isActive,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt,
        lastUpdatedAt = arg.lastUpdatedAt)
    }
  }

}
