package tech.pegb.backoffice.domain.customer

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.customer.abstraction._
import tech.pegb.backoffice.dao.customer.entity
import tech.pegb.backoffice.dao.customer.entity.{BusinessUser, GenericUser}
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.dto.GenericUserCriteria
import tech.pegb.backoffice.domain.customer.implementation.CustomerReadService
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.TestExecutionContext
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.IndividualUser
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._

import scala.concurrent.duration._

class CustomerReadServiceSpec extends PlaySpec with MockFactory with ScalaFutures {

  override implicit val patienceConfig = PatienceConfig(timeout = 5.seconds)
  implicit val timeout = Timeout(patienceConfig.timeout)

  val config = AppConfig("application.test.conf")
  val executionContexts = TestExecutionContext
  val mockBusinessUserDao = stub[BusinessUserDao]
  val mockAccountDao = stub[AccountDao]
  val mockCustomerDao = stub[CustomerAttributesDao]
  val mockCustomerExtraAttributeDao = stub[CustomerExtraAttributeDao]
  val mockIndividualUserDao = stub[IndividualUserDao]
  val mockUserDao = stub[UserDao]

  val customerReadService = new CustomerReadService(config, executionContexts, mockUserDao, mockBusinessUserDao,
    mockIndividualUserDao, mockAccountDao, mockCustomerExtraAttributeDao, mockCustomerDao)

  "CustomerReadService fetching individual users" should {
    "get individual user by id" in {
      val mockCustomerId = UUID.randomUUID()
      val mockIndividualUser = IndividualUser
        .getEmpty.copy(
          id = mockCustomerId,
          msisdn = Msisdn("+971544451345"),
          name = Some("George"),
          fullName = Some("George Ogalo"),
          occupation = Some(NameAttribute("Technical Account Manager")),
          employer = Some(NameAttribute("PEGB")),
          createdBy = Some("pegbuser"))

      val mockUserJoinIndividualUser = entity.IndividualUser.getEmpty.copy(
        uuid = mockIndividualUser.id.toString,
        msisdn = mockIndividualUser.msisdn.underlying,
        name = mockIndividualUser.name,
        fullName = mockIndividualUser.fullName,
        employer = mockIndividualUser.employer.map(_.underlying),
        occupation = mockIndividualUser.occupation.map(_.underlying),
        createdBy = "pegbuser")

      (mockIndividualUserDao.getIndividualUser _)
        .when(mockCustomerId.toString).returns(Right(Some(mockUserJoinIndividualUser)))

      /*val individualUserDao = new IndividualUserMockDao {
        override def getUserAndIndividualUserJoin(id: String): DaoResponse[Option[UserAndIndividualUserJoin]] = {
          Right(Some(mockUserJoinIndividualUser))
        }
      }
      val customerReadService = new CustomerReadService(config, executionContexts, mockBusinessUserDao, individualUserDao,
        mockAccountDao, mockCustomerExtraAttributeDao, mockCustomerDao)*/

      val result = customerReadService.getIndividualUser(mockCustomerId)

      whenReady(result) { individualUser ⇒
        //println(individualUser.left.get)
        individualUser.isRight mustBe true
        individualUser.right.get.id mustBe mockCustomerId
        individualUser.right.get.msisdn mustBe mockIndividualUser.msisdn
        individualUser.right.get.name mustBe mockIndividualUser.name
        individualUser.right.get.fullName mustBe mockIndividualUser.fullName
        individualUser.right.get.employer mustBe mockIndividualUser.employer
        individualUser.right.get.occupation mustBe mockIndividualUser.occupation
      }
    }

    "get service error 'NotFoundError' if individual user was not found" in {
      val unknownCustomerId = UUID.randomUUID()
      (mockIndividualUserDao.getIndividualUser _)
        .when(unknownCustomerId.toString).returns(Right(None))

      val result = customerReadService.getIndividualUser(unknownCustomerId)

      val expected = ServiceError.notFoundError(s"IndividualUser ${unknownCustomerId.toString} was not found", UUID.randomUUID().toOption)
      whenReady(result) { individualUser ⇒

        individualUser.left.get.equals(expected) mustBe true
      }
    }

    "get service error 'DTOMappingError' if individual user was found in DB but cannot be parsed" in {
      val mockCustomerId = UUID.randomUUID()
      val mockIndividualUser = IndividualUser
        .getEmpty.copy(
          id = mockCustomerId,
          msisdn = Msisdn("+971544451345"),
          name = Some("George"),
          fullName = Some("George Ogalo"),
          occupation = Some(NameAttribute("Technical Account Manager")),
          employer = Some(NameAttribute("PEGB")),
          createdBy = Some("pegbuser"))

      val mockIndividualUserFromDBWithBadMsisdn = entity.IndividualUser.getEmpty.copy(
        uuid = mockIndividualUser.id.toString,
        msisdn = "q73yhjiuweryq3iug", //bad msisdn
        name = mockIndividualUser.name,
        fullName = mockIndividualUser.fullName,
        employer = mockIndividualUser.employer.map(_.underlying),
        occupation = mockIndividualUser.occupation.map(_.underlying),
        createdBy = "pegbuser")

      (mockIndividualUserDao.getIndividualUser _)
        .when(mockCustomerId.toString).returns(Right(Some(mockIndividualUserFromDBWithBadMsisdn)))

      val result = customerReadService.getIndividualUser(mockCustomerId)

      val expected = ServiceError.dtoMappingError(s"IndividualUser ${mockCustomerId.toString} could not be converted to domain object", UUID.randomUUID().toOption)
      whenReady(result) { individualUser ⇒
        individualUser.left.get.equals(expected) mustBe true
      }
    }

    "get all generic users in getUsersByCriteria" in {
      val testUuidOne = UUID.randomUUID().toString
      val testUuidThree = UUID.randomUUID().toString
      val testUuid5 = UUID.randomUUID().toString

      val iu1 = entity.IndividualUser(
        id = 1,
        uuid = s"$testUuidOne",
        username = Some("user01"),
        password = Some("pword"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("alice@gmail.com"),
        status = "ACTIVE",
        msisdn = "971544465329",
        `type` = Some("type_one"),
        name = "Alice".some,
        fullName = Some("Alice Smith"),
        gender = Some("F"),
        personId = None,
        documentNumber = None,
        documentType = None,
        company = Some("PegB"),
        birthDate = Some(LocalDate.of(1990, 1, 1)),
        birthPlace = Some("Dubai"),
        nationality = Some("Emirati"),
        occupation = Some("Manager"),
        employer = Some("EMAAR"),
        createdAt = LocalDateTime.of(2018, 10, 1, 0, 0, 0, 0),
        createdBy = "SuperUser",
        updatedAt = None,
        updatedBy = None,
        activatedAt = Some(LocalDateTime.of(2018, 10, 1, 0, 0)))

      val bu1 = BusinessUser(
        id = 1,
        uuid = testUuidOne,
        userId = 3,
        userUUID = UUID.randomUUID().toString,
        businessName = "PegB",
        brandName = "PegB",
        businessCategory = "business_category",
        businessType = "business_type",

        registrationNumber = "registration_number",
        taxNumber = "tax_number".some,
        registrationDate = LocalDate.of(1970, 1, 1).some,
        currencyId = 1,
        collectionAccountId = None,
        collectionAccountUUID = None,
        distributionAccountId = None,
        distributionAccountUUID = None,
        createdBy = "SuperUser",
        createdAt = LocalDateTime.of(2018, 10, 14, 0, 0),
        updatedAt = LocalDateTime.of(2018, 10, 14, 0, 0).some,
        updatedBy = "pegbuser".some)

      val genericUser1 = GenericUser(
        id = 1,
        uuid = testUuidOne,
        userName = "user01",
        password = Some("pword"),
        customerType = Some("type_one"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("alice@gmail.com"),
        status = Some("ACTIVE"),
        activatedAt = Option(LocalDateTime.of(2018, 10, 1, 0, 0, 0)),
        passwordUpdatedAt = None,
        createdAt = LocalDateTime.of(2018, 10, 1, 0, 0, 0),
        createdBy = "SuperUser",
        updatedAt = None,
        updatedBy = None,

        customerName = "Alice".some,
        businessUserFields = none,
        individualUserFields = iu1.some)

      val genericUser3 = GenericUser(
        id = 3,
        uuid = testUuidThree,
        userName = "david_merchant",
        password = Some("pword03"),
        customerType = Some("type_one"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("david@gmail.com"),
        status = Some("ACTIVE"),
        activatedAt = Option(LocalDateTime.of(2018, 10, 15, 0, 0, 0)),
        passwordUpdatedAt = None,
        createdAt = LocalDateTime.of(2018, 10, 16, 0, 0, 0),
        createdBy = "SuperUser",
        updatedAt = None,
        updatedBy = None,

        customerName = "PegB".some,
        businessUserFields = bu1.some,
        individualUserFields = none)

      val genericUser5 = GenericUser(
        id = 5,
        uuid = testUuid5,
        userName = "cybersource",
        password = Some("pword05"),
        customerType = Some("business_user"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("cybersource@gmail.com"),
        status = Some("ACTIVE"),
        activatedAt = Option(LocalDateTime.of(2018, 10, 14, 0, 0, 0)),
        passwordUpdatedAt = None,
        createdAt = LocalDateTime.of(2018, 10, 16, 0, 0, 0),
        createdBy = "SuperUser",
        updatedAt = None,
        updatedBy = None,

        customerName = "cybersource".some,
        businessUserFields = none,
        individualUserFields = none)

      val criteria = GenericUserCriteria()
      val ordering = Seq(Ordering("created_at", Ordering.DESCENDING))

      (mockUserDao.getUserByCriteria _)
        .when(criteria.asDao, ordering.asDao, None, None)
        .returns(Right(Seq(genericUser1, genericUser3, genericUser5)))

      val result = customerReadService.getUserByCriteria(criteria, ordering, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(genericUser1, genericUser3, genericUser5).flatMap(_.asDomain.toOption))
      }
    }

    "get all generic users in getUsersByCriteria filter any_name" in {
      val testUuidThree = UUID.randomUUID()
      val genericUser3 = GenericUser(
        id = 3,
        uuid = testUuidThree.toString,
        userName = "david_merchant",
        password = "pword03".some,
        customerType = "individual_user".some,
        tier = "tier".some,
        segment = "segment1".some,
        subscription = "sub_one".some,
        email = "david@gmail.com".some,
        status = "active".some,
        activatedAt = LocalDateTime.of(2018, 10, 15, 0, 0, 0).some,
        passwordUpdatedAt = None,
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0),
        createdBy = "pegbuser",
        updatedAt = LocalDateTime.of(2018, 10, 16, 0, 0, 0).some,
        updatedBy = "pegbuser".some,

        customerName = "PegB".some,
        businessUserFields = none,
        individualUserFields = none)

      val domainG3 = model.GenericUser(
        dbUserId = 3,
        id = testUuidThree,
        userName = LoginUsername("david_merchant").some,
        password = "pword03".some,
        tier = CustomerTier("tier").some,
        segment = CustomerSegment("segment1").some,
        subscription = CustomerSubscription("sub_one").some,
        email = Email("david@gmail.com").some,
        status = CustomerStatus("active").some,
        customerType = CustomerType("individual_user").some,
        createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0),
        createdBy = "pegbuser",
        updatedAt = LocalDateTime.of(2018, 10, 16, 0, 0, 0).some,
        updatedBy = "pegbuser".some,
        passwordUpdatedAt = None,
        activatedAt = LocalDateTime.of(2018, 10, 15, 0, 0, 0).some,
        customerName = "PegB".some,

        msisdn = None,
        individualUserType = None,
        name = None,
        fullName = None,
        gender = None,
        personId = None,
        documentNumber = None,
        documentType = None,
        documentModel = None,
        birthDate = None,
        birthPlace = None,
        nationality = None,
        occupation = None,
        companyName = None,
        employer = None,

        businessName = None,
        brandName = None,
        businessCategory = None,
        businessType = None,
        registrationNumber = None,
        taxNumber = None,
        registrationDate = None)

      val criteria = GenericUserCriteria()
      val ordering = Seq(Ordering("created_at", Ordering.DESCENDING))

      (mockUserDao.getUserByCriteria _)
        .when(criteria.asDao, ordering.asDao, None, None)
        .returns(Right(Seq(genericUser3)))

      val result = customerReadService.getUserByCriteria(criteria, ordering, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(domainG3))
      }
    }

    "get generic user matching the id in getUser" in {
      val testUuidOne = UUID.randomUUID().toString

      val iu1 = entity.IndividualUser(
        id = 1,
        uuid = s"$testUuidOne",
        username = Some("user01"),
        password = Some("pword"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("alice@gmail.com"),
        status = "ACTIVE",
        msisdn = "971544465329",
        `type` = Some("type_one"),
        name = "Alice".some,
        fullName = Some("Alice Smith"),
        gender = Some("F"),
        personId = None,
        documentNumber = None,
        documentType = None,
        company = Some("PegB"),
        birthDate = Some(LocalDate.of(1990, 1, 1)),
        birthPlace = Some("Dubai"),
        nationality = Some("Emirati"),
        occupation = Some("Manager"),
        employer = Some("EMAAR"),
        createdAt = LocalDateTime.of(2018, 10, 1, 0, 0, 0, 0),
        createdBy = "SuperUser",
        updatedAt = None,
        updatedBy = None,
        activatedAt = Some(LocalDateTime.of(2018, 10, 1, 0, 0)))

      val bu1 = BusinessUser(
        id = 1,
        uuid = testUuidOne,
        userId = 3,
        userUUID = UUID.randomUUID().toString(),
        businessName = "PegB",
        brandName = "PegB",
        businessCategory = "business_category",
        businessType = "business_type",

        registrationNumber = "registration_number",
        taxNumber = "tax_number".some,
        registrationDate = LocalDate.of(1970, 1, 1).some,
        currencyId = 1,
        collectionAccountId = None,
        collectionAccountUUID = None,
        distributionAccountId = None,
        distributionAccountUUID = None,
        createdBy = "SuperUser",
        createdAt = LocalDateTime.of(2018, 10, 14, 0, 0),
        updatedAt = LocalDateTime.of(2018, 10, 14, 0, 0).some,
        updatedBy = "pegbuser".some)

      val genericUser1 = GenericUser(
        id = 1,
        uuid = testUuidOne,
        userName = "user01",
        password = Some("pword"),
        customerType = Some("type_one"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("alice@gmail.com"),
        status = Some("ACTIVE"),
        activatedAt = Option(LocalDateTime.of(2018, 10, 1, 0, 0, 0)),
        passwordUpdatedAt = None,
        createdAt = LocalDateTime.of(2018, 10, 1, 0, 0, 0),
        createdBy = "SuperUser",
        updatedAt = None,
        updatedBy = None,

        customerName = "Alice".some,
        businessUserFields = none,
        individualUserFields = iu1.some)

      val criteria = GenericUserCriteria(userId = UUIDLike(testUuidOne).some)

      (mockUserDao.getUserByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Right(Seq(genericUser1)))

      val result = customerReadService.getUser(UUID.fromString(testUuidOne))

      whenReady(result) { actual ⇒
        actual mustBe Right(genericUser1.asDomain.get)
      }
    }
  }

}
