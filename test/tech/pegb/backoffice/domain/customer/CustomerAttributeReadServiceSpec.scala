package tech.pegb.backoffice.domain.customer

import java.time.LocalDateTime

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import tech.pegb.backoffice.dao.customer.abstraction._
import tech.pegb.backoffice.dao.customer.entity
import tech.pegb.backoffice.domain.customer.implementation.CustomerAttributeReadService
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.core.{TestExecutionContext}
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._

class CustomerAttributeReadServiceSpec extends PlaySpec with MockFactory with ScalaFutures {

  val config = AppConfig("application.test.conf")
  val executionContexts = TestExecutionContext
  val mockEmployerDao = stub[EmployerDao]
  val mockNationalityDao = stub[NationalityDao]
  val mockOccupationDao = stub[OccupationDao]
  val mockCompanyDao = stub[CompanyDao]

  "CustomerAttributeReadService" should {
    "return set of employers in getEmployers" in {
      val customerAttributeReadService = new CustomerAttributeReadService(config, executionContexts, mockEmployerDao, mockNationalityDao, mockOccupationDao, mockCompanyDao)

      val expectedSet = Set(
        entity.Employer(
          id = 1,
          employerName = "PegB",
          description = None,
          createdAt = LocalDateTime.now(),
          createdBy = "Bob",
          updatedAt = None,
          updatedBy = None,
          isActive = true),
        entity.Employer(
          id = 2,
          employerName = "Kentech",
          description = None,
          createdAt = LocalDateTime.now(),
          createdBy = "Bob",
          updatedAt = None,
          updatedBy = None,
          isActive = true))
      (mockEmployerDao.getAll _).when()
        .returns(Right(expectedSet))

      val actual = customerAttributeReadService.getEmployers

      actual.isRight mustBe true
      actual.right.get mustEqual expectedSet.map(_.asDomain)
    }
    "return set of nationalities in getNationalities" in {
      val customerAttributeReadService = new CustomerAttributeReadService(config, executionContexts, mockEmployerDao, mockNationalityDao, mockOccupationDao, mockCompanyDao)

      val expectedSet = Set(
        entity.Nationality(
          id = 1,
          name = "Spanish",
          description = None,
          createdAt = LocalDateTime.now(),
          createdBy = "Bob",
          updatedAt = None,
          updatedBy = None,
          isActive = true),
        entity.Nationality(
          id = 2,
          name = "French",
          description = None,
          createdAt = LocalDateTime.now(),
          createdBy = "Bob",
          updatedAt = None,
          updatedBy = None,
          isActive = true))
      (mockNationalityDao.getAll _).when()
        .returns(Right(expectedSet))

      val actual = customerAttributeReadService.getNationalities

      actual.isRight mustBe true
      actual.right.get mustEqual expectedSet.map(_.asDomain)
    }
    "return set of occupations in getOccupations" in {
      val customerAttributeReadService = new CustomerAttributeReadService(config, executionContexts, mockEmployerDao, mockNationalityDao, mockOccupationDao, mockCompanyDao)

      val expectedSet = Set(
        entity.Occupation(
          id = 1,
          name = "SW Developer",
          description = None,
          createdAt = LocalDateTime.now(),
          createdBy = "Bob",
          updatedAt = None,
          updatedBy = None,
          isActive = true),
        entity.Occupation(
          id = 2,
          name = "Gardener",
          description = None,
          createdAt = LocalDateTime.now(),
          createdBy = "Bob",
          updatedAt = None,
          updatedBy = None,
          isActive = true))
      (mockOccupationDao.getAll _).when()
        .returns(Right(expectedSet))

      val actual = customerAttributeReadService.getOccupations

      actual.isRight mustBe true
      actual.right.get mustEqual expectedSet.map(_.asDomain)
    }
    "return set of companies in getCompanies" in {
      val customerAttributeReadService = new CustomerAttributeReadService(config, executionContexts, mockEmployerDao, mockNationalityDao, mockOccupationDao, mockCompanyDao)

      val expectedSet = Set(
        entity.Company(
          id = 1,
          companyName = "PESALINK",
          companyFullName = None,
          createdAt = LocalDateTime.now(),
          createdBy = "Bob",
          updatedAt = None,
          updatedBy = None,
          isActive = true),
        entity.Company(
          id = 2,
          companyName = "MPESA",
          companyFullName = None,
          createdAt = LocalDateTime.now(),
          createdBy = "Bob",
          updatedAt = None,
          updatedBy = None,
          isActive = true))
      (mockCompanyDao.getAll _).when()
        .returns(Right(expectedSet))

      val actual = customerAttributeReadService.getCompanies

      actual.isRight mustBe true
      actual.right.get mustEqual expectedSet.map(_.asDomain)
    }
  }
}
