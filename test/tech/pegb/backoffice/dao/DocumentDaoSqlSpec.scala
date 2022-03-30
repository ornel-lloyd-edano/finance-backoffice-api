package tech.pegb.backoffice.dao

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.dao.DaoError.PreconditionFailed
import tech.pegb.backoffice.dao.document.abstraction.DocumentDao
import tech.pegb.backoffice.dao.document.dto.{DocumentCriteria, DocumentToCreate, DocumentToUpdate}
import tech.pegb.backoffice.dao.document.sql.DocumentSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet, Ordering ⇒ DaoSorter}
import tech.pegb.backoffice.util.{Utils}
import tech.pegb.core.PegBTestApp

import scala.util.Random

class DocumentDaoSqlSpec extends PegBTestApp {

  private val existingApplicationUUID: UUID = UUID.randomUUID()

  private val anotherExistingApplicationUUID: UUID = UUID.randomUUID()

  private val existingUserUUID: UUID = UUID.randomUUID()

  private val existingDocumentUUID = UUID.randomUUID()

  val mockBusinessApplicationIdForDeleted = UUID.randomUUID()
  val mockBusinessApplicationId = UUID.randomUUID()

  override val initSql: String =
    s"""
       |CREATE TABLE IF NOT EXISTS `${DocumentSqlDao.TableName}` (
       | `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
       | `uuid` VARCHAR(36),
       | `user_id` INT,
       | `application_id` INT DEFAULT NULL,
       | `bu_application_id` INT DEFAULT NULL,
       | `document_name` VARCHAR(50) DEFAULT NULL,
       | `status` VARCHAR(8),
       | `document_number` VARCHAR(36),
       | `document_type` VARCHAR(20),
       | `purpose` VARCHAR(50),
       | `rejection_reason` VARCHAR(50) DEFAULT NULL,
       | `image_type` ENUM('document_front', 'document_back'),
       | `properties` VARCHAR(200),
       | `created_by` VARCHAR(50),
       | `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
       | `file_name` VARCHAR(100) DEFAULT NULL,
       | `file_uploaded_by` VARCHAR(50) DEFAULT NULL,
       | `file_uploaded_at` DATETIME DEFAULT NULL,
       | `checked_by` VARCHAR(50) DEFAULT NULL,
       | `checked_at` DATETIME DEFAULT NULL,
       | `updated_by` VARCHAR(50),
       | `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       | PRIMARY KEY(`id`)
       |);
       |
      |CREATE TABLE IF NOT EXISTS `user_applications` (
       |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
       |  `uuid` varchar(36) NOT NULL,
       |  `user_id` int(10) unsigned NOT NULL,
       |  `status` varchar(30) NOT NULL,
       |  `stage` varchar(30) NOT NULL,
       |  `rejection_reason` varchar(300) DEFAULT NULL,
       |  `checked_by` varchar(50) DEFAULT NULL,
       |  `checked_at` datetime DEFAULT NULL,
       |  `total_score` float DEFAULT NULL,
       |  `fullname_score` float DEFAULT NULL,
       |  `fullname_original` varchar(50) DEFAULT NULL,
       |  `fullname_updated` varchar(50) DEFAULT NULL,
       |  `birthdate_score` float DEFAULT NULL,
       |  `birthdate_original` datetime DEFAULT NULL,
       |  `birthdate_updated` datetime DEFAULT NULL,
       |  `birthplace_score` float DEFAULT NULL,
       |  `birthplace_original` varchar(50) DEFAULT NULL,
       |  `birthplace_updated` varchar(50) DEFAULT NULL,
       |  `gender_score` float DEFAULT NULL,
       |  `gender_original` varchar(50) DEFAULT NULL,
       |  `gender_updated` varchar(50) DEFAULT NULL,
       |  `nationality_score` float DEFAULT NULL,
       |  `nationality_original` varchar(50) DEFAULT NULL,
       |  `nationality_updated` varchar(50) DEFAULT NULL,
       |  `person_id_score` float DEFAULT NULL,
       |  `person_id_original` varchar(50) DEFAULT NULL,
       |  `person_id_updated` varchar(50) DEFAULT NULL,
       |  `document_id_score` float DEFAULT NULL,
       |  `document_id_original` varchar(50) DEFAULT NULL,
       |  `document_id_updated` varchar(50) DEFAULT NULL,
       |  `document_type` varchar(50) DEFAULT NULL,
       |  `created_by` varchar(50) NOT NULL,
       |  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
       |  `updated_by` varchar(50) DEFAULT NULL,
       |  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       |  PRIMARY KEY (`id`)
       |);
       |INSERT INTO users(id, uuid, username, status, created_by, created_at)
       |VALUES(1, '${existingUserUUID.toString}', 'Alice Smith', 'waiting_for_activation', 'Admin', '2019-01-01 10:30:00');
       |
       |INSERT INTO individual_users(id, msisdn, user_id, type, name, fullname, gender, company, birthdate, birth_place, nationality, occupation, employer, created_at, created_by, updated_at, updated_by) VALUES
       |(1, '971537465009', 1, 'type_one', 'Alice', 'Alice Smith', 'F', 'PegB', '2019-01-01 10:30:00', 'Dubai', 'Emirati', 'Manager', 'EMAAR', '2018-10-01 00:00:00', 'SuperUser', null, null);
       |
       |INSERT INTO user_applications(id, uuid, user_id, status, stage, created_by, created_at)
       |VALUES(1, '${existingApplicationUUID.toString}', 1, 'pending', 'new', 'Admin', '2019-01-01 10:30:00');
       |
       |INSERT INTO user_applications(id, uuid, user_id, status, stage, created_by, created_at)
       |VALUES(2, '${anotherExistingApplicationUUID.toString}', 1, 'pending', 'new', 'Admin', '2019-01-01 10:30:00');
       |
       |CREATE TABLE IF NOT EXISTS `business_user_applications` (
       |  id int(10) unsigned NOT NULL AUTO_INCREMENT,
       |  uuid varchar(36) NOT NULL,
       |  business_name varchar(36) NOT NULL,
       |  brand_name varchar(36) NOT NULL,
       |  business_category varchar(36) NOT NULL,
       |  stage varchar(30) NOT NULL,
       |  status varchar(30) NOT NULL,
       |  user_tier varchar(30) NOT NULL,
       |  business_type varchar(50) NOT NULL,
       |  registration_number varchar(50) NOT NULL,
       |  tax_number varchar(50) DEFAULT NULL,
       |  registration_date date DEFAULT NULL,
       |  explanation varchar(256) NULL,
       |  submitted_by varchar(50) NULL,
       |  submitted_at datetime NULL,
       |  checked_by varchar(50) NULL,
       |  checked_at datetime NULL,
       |  created_by varchar(50) NOT NULL,
       |  created_at datetime DEFAULT CURRENT_TIMESTAMP,
       |  updated_by varchar(50) DEFAULT NULL,
       |  updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       |PRIMARY KEY (id));
       |
       |INSERT INTO business_user_applications (id,uuid,business_name,brand_name,business_category,stage,status,user_tier, business_type, registration_number, tax_number, registration_date, created_by,updated_by,created_at,updated_at) VALUES
       |('1','fcad736b-a6d8-4b8e-845d-edb83489ac50','Universal Catering Co','Costa Coffee DSO','Restaurants - 5182','identity_info','ongoing', 'basic', 'merchant', '212/212EE', 'B12342M', '2019-01-01','system','system', now(), now()),
       |('2','${mockBusinessApplicationIdForDeleted.toString}','Tindahan ni Aling Nena','Nenas Store','Store - 1111','application_documents','approved', 'basic', 'merchant', '111/212EE', 'A12342M', '1990-01-01','system','system', now(), now()),
       |('3','${mockBusinessApplicationId.toString}','Henry Sy and kids','SM Megamall','Department Store - 9999','application_documents','pending', 'basic', 'merchant', '999/212EE', 'C12342M', '1999-01-01','system','system', now(), now());
       |
       |INSERT INTO application_documents(id, uuid, user_id, application_id, status, document_number, document_type, purpose, created_by, created_at)
       |VALUES(1, '${existingDocumentUUID.toString}', 1, 1, 'pending', 'selfie_uid', 'selfie', 'wallet application requirement', 'Admin', '2019-01-30 10:30:00');
       |
       |CREATE TABLE `business_user_application_primary_contacts` (
       |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
       |  `uuid` varchar(36) NOT NULL,
       |  `application_id` int(10) unsigned NOT NULL,
       |  `contact_type` varchar(30)  NOT NULL,
       |  `name` varchar(50)  NOT NULL,
       |  `middle_name` varchar(50)  DEFAULT NULL,
       |  `surname` varchar(50)  NOT NULL,
       |  `phone_number` varchar(50)  NOT NULL,
       |  `email` varchar(50)  NOT NULL,
       |  `id_type` varchar(50)  NOT NULL,
       |  `is_velocity_user` tinyint(1) NOT NULL DEFAULT '0',
       |  `velocity_level` varchar(50)  DEFAULT NULL,
       |  `is_default_contact` tinyint DEFAULT 0,
       |  `created_by` varchar(50)  NOT NULL,
       |  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
       |  `updated_by` varchar(50)  DEFAULT NULL,
       |  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       |  PRIMARY KEY (`id`),
       |  UNIQUE KEY (`uuid`),
       |  KEY `FK_bu_app_primary_contacts_application_id_bu_applications_id` (`application_id`),
       |  CONSTRAINT `FK_bu_app_primary_contacts_application_id_bu_applications_id` FOREIGN KEY (`application_id`) REFERENCES `business_user_applications` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
       |);
       |""".stripMargin

  override def cleanupSql: String = {
    s"""
       |DELETE FROM ${DocumentSqlDao.TableName};
       |DELETE FROM accounts;
       |DELETE FROM user_applications;
       |DELETE FROM individual_users;
       |DELETE FROM users;
     """.stripMargin
  }

  private val docToCreate = DocumentToCreate(
    customerId = Some(existingUserUUID),
    walletApplicationId = Some(existingApplicationUUID),
    businessApplicationId = None,
    documentType = "identity-card",
    documentIdentifier = None,
    fileName = Some("identity-card.jpg"),
    purpose = "some-purpose",
    createdBy = "unit-test",
    createdAt = Utils.nowAsLocal())
  private var docUuid: UUID = _

  "DocumentDaoSql" should {

    "createDocument return Right[Document] if document metadata was created on sql db" in {
      val dao = inject[DocumentDao]
      val resp = dao.createDocument(docToCreate)
      resp.isRight mustBe true
      resp.foreach(doc ⇒ docUuid = doc.uuid)
      Option(docUuid).isDefined mustBe true
    }

    "createDocument create some more docs" in {
      def randomDoc: DocumentToCreate = docToCreate.copy(
        customerId = Some(existingUserUUID),
        walletApplicationId = Some(anotherExistingApplicationUUID),
        purpose = "some-purpose" + Random.nextString(3),
        documentType = "some doc type",
        createdAt = Utils.nowAsLocal())

      val dao = inject[DocumentDao]
      dao.createDocument(randomDoc).isRight mustBe true
      dao.createDocument(randomDoc).isRight mustBe true
    }

    "getDocument return Right[Option[Document]] if document metadata was found on sql db" in {
      val dao = inject[DocumentDao]
      val resp = dao.getDocument(docUuid)
      resp.isRight mustBe true
      resp.flatMap(_.map(_.uuid).toRight(s"Document $docUuid was not found")) mustBe Right(docUuid)
    }

    "getDocument return Right[None] if document metadata was not found on sql db" in {
      val dao = inject[DocumentDao]
      val resp = dao.getDocument(UUID.randomUUID())
      resp mustBe Right(None)
    }

    "getDocumentsByCriteria return Right[Seq[Document]] for all documents metadata found on sql db" in {
      val dao = inject[DocumentDao]
      val resp = dao.getDocumentsByCriteria(DocumentCriteria.Empty, None, None, None)
      resp.map(_.size) mustBe Right(4)
      resp.map(_.exists(_.uuid == docUuid)) mustBe Right(true)
    }

    "getDocumentsByCriteria return Right[Seq[Document]] for all documents metadata found on sql db in specific ordering" in {
      val dao = inject[DocumentDao]
      val resp = dao.getDocumentsByCriteria(
        criteria = DocumentCriteria.Empty,
        ordering = Some(OrderingSet(DaoSorter("purpose", DaoSorter.DESC))),
        limit = None,
        offset = None)
      resp.map(_.size) mustBe Right(4)
      resp.map(_.last.uuid == docUuid) mustBe Right(true)
    }

    "getDocumentsByCriteria return Right[Seq[Document]] for subset of documents metadata found on sql db matching the given criteria" in {
      val dao = inject[DocumentDao]
      val resp = dao.getDocumentsByCriteria(
        criteria = DocumentCriteria.Empty.copy(
          applicationId = Some(existingApplicationUUID),
          customerId = Some(CriteriaField("user_id", existingUserUUID.toString))),
        ordering = None,
        limit = None,
        offset = None)
      resp.map(_.size) mustBe Right(2)
      resp.map(_.last.uuid == docUuid) mustBe Right(true)
      resp.map(_.last.walletApplicationId == Some(existingApplicationUUID)) mustBe Right(true)

      resp.map(_.last.customerId == Some(existingUserUUID)) mustBe Right(true)
    }

    "getDocumentsByCriteria return Right[Seq[Document]] for the next subset of documents metadata found on sql db matching the given criteria with limit and offset" in {
      val dao = inject[DocumentDao]
      val resp = dao.getDocumentsByCriteria(
        criteria = DocumentCriteria.Empty,
        ordering = Some(OrderingSet(DaoSorter("purpose", DaoSorter.DESC))),
        limit = Some(1),
        offset = Some(1))
      resp.map(_.size) mustBe Right(1)
      // after sorting our doc is head, so with limit 1 and offset 1 it must not be in response
      resp.map(_.head.uuid != docUuid) mustBe Right(true)
    }

    "countDocumentsByCriteria return Right[Int] which is total count of all documents metadata in sql db" in {
      val dao = inject[DocumentDao]
      val resp = dao.countDocumentsByCriteria(DocumentCriteria.Empty)
      resp mustBe Right(4)
    }

    "countDocumentsByCriteria return Right[Int] which is total count of subset of documents metadata matching given criteria in sql db" in {
      val dao = inject[DocumentDao]
      val resp = dao.countDocumentsByCriteria(DocumentCriteria.Empty.copy(applicationId = Some(existingApplicationUUID)))
      resp mustBe Right(2)
    }

    "updateDocument return Right[Option[Document]] if document metadata was updated on sql db" in {
      val local = LocalDateTime.now
      val dao = inject[DocumentDao]
      val newPurpose = "other-purpose"
      val resp = dao.updateDocument(docUuid, DocumentToUpdate(purpose = Some(newPurpose), updatedAt = Some(local), lastUpdatedAt = Some(docToCreate.createdAt)))

      resp.map(_.map(_.purpose)) mustBe Right(Some(newPurpose))
    }

    "updateDocument return Left[Precondition failed] if updated_at doesnt match" in {
      val dao = inject[DocumentDao]
      val newPurpose = "other-purpose"
      val resp = dao.updateDocument(docUuid, DocumentToUpdate(purpose = Some(newPurpose), lastUpdatedAt = None))

      resp mustBe Left(PreconditionFailed(s"Update failed. Document $docUuid has been modified by another process."))
    }

    "updateDocument return Right[None] if document metadata was not found on sql db" in {
      val dao = inject[DocumentDao]
      val newPurpose = "other-purpose"
      val resp = dao.updateDocument(UUID.randomUUID(), DocumentToUpdate(purpose = Some(newPurpose), lastUpdatedAt = None))

      resp.map(_.map(_.purpose)) mustBe Right(None)
    }

    "delsert should create documents based on dto and delete documents based on criteria" in {
      val dao = inject[DocumentDao]

      val supposedlyExistingDoc1 = DocumentToCreate(
        customerId = None,
        walletApplicationId = None,
        businessApplicationId = Some(mockBusinessApplicationIdForDeleted),
        documentType = "type1 Diabetes",
        documentIdentifier = None,
        fileName = Some("merchant-agreement01.pdf"),
        purpose = "application",
        createdBy = "test user",
        createdAt = LocalDateTime.now)

      val supposedlyExistingDoc2 = DocumentToCreate(
        customerId = None,
        walletApplicationId = None,
        businessApplicationId = Some(mockBusinessApplicationIdForDeleted),
        documentType = "type moco",
        documentIdentifier = None,
        fileName = Some("my certificate01.pdf"),
        purpose = "application",
        createdBy = "test user",
        createdAt = LocalDateTime.now)

      val toBeDeletedDoc1 = dao.createDocument(supposedlyExistingDoc1).right.get
      val toBeDeletedDoc2 = dao.createDocument(supposedlyExistingDoc2).right.get

      val dtoToCreate = Seq(
        DocumentToCreate(
          customerId = None,
          walletApplicationId = None,
          businessApplicationId = Some(mockBusinessApplicationId),
          documentType = "merchant-agreement",
          documentIdentifier = Some("11111111"),
          fileName = Some("merchant-agreement01.pdf"),
          purpose = "new application",
          createdBy = "test user",
          createdAt = LocalDateTime.now),
        DocumentToCreate(
          customerId = None,
          walletApplicationId = None,
          businessApplicationId = Some(mockBusinessApplicationId),
          documentType = "registration-certificate",
          documentIdentifier = Some("22222222222"),
          fileName = Some("my registration.pdf"),
          purpose = "new application",
          createdBy = "test user",
          createdAt = LocalDateTime.now))

      val criteria = DocumentCriteria(businessUserApplicationId = Some(mockBusinessApplicationIdForDeleted))
      val result = dao.delsert(dtoToCreate, criteriaToDelete = criteria)
      val expected = Set(
        (Some(mockBusinessApplicationId), Some("merchant-agreement01.pdf"), Some("11111111")),
        (Some(mockBusinessApplicationId), Some("my registration.pdf"), Some("22222222222")))

      result.right.get.map(d ⇒ (d.businessUserApplicationId, d.fileName, d.documentIdentifier)).toSet mustBe expected

      val confirmationOfDelsert = dao.getDocumentsByCriteria(DocumentCriteria(), None, None, None)
      import tech.pegb.backoffice.util.Implicits._

      confirmationOfDelsert.right.get.map(_.businessUserApplicationId).filter(_ == Some(mockBusinessApplicationIdForDeleted)).size mustBe 0
      confirmationOfDelsert.right.get.map(_.businessUserApplicationId).filter(_ == Some(mockBusinessApplicationId)).size mustBe 2
    }
  }

}
