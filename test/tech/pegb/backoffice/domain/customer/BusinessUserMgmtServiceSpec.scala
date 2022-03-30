package tech.pegb.backoffice.domain.customer

import java.sql.Connection
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import tech.pegb.backoffice.dao.address.abstraction.AddressDao
import tech.pegb.backoffice.dao.address.dto.{AddressCriteria, AddressToInsert, AddressToUpdate}
import tech.pegb.backoffice.dao.address.entity
import tech.pegb.backoffice.dao.address.entity.Address
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.CountryDao
import tech.pegb.backoffice.dao.businessuserapplication.entity.Country
import tech.pegb.backoffice.dao.contacts
import tech.pegb.backoffice.dao.contacts.abstraction.ContactsDao
import tech.pegb.backoffice.dao.contacts.dto.{ContactToInsert, ContactsCriteria}
import tech.pegb.backoffice.dao.contacts.entity.Contact
import tech.pegb.backoffice.dao.customer.abstraction.{UserDao, VelocityPortalUserDao}
import tech.pegb.backoffice.dao.customer.dto.VelocityPortalUsersCriteria
import tech.pegb.backoffice.dao.customer.entity.{GenericUser, VelocityPortalUser}
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.abstraction.{AddressManagement, BusinessUserManagement, ContactManagement}
import tech.pegb.backoffice.domain.customer.dto.{ContactAddressToCreate, ContactAddressToUpdate, ContactToCreate, ContactToUpdate, GenericUserCriteria}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn
import tech.pegb.backoffice.domain.{HttpClient, ServiceError}
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.customer.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class BusinessUserMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val userDao = stub[UserDao]
  private val velocityPortalUserDao = stub[VelocityPortalUserDao]
  private val contactsDao = stub[ContactsDao]
  private val addressDao = stub[AddressDao]
  private val countryDao = stub[CountryDao]
  private val httpClientService = stub[HttpClient]

  val businessUserService = inject[BusinessUserManagement]
  val contactService = inject[ContactManagement]
  val addressService = inject[AddressManagement]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val config = inject[AppConfig]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[UserDao].to(userDao),
      bind[VelocityPortalUserDao].to(velocityPortalUserDao),
      bind[ContactsDao].to(contactsDao),
      bind[AddressDao].to(addressDao),
      bind[CountryDao].to(countryDao),
      bind[HttpClient].to(httpClientService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val userId = UUID.randomUUID()

  val user = GenericUser(
    id = 1,
    uuid = userId.toString,
    userName = "sm_company",
    password = "pass".some,
    customerType = "business".some,
    tier = "standard".some,
    segment = None,
    subscription = None,
    email = None,
    status = "active".some,
    activatedAt = now.some,
    passwordUpdatedAt = None,
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = now.some,
    updatedBy = "pegbuser".some,

    customerName = "SM Group".some,
    businessUserFields = none,
    individualUserFields = none)

  val vp1 = VelocityPortalUser(
    id = 1,
    uuid = UUID.randomUUID().toString,
    userId = 1,
    userUUID = UUID.randomUUID().toString,
    name = "Henry",
    middleName = None,
    surname = "Sy",
    msisdn = "+639111111",
    email = "h.sy@gmail.com",
    username = "h.sy@gmail.com",
    role = "admin",
    status = "active",
    lastLoginAt = now.some,
    createdBy = "pegbuser",
    createdAt = now,
    updatedBy = "pegbuser".some,
    updatedAt = now.some)

  val vp2 = VelocityPortalUser(
    id = 2,
    uuid = UUID.randomUUID().toString,
    userId = 1,
    userUUID = UUID.randomUUID().toString,
    name = "Atsi",
    middleName = "Sy".some,
    surname = "Tan",
    msisdn = "+639222222",
    email = "atsi@gmail.com",
    username = "atsi_sy",
    role = "admin",
    status = "active",
    lastLoginAt = now.some,
    createdBy = "pegbuser",
    createdAt = now,
    updatedBy = "pegbuser".some,
    updatedAt = now.some)

  val vp3 = VelocityPortalUser(
    id = 3,
    uuid = UUID.randomUUID().toString,
    userId = 1,
    name = "Shobe",
    userUUID = UUID.randomUUID().toString,
    middleName = None,
    surname = "Sy",
    msisdn = "+639333333",
    email = "shobe.sy@gmail.com",
    username = "shobe_sy",
    role = "admin",
    status = "active",
    lastLoginAt = now.some,
    createdBy = "pegbuser",
    createdAt = now,
    updatedBy = "pegbuser".some,
    updatedAt = now.some)

  "BusinessUserMgmtService" should {

    "return velocity portal users of the user" in {
      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      val criteria = VelocityPortalUsersCriteria(
        userId = CriteriaField("user_id", 1).some)
      (velocityPortalUserDao.getVelocityPortalUsersByCriteria _).when(criteria, None, None, None)
        .returns(Right(Seq(vp1, vp2, vp3)))

      val result = businessUserService.getVelocityUsers(userId, Nil, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(vp1, vp2, vp3).map(_.asDomain.get))
      }

    }

    "return error when calling getVelocityPortalUsersByCriteria if user is not found " in {
      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Nil))

      val result = businessUserService.getVelocityUsers(userId, Nil, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.notFoundError(s"Customer $userId was not found"))
      }

    }

    "return error when calling getVelocityPortalUsersByCriteria if user is not a business user" in {
      val individualUser = user.copy(customerType = "individual".some)
      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(individualUser)))

      val result = businessUserService.getVelocityUsers(userId, Nil, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"User $userId is not a business user"))
      }

    }

    "return velocity portal users of the user in get by Id" in {
      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      val criteria = VelocityPortalUsersCriteria(
        uuid = CriteriaField(VelocityPortalUser.cUuid, vp1.uuid).some)
      (velocityPortalUserDao.getVelocityPortalUsersByCriteria _).when(criteria, None, None, None)
        .returns(Right(Seq(vp1)))

      val result = businessUserService.getVelocityUsersById(userId, UUID.fromString(vp1.uuid))

      whenReady(result) { actual ⇒
        actual mustBe Right(vp1.asDomain.get)
      }
    }

    "return contactInfo of the user" in {
      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      val contact = Contact(
        id = 1,
        uuid = UUID.randomUUID().toString,
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().toString.some,
        userId = 1.some,
        userUUID = UUID.randomUUID().toString.some,
        contactType = "owner",
        name = "Henry",
        middleName = None,
        surname = "Sy",
        phoneNumber = "+639111111",
        email = "h.sy@gmail.com",
        idType = "Not Available",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        vpUserId = vp1.id.some,
        vpUserUUID = UUID.randomUUID().toString.some,
        isActive = true)

      val contact2 = Contact(
        id = 2,
        uuid = UUID.randomUUID().toString,
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().toString.some,
        userId = 1.some,
        userUUID = UUID.randomUUID().toString.some,
        contactType = "vp",
        name = "Shobe",
        middleName = None,
        surname = "Sy",
        phoneNumber = "+639222222",
        email = "s.sy@gmail.com",
        idType = "Not Available",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        vpUserId = vp1.id.some,
        vpUserUUID = UUID.randomUUID().toString.some,
        isActive = true)

      val criteria = ContactsCriteria(userUuid = Some(CriteriaField("", userId.toString)))
      (contactsDao.getByCriteria(
        _: ContactsCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection])).when(criteria, None, None, None, None)
        .returns(Right(Seq(contact, contact2)))

      val result = contactService.getContactInfo(userId, Nil, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(contact, contact2).map(_.asDomain.get))
      }

    }

    "return contact of the user in get by Id" in {
      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      val contact = Contact(
        id = 1,
        uuid = UUID.randomUUID().toString,
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().toString.some,
        userId = 1.some,
        userUUID = UUID.randomUUID().toString.some,
        contactType = "owner",
        name = "Henry",
        middleName = None,
        surname = "Sy",
        phoneNumber = "+639111111",
        email = "h.sy@gmail.com",
        idType = "Not Available",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        vpUserId = vp1.id.some,
        vpUserUUID = UUID.randomUUID().toString.some,
        isActive = true)

      val criteria = ContactsCriteria(
        uuid = Some(CriteriaField(Contact.cUuid, contact.uuid)),
        userUuid = Some(CriteriaField("", userId.toString)))
      (contactsDao.getByCriteria(
        _: ContactsCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection])).when(criteria, None, None, None, None)
        .returns(Right(Seq(contact)))

      val result = contactService.getContactInfoById(userId, UUID.fromString(contact.uuid))

      whenReady(result) { actual ⇒
        actual mustBe Right(contact.asDomain.get)
      }
    }

    "return Right() in resetVelocityUserPin" in {
      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      val getVpCriteria = VelocityPortalUsersCriteria(
        uuid = CriteriaField(VelocityPortalUser.cUuid, vp1.uuid.toString).some)
      (velocityPortalUserDao.getVelocityPortalUsersByCriteria _).when(getVpCriteria, None, None, None)
        .returns(Right(Seq(vp1)))

      val contact = Contact(
        id = 1,
        uuid = UUID.randomUUID().toString,
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().toString.some,
        userId = 1.some,
        userUUID = UUID.randomUUID().toString.some,
        contactType = "owner",
        name = "Henry",
        middleName = None,
        surname = "Sy",
        phoneNumber = "+639111111",
        email = "h.sy@gmail.com",
        idType = "Not Available",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        vpUserId = vp1.id.some,
        vpUserUUID = UUID.randomUUID().toString.some,
        isActive = true)

      val getContactCriteria = ContactsCriteria(
        vpUserId = CriteriaField(Contact.cVpUserId, vp1.userId).some)
      (contactsDao.getByCriteria(
        _: ContactsCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection])).when(getContactCriteria, None, None, None, None)
        .returns(Right(Seq(contact)))

      (httpClientService.request(_: String, _: String, _: Option[JsValue]))
        .when("PATCH", config.ResetVelocityPortalUserPinUrl.replace("{id}", vp1.id.toString), Json.obj("reason" → "pin forgotten", "updated_by" → "pegbuser", "last_updated_at" → now.toString).some)
        .returns(Future.successful(HttpResponse(true, 204, None)))

      val result = businessUserService.resetVelocityUserPin(userId, UUID.fromString(vp1.uuid), "pin forgotten", "pegbuser", now, now.some)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }

    }

    "return created contact in create contact" in {
      val dto = ContactToCreate(
        uuid = UUID.randomUUID(),
        userUuid = userId,
        contactType = "business_owner",
        name = "John",
        middleName = None,
        surname = "Doe",
        phoneNumber = Msisdn("+97112345678"),
        email = Email("j.doe@gmail.com"),
        idType = "national_id",
        createdBy = "pegbuser",
        createdAt = now,
        isActive = true)

      val contact = Contact(
        id = 1,
        uuid = UUID.randomUUID().toString,
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().toString.some,
        userId = 1.some,
        userUUID = UUID.randomUUID().toString.some,
        contactType = "owner",
        name = "Henry",
        middleName = None,
        surname = "Sy",
        phoneNumber = "+639111111",
        email = "h.sy@gmail.com",
        idType = "Not Available",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        vpUserId = vp1.id.some,
        vpUserUUID = UUID.randomUUID().toString.some,
        isActive = true)

      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      (contactsDao.insert(_: ContactToInsert)(_: Option[Connection]))
        .when(dto.asDao, *)
        .returns(Right(contact))

      val result = contactService.insertContactInfo(dto)

      whenReady(result) { actual ⇒
        actual mustBe Right(contact.asDomain.get)
      }

    }
    "return updated contact in update contact" in {
      val dto = ContactToUpdate(
        contactType = "owner".some,
        name = "John".some,
        middleName = None,
        surname = "Doe".some,
        phoneNumber = Msisdn("+97112345678").some,
        email = Email("j.doe@gmail.com").some,
        idType = "national_id".some,
        isActive = true.some,
        updatedBy = "pegbuser",
        updatedAt = now,
        lastUpdatedAt = now.some)

      val contactId = UUID.randomUUID()

      val contact = Contact(
        id = 1,
        uuid = contactId.toString,
        buApplicationId = 1.some,
        buApplicationUUID = UUID.randomUUID().toString.some,
        userId = 1.some,
        userUUID = UUID.randomUUID().toString.some,
        contactType = "owner",
        name = "Henry",
        middleName = None,
        surname = "Sy",
        phoneNumber = "+639111111",
        email = "h.sy@gmail.com",
        idType = "Not Available",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        vpUserId = vp1.id.some,
        vpUserUUID = UUID.randomUUID().toString.some,
        isActive = true)

      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      val criteria = ContactsCriteria(
        uuid = Some(CriteriaField(Contact.cUuid, contact.uuid)),
        userUuid = Some(CriteriaField("", userId.toString)))
      (contactsDao.getByCriteria(
        _: ContactsCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection])).when(criteria, None, None, None, None)
        .returns(Right(Seq(contact)))

      (contactsDao.update(_: String, _: contacts.dto.ContactToUpdate)(_: Option[Connection]))
        .when(contactId.toString, dto.asDao, *)
        .returns(Right(contact.some))

      val result = contactService.updateContactInfo(userId, contactId, dto)

      whenReady(result) { actual ⇒
        actual mustBe Right(contact.asDomain.get)
      }

    }

    "return address of the user" in {
      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      val address = Address(
        id = 1,
        uuid = UUID.randomUUID().toString,
        buApplicationId = 1.some,
        buApplicationUuid = UUID.randomUUID().toString.some,
        userId = 1.some,
        userUuid = UUID.randomUUID().toString.some,
        addressType = "primary_address",
        countryId = 1,
        countryName = "Kenya",
        city = "Nairobi",
        postalCode = "10100".some,
        address = "kinhasa road nairobi business center".some,
        coordinateX = BigDecimal("-1.29044").some,
        coordinateY = BigDecimal("36.816472").some,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        isActive = true)

      val address2 = Address(
        id = 2,
        uuid = UUID.randomUUID().toString,
        buApplicationId = 1.some,
        buApplicationUuid = UUID.randomUUID().toString.some,
        userId = 1.some,
        userUuid = UUID.randomUUID().toString.some,
        addressType = "branch_address",
        countryId = 1,
        countryName = "UAE",
        city = "Dubai",
        postalCode = "00000".some,
        address = "dubai silicon oasis".some,
        coordinateX = None,
        coordinateY = None,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        isActive = true)

      val criteria = AddressCriteria(userUuid = Some(CriteriaField(Address.cUsrUuid, userId.toString)))
      (addressDao.getByCriteria(
        _: AddressCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection])).when(criteria, None, None, None, None)
        .returns(Right(Seq(address, address2)))

      val result = addressService.getAddresses(userId, Nil, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(address, address2).map(_.asDomain.get))
      }

    }

    "return address of the user by address_id" in {

      val addressId = UUID.randomUUID()

      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      val address = Address(
        id = 1,
        uuid = addressId.toString,
        buApplicationId = 1.some,
        buApplicationUuid = UUID.randomUUID().toString.some,
        userId = 1.some,
        userUuid = userId.toString.some,
        addressType = "primary_address",
        countryId = 1,
        countryName = "Kenya",
        city = "Nairobi",
        postalCode = "10100".some,
        address = "kinhasa road nairobi business center".some,
        coordinateX = BigDecimal("-1.29044").some,
        coordinateY = BigDecimal("36.816472").some,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        isActive = true)

      val criteria = AddressCriteria(
        userUuid = Some(CriteriaField(Address.cUsrUuid, userId.toString)),
        uuid = Some(CriteriaField(Address.cUuid, addressId.toString)))

      (addressDao.getByCriteria(
        _: AddressCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection])).when(criteria, None, None, None, None)
        .returns(Right(Seq(address)))

      val result = addressService.getAddressById(userId, addressId)

      whenReady(result) { actual ⇒
        actual mustBe Right(address.asDomain.get)
      }

    }

    "return created address in create address" in {
      val dto = ContactAddressToCreate(
        uuid = UUID.randomUUID(),
        userUuid = userId,
        addressType = "primary_address",
        country = "UAE",
        city = "Dubai",
        postalCode = "00000".some,
        address = "Dubai Silicon Oasis".some,
        coordinateX = None,
        coordinateY = None,
        createdBy = "pegbuser",
        createdAt = now,
        isActive = true)

      val address = entity.Address(
        id = 1,
        uuid = UUID.randomUUID().toString,
        buApplicationId = None,
        buApplicationUuid = None,
        userId = 1.some,
        userUuid = UUID.randomUUID().toString.some,
        addressType = "primary_address",
        countryId = 1,
        countryName = "UAE",
        city = "Dubai",
        postalCode = "00000".some,
        address = "Dubai Silicon Oasis".some,
        coordinateX = None,
        coordinateY = None,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        isActive = true)

      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      (addressDao.insert(_: AddressToInsert)(_: Option[Connection]))
        .when(dto.asDao, *)
        .returns(Right(address))

      val result = addressService.insertAddress(dto)

      whenReady(result) { actual ⇒
        actual mustBe Right(address.asDomain.get)
      }

    }

    "return updated contact in update address" in {
      val dto = ContactAddressToUpdate(
        addressType = "primary_address".some,
        country = "UAE".some,
        city = "Dubai".some,
        postalCode = "00000".some,
        address = "DSO HQ WING A".some,
        coordinateX = BigDecimal("2333.32").some,
        coordinateY = BigDecimal("12314.32").some,
        isActive = true.some,
        updatedBy = "pegbuser",
        updatedAt = now,
        lastUpdatedAt = now.some)

      val address = entity.Address(
        id = 1,
        uuid = UUID.randomUUID().toString,
        buApplicationId = None,
        buApplicationUuid = None,
        userId = 1.some,
        userUuid = UUID.randomUUID().toString.some,
        addressType = "primary_address",
        countryId = 1,
        countryName = "UAE",
        city = "Dubai",
        postalCode = "00000".some,
        address = "Dubai Silicon Oasis".some,
        coordinateX = None,
        coordinateY = None,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some,
        isActive = true)

      val addressId = UUID.randomUUID()

      (userDao.getUserByCriteria _).when(GenericUserCriteria(userId = UUIDLike(userId.toString).some).asDao, None, None, None)
        .returns(Right(Seq(user)))

      (countryDao.getCountries _).when()
        .returns(Right(Seq(Country(
          id = 1,
          name = "UAE",
          createdBy = "pegbuser",
          createdAt = now))))

      (addressDao.update(_: String, _: AddressToUpdate)(_: Option[Connection]))
        .when(addressId.toString, dto.asDao(1.some), *)
        .returns(Right(address.some))

      val result = addressService.updateAddress(userId, addressId, dto)

      whenReady(result) { actual ⇒
        actual mustBe Right(address.asDomain.get)
      }

    }
  }
}
