package tech.pegb.backoffice.domain.auth

import java.time.{LocalDateTime}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.{Binding, bind}
import play.api.test.Injecting
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.auth.abstraction.{BackOfficeUserDao, BusinessUnitDao}
import tech.pegb.backoffice.dao.auth.dto.{BackOfficeUserCriteria, BusinessUnitCriteria ⇒ DaoBusinessUnitCriteria, BusinessUnitToUpdate ⇒ DaoBusinessUnitToUpdate}
import tech.pegb.backoffice.dao.auth.entity.{BusinessUnit ⇒ DaoBusinessUnit}
import tech.pegb.backoffice.dao.auth.sql.BackOfficeUserSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.BusinessUnitService
import tech.pegb.backoffice.domain.auth.dto.{BusinessUnitCriteria, BusinessUnitToCreate, BusinessUnitToRemove, BusinessUnitToUpdate}
import tech.pegb.backoffice.domain.auth.model.BusinessUnit
import tech.pegb.backoffice.mapping.dao.domain.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.Implicits._
import tech.pegb.backoffice.util.Constants._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.PegBNoDbTestApp

class BusinessUnitServiceSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val businessUnitDao: BusinessUnitDao = stub[BusinessUnitDao]
  val backOfficeUserDao: BackOfficeUserDao = stub[BackOfficeUserDao]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[BusinessUnitDao].to(businessUnitDao),
      bind[BackOfficeUserDao].to(backOfficeUserDao))

  val businessUnitService: BusinessUnitService = inject[BusinessUnitService]

  "BusinessUnitService" should {

    "create Business unit" in {
      val dto = BusinessUnitToCreate(
        name = "new_department",
        createdBy = "George",
        createdAt = LocalDateTime.now())

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = true), None, None, None).returns(Right(Seq.empty))

      val mockResult = DaoBusinessUnit(
        id = UUID.randomUUID().toString,
        name = dto.name,
        isActive = 1,
        createdBy = Some(dto.createdBy),
        createdAt = Some(dto.createdAt),
        updatedBy = None,
        updatedAt = None)

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = false), None, None, None).returns(Right(Seq.empty))

      (businessUnitDao.create _).when(dto.asDao).returns(Right(mockResult))

      val futureResult = businessUnitService.create(dto, reactivateIfExisting = false)
      val expectedResult = BusinessUnit(
        id = UUID.fromString(mockResult.id),
        name = dto.name, createdBy = dto.createdBy, createdAt = dto.createdAt,
        updatedAt = None, updatedBy = None)

      whenReady(futureResult)(result ⇒ {
        result.right.get mustBe expectedResult
      })
    }

    "create Business unit -reactivate inactive" in {
      val dto = BusinessUnitToCreate(
        name = "new_department",
        createdBy = "George",
        createdAt = LocalDateTime.now())

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = true), None, None, None)
        .returns(Right(Nil))

      val mockResult = DaoBusinessUnit(
        id = UUID.randomUUID().toString,
        name = dto.name,
        isActive = 0,
        createdBy = Some(dto.createdBy),
        createdAt = Some(dto.createdAt),
        updatedBy = None,
        updatedAt = None)

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = false), None, None, None)
        .returns(Right(Seq(mockResult)))

      val updatedMockResult = mockResult.copy(
        updatedAt = Some(dto.createdAt),
        updatedBy = Some(dto.createdBy))

      (businessUnitDao.update(_: String, _: DaoBusinessUnitToUpdate))
        .when(
          mockResult.id,
          BusinessUnitToUpdate(
            name = Some(dto.name),
            updatedBy = dto.createdBy,
            updatedAt = dto.createdAt,
            lastUpdatedAt = None).asDao(isActive = true))
        .returns(Right(updatedMockResult.toOption))

      val futureResult = businessUnitService.create(dto, reactivateIfExisting = true)
      val expectedResult = BusinessUnit(
        id = UUID.fromString(updatedMockResult.id),
        name = updatedMockResult.name,
        createdBy = updatedMockResult.createdBy.getOrElse("UNKNOWN"),
        createdAt = updatedMockResult.createdAt.getOrElse(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
        updatedAt = updatedMockResult.updatedAt,
        updatedBy = updatedMockResult.updatedBy)

      whenReady(futureResult)(result ⇒ {
        result.right.get mustBe expectedResult
      })
    }

    "fail to recreate inactive Business unit -reactivate false" in {
      val dto = BusinessUnitToCreate(
        name = "new_department",
        createdBy = "George",
        createdAt = LocalDateTime.now())

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = true), None, None, None)
        .returns(Right(Nil))

      val mockResult = DaoBusinessUnit(
        id = UUID.randomUUID().toString,
        name = dto.name,
        isActive = 0,
        createdBy = Some(dto.createdBy),
        createdAt = Some(dto.createdAt),
        updatedBy = None,
        updatedAt = None)

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = false), None, None, None)
        .returns(Right(Seq(mockResult)))

      val futureResult = businessUnitService.create(dto, reactivateIfExisting = false)

      whenReady(futureResult)(result ⇒ {
        result.left.get mustBe ServiceError.duplicateError("Create business_unit failed. Recreate flag must be set to true.")
      })
    }

    "fail to recreate Business unit -not found during update" in {
      val dto = BusinessUnitToCreate(
        name = "new_department",
        createdBy = "George",
        createdAt = LocalDateTime.now())

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = true), None, None, None)
        .returns(Right(Nil))

      val mockResult = DaoBusinessUnit(
        id = UUID.randomUUID().toString,
        name = dto.name,
        isActive = 0,
        createdBy = Some(dto.createdBy),
        createdAt = Some(dto.createdAt),
        updatedBy = None,
        updatedAt = None)

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = false), None, None, None)
        .returns(Right(Seq(mockResult)))

      (businessUnitDao.update(_: String, _: DaoBusinessUnitToUpdate))
        .when(
          mockResult.id,
          BusinessUnitToUpdate(
            name = Some(dto.name),
            updatedBy = dto.createdBy,
            updatedAt = dto.createdAt,
            lastUpdatedAt = None).asDao(isActive = true))
        .returns(Right(None))

      val futureResult = businessUnitService.create(dto, reactivateIfExisting = true)

      whenReady(futureResult)(result ⇒ {
        result.left.get mustBe ServiceError.notFoundError("Create business_unit failed. Inactive business_unit was not found.")
      })
    }

    "fail to create Business unit -from dao error" in {
      val dto = BusinessUnitToCreate(
        name = "new_department",
        createdBy = "George",
        createdAt = LocalDateTime.now())

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = true), None, None, None).returns(Right(Nil))

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = false), None, None, None).returns(Right(Nil))

      val mockResult = DaoError.EntityAlreadyExistsError("some dao layer message")
      (businessUnitDao.create _).when(dto.asDao).returns(Left(mockResult))

      val futureResult = businessUnitService.create(dto, reactivateIfExisting = false)

      whenReady(futureResult)(result ⇒ {
        result.left.get mustBe ServiceError.duplicateError(mockResult.message, Some(mockResult.id))
      })
    }

    "fail to create Business unit -if name is empty" in {
      val dto = BusinessUnitToCreate(
        name = "",
        createdBy = "George",
        createdAt = LocalDateTime.now())

      val futureResult = businessUnitService.create(dto, reactivateIfExisting = false)

      whenReady(futureResult)(result ⇒ {
        result.left.get mustBe ServiceError.validationError("Create business_unit failed. Name cannot be empty and cannot be longer than 32 characters.")
      })
    }

    "fail to create Business unit -if name is longer than 32 characters" in {
      val dto = BusinessUnitToCreate(
        name = "Department of Risk and Disaster Management and Mitigation",
        createdBy = "George",
        createdAt = LocalDateTime.now())

      val futureResult = businessUnitService.create(dto, reactivateIfExisting = false)

      whenReady(futureResult)(result ⇒ {
        result.left.get mustBe ServiceError.validationError("Create business_unit failed. Name cannot be empty and cannot be longer than 32 characters.")
      })
    }

    "fail to create Business unit -if created_by is empty" in {
      val dto = BusinessUnitToCreate(
        name = "new_department",
        createdBy = null,
        createdAt = LocalDateTime.now())

      val futureResult = businessUnitService.create(dto, reactivateIfExisting = false)

      whenReady(futureResult)(result ⇒ {
        result.left.get mustBe ServiceError.validationError("Create business_unit failed. Created_by cannot be empty.")
      })
    }

    "return all active business units" in {
      val criteria = BusinessUnitCriteria()

      val mockDaoResults = Seq(
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Accounting", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Finance", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Management", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Operations", createdBy = Some("pegbuser")))

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(criteria.asDao(isActive = true), None, None, None)
        .returns(Right(mockDaoResults))

      val futureResult = businessUnitService.getAllActiveBusinessUnits(criteria, Seq.empty, None, None)

      val expected = mockDaoResults.map(_.asDomain.get)

      whenReady(futureResult)(result ⇒ {
        result.right.get mustBe expected
      })
    }

    "fail to get business units if id from database is not UUID" in {
      val criteria = BusinessUnitCriteria()

      val mockDaoResults = Seq(
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Accounting", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Finance", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Management", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = "12345", name = "Operations", createdBy = Some("pegbuser")))

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(criteria.asDao(isActive = true), None, None, None)
        .returns(Right(mockDaoResults))

      val futureResult = businessUnitService.getAllActiveBusinessUnits(criteria, Seq.empty, None, None)

      whenReady(futureResult)(result ⇒ {
        result.isLeft mustBe true
        result.left.get mustBe ServiceError.dtoMappingError("Unable to read business unit resource correctly. Id was not UUID")
      })
    }

    "fail to get business units if name from database is empty" in {
      val criteria = BusinessUnitCriteria()

      val mockDaoResults = Seq(
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Accounting", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Finance", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Management", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = " ", createdBy = Some("pegbuser")))

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(criteria.asDao(isActive = true), None, None, None)
        .returns(Right(mockDaoResults))

      val futureResult = businessUnitService.getAllActiveBusinessUnits(criteria, Seq.empty, None, None)

      whenReady(futureResult)(result ⇒ {
        result.isLeft mustBe true
        result.left.get mustBe ServiceError.dtoMappingError("Unable to read business unit resource correctly. Name or created_by cannot be empty.")
      })
    }

    "fail to get business units if createdBy from database is empty" ignore {
      val criteria = BusinessUnitCriteria()

      val mockDaoResults = Seq(
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Accounting", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Finance", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Management", createdBy = Some("pegbuser")),
        DaoBusinessUnit.empty.copy(id = UUID.randomUUID().toString, name = "Operations", createdBy = Some("")))

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(criteria.asDao(isActive = true), None, None, None)
        .returns(Right(mockDaoResults))

      val futureResult = businessUnitService.getAllActiveBusinessUnits(criteria, Seq.empty, None, None)

      whenReady(futureResult)(result ⇒ {
        result.isLeft mustBe true
        result.left.get mustBe ServiceError.dtoMappingError("Unable to read business unit resource correctly. Name or created_by cannot be empty.")
      })
    }

    "count business units based on criteria" in {
      val randomUuid = UUID.randomUUID()

      val expectedResult = List.empty[BusinessUnit] //todo add business units
      val criteria = BusinessUnitCriteria()

      (businessUnitDao.countBusinessUnitsByCriteria _).when(criteria.asDao(isActive = true))
        .returns(Right(10))

      val futureResult = businessUnitService.countAllActiveBusinessUnits(criteria)

      whenReady(futureResult)(result ⇒ result.right.get mustBe 10)
    }

    "return updated business unit" in {
      val randomUuid = UUID.randomUUID()

      val dto = BusinessUnitToUpdate(
        name = Some("new_department"),
        updatedBy = "Lilet",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = None)
      val mockResult = DaoBusinessUnit(
        id = randomUuid.toString,
        name = "new_department",
        isActive = 1,
        createdBy = Some("pegbuser"),
        createdAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0)),
        updatedBy = Some("Lilet"),
        updatedAt = Some(dto.updatedAt))

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(id = randomUuid.toOption).asDao(isActive = true), None, None, None)
        .returns(Right(Seq(DaoBusinessUnit.empty.copy(updatedAt = None))))

      (businessUnitDao.update _).when(randomUuid.toString, dto.asDao(isActive = true))
        .returns(Right(Some(mockResult)))

      val futureResult = businessUnitService.update(randomUuid, dto)

      whenReady(futureResult)(result ⇒ {
        result.right.get.id mustBe randomUuid
        result.right.get.name mustBe dto.name.get
        result.right.get.updatedBy mustBe dto.updatedBy.toOption
        result.right.get.updatedAt mustBe dto.updatedAt.toOption
      })
    }

    "fail update business unit if dao returned entity which cannot be mapped in domain -id is not UUID" in {
      val randomUuid = UUID.randomUUID()

      val dto = BusinessUnitToUpdate(
        name = Some("new_department"),
        updatedBy = "Lilet",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = None)

      val mockResult = DaoBusinessUnit(
        id = "123456",
        name = "new_department",
        isActive = 1,
        createdBy = Some("pegbuser"),
        createdAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0)),
        updatedBy = Some("Lilet"),
        updatedAt = Some(dto.updatedAt))

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(id = randomUuid.toOption).asDao(isActive = true), None, None, None)
        .returns(Right(Seq(DaoBusinessUnit.empty.copy(updatedAt = None))))

      (businessUnitDao.update _).when(randomUuid.toString, dto.asDao(isActive = true))
        .returns(Right(Some(mockResult)))

      val futureResult = businessUnitService.update(randomUuid, dto)

      whenReady(futureResult)(result ⇒ {
        result.isLeft mustBe true
        result.left.get mustBe ServiceError.dtoMappingError("Update may have succeeded but unable to read business unit resource correctly. Id was not UUID")
      })
    }

    "fail update business unit if dao returned entity which cannot be mapped in domain -name is empty" in {
      val randomUuid = UUID.randomUUID()

      val dto = BusinessUnitToUpdate(
        name = Some("new_department"),
        updatedBy = "Lilet",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = None)

      val mockResult = DaoBusinessUnit(
        id = randomUuid.toString,
        name = "",
        isActive = 1,
        createdBy = Some("pegbuser"),
        createdAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0)),
        updatedBy = Some("Lilet"),
        updatedAt = Some(dto.updatedAt))

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(id = randomUuid.toOption).asDao(isActive = true), None, None, None)
        .returns(Right(Seq(DaoBusinessUnit.empty.copy(updatedAt = None))))

      (businessUnitDao.update _).when(randomUuid.toString, dto.asDao(isActive = true))
        .returns(Right(Some(mockResult)))

      val futureResult = businessUnitService.update(randomUuid, dto)

      whenReady(futureResult)(result ⇒ {
        result.isLeft mustBe true
        result.left.get mustBe ServiceError.dtoMappingError("Update may have succeeded but unable to read business unit resource correctly. Name or created_by cannot be empty.")
      })
    }

    "fail to update business unit if id was not found" in {
      val randomUuid = UUID.randomUUID()

      val dto = BusinessUnitToUpdate(
        name = Some("new_department"),
        updatedBy = "Lilet",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = None)

      val mockResult = None

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(id = randomUuid.toOption).asDao(isActive = true), None, None, None)
        .returns(Right(Seq(DaoBusinessUnit.empty.copy(updatedAt = None))))

      (businessUnitDao.update _).when(randomUuid.toString, dto.asDao(isActive = true))
        .returns(Right(None))

      val futureResult = businessUnitService.update(randomUuid, dto)

      whenReady(futureResult)(result ⇒ {
        result.isLeft mustBe true
        result.left.get mustBe ServiceError.notFoundError(s"Update business_unit failed. Id [$randomUuid] not found.")
      })
    }

    "fail to update business unit if name is empty" in {
      val randomUuid = UUID.randomUUID()

      val dto = BusinessUnitToUpdate(
        name = Some(" "),
        updatedBy = "Lilet",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = None)

      val futureResult = businessUnitService.update(randomUuid, dto)

      whenReady(futureResult)(result ⇒ {
        result.isLeft mustBe true
        result.left.get mustBe ServiceError.validationError(s"Update business_unit failed. Name cannot be empty and cannot be longer than 32 characters.")
      })
    }

    "fail to update business unit if name is longer than 32 characters" in {
      val randomUuid = UUID.randomUUID()

      val dto = BusinessUnitToUpdate(
        name = Some("Department of Risk and Disaster Management and Mitigation"),
        updatedBy = "Lilet",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = None)

      val futureResult = businessUnitService.update(randomUuid, dto)

      whenReady(futureResult)(result ⇒ {
        result.isLeft mustBe true
        result.left.get mustBe ServiceError.validationError(s"Update business_unit failed. Name cannot be empty and cannot be longer than 32 characters.")
      })
    }

    "fail to update business unit if updated_by is empty" in {
      val randomUuid = UUID.randomUUID()

      val dto = BusinessUnitToUpdate(
        name = Some("new_department"),
        updatedBy = "",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = None)

      val futureResult = businessUnitService.update(randomUuid, dto)

      whenReady(futureResult)(result ⇒ {
        result.isLeft mustBe true
        result.left.get mustBe ServiceError.validationError(s"Update business_unit failed. Updated_by cannot be empty.")
      })
    }

    "fail to update business unit if updated_at is before last_updated_at" in {
      val randomUuid = UUID.randomUUID()

      val dto = BusinessUnitToUpdate(
        name = Some("new_department"),
        updatedBy = "Lilet",
        updatedAt = LocalDateTime.now.minusDays(1),
        lastUpdatedAt = Some(LocalDateTime.now))

      val futureResult = businessUnitService.update(randomUuid, dto)

      whenReady(futureResult)(result ⇒ {
        result.isLeft mustBe true
        result.left.get mustBe ServiceError.validationError(s"Update business_unit failed. Updated_at cannot be before last_updated_at.")
      })
    }

    "delete business unit" in {
      val randomUuid = UUID.randomUUID()
      val removedBy = "Analyn"
      val removedAt = LocalDateTime.now()
      val mockInput = BackOfficeUserCriteria(
        businessUnitId = Some(CriteriaField(BackOfficeUserSqlDao.cBuId, randomUuid.toString)),
        isActive = Some(CriteriaField(BackOfficeUserSqlDao.cIsActive, 1)))

      (backOfficeUserDao.countBackOfficeUsersByCriteria _)
        .when(mockInput.toOption)
        .returns(Right(0))

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(id = randomUuid.toOption).asDao(isActive = true), None, None, None)
        .returns(Right(Seq(DaoBusinessUnit.empty.copy(updatedAt = None))))

      val dto = BusinessUnitToUpdate.empty.copy(updatedAt = removedAt, updatedBy = removedBy, lastUpdatedAt = None).asDao(isActive = false)
      (businessUnitDao.update _).when(randomUuid.toString, dto)
        .returns(Right(DaoBusinessUnit.empty.copy(id = randomUuid.toString).toOption))

      val futureResult = businessUnitService.remove(randomUuid, BusinessUnitToRemove(removedBy, removedAt, None))
      val expectedResult = UnitInstance

      whenReady(futureResult)(result ⇒ {
        println(result)
        result.right.get mustBe expectedResult
      })
    }

    "delete business unit will succeed even if business unit was not found" in {
      val randomUuid = UUID.randomUUID()
      val removedBy = "Analyn"
      val removedAt = LocalDateTime.now()
      val mockInput = BackOfficeUserCriteria(
        businessUnitId = Some(CriteriaField(BackOfficeUserSqlDao.cBuId, randomUuid.toString)),
        isActive = Some(CriteriaField(BackOfficeUserSqlDao.cIsActive, 1)))

      (backOfficeUserDao.countBackOfficeUsersByCriteria _)
        .when(mockInput.toOption)
        .returns(Right(0))

      (businessUnitDao.getBusinessUnitsByCriteria(_: DaoBusinessUnitCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BusinessUnitCriteria(id = randomUuid.toOption).asDao(isActive = true), None, None, None)
        .returns(Right(Seq(DaoBusinessUnit.empty.copy(updatedAt = None))))

      val dto = BusinessUnitToUpdate.empty.copy(updatedAt = removedAt, updatedBy = removedBy).asDao(isActive = false)

      (businessUnitDao.update _).when(randomUuid.toString, dto)
        .returns(Right(None))

      val futureResult = businessUnitService.remove(randomUuid, BusinessUnitToRemove(removedBy, removedAt, None))
      val expectedResult = UnitInstance

      whenReady(futureResult)(result ⇒ result.right.get mustBe expectedResult)
    }

    "fail to delete business unit if 1 or more active backoffice user belongs to it" in {
      val randomUuid = UUID.randomUUID()
      val removedBy = "Analyn"
      val removedAt = LocalDateTime.now()

      (backOfficeUserDao.countBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          businessUnitId = Some(CriteriaField(BackOfficeUserSqlDao.cBuId, randomUuid.toString)),
          isActive = Some(CriteriaField(BackOfficeUserSqlDao.cIsActive, 1))).toOption)
        .returns(Right(3))

      val futureResult = businessUnitService.remove(randomUuid, BusinessUnitToRemove(removedBy, removedAt, None))
      val expectedResult = UnitInstance

      whenReady(futureResult)(result ⇒
        result.left.get mustBe ServiceError.validationError(s"Remove business_unit failed. One or more active back_office_users still belong to this business_unit."))
    }

    "fail to delete business unit if user (backoffice user from the request header) is empty" in {
      val randomUuid = UUID.randomUUID()
      val removedBy = " "
      val removedAt = LocalDateTime.now()

      val futureResult = businessUnitService.remove(randomUuid, BusinessUnitToRemove(removedBy, removedAt, None))
      val expectedResult = UnitInstance

      whenReady(futureResult)(result ⇒
        result.left.get mustBe ServiceError.validationError(s"Remove business_unit failed. User cannot be empty."))
    }
  }
}
