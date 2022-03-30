package tech.pegb.backoffice.dao

import java.time.LocalDateTime

import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.contacts.dto.{ContactToInsert, ContactToUpdate, ContactsCriteria}
import tech.pegb.backoffice.dao.contacts.sql.ContactsSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, OrderingSet}
import tech.pegb.core.PegBTestApp

class ContactsSqlDaoSpec extends PegBTestApp with MockFactory {

  override def initSql =
    s"""
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES
       |('1', 'bcc32571-cf16-4abc-ac38-38d58f9cbab5', null, null, null, null, 'individual', null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |INSERT INTO business_user_applications (id,uuid,business_name,brand_name,business_category,stage,status,user_tier, business_type, registration_number, tax_number, registration_date, created_by,updated_by,created_at,updated_at) VALUES
       |('1','fcad736b-a6d8-4b8e-845d-edb83489ac50','Universal Catering Co','Costa Coffee DSO','Restaurants - 5182','identity_info','ongoing', 'basic', 'merchant', '212/212EE', 'B12342M', '2019-01-01','system','system','2019-01-01 00:00:00','2019-01-01 00:00:00'),
       |('2','926551f7-84f6-459e-bf15-a76d07d1b712','Tindahan ni Aling Nena','Nenas Store','Store - 1111','application_documents','approved', 'basic', 'merchant', '111/212EE', 'A12342M', '1990-01-01','system','system','2019-01-01 00:00:00','2019-01-01 00:00:00');
       |
       |CREATE TABLE contact_persons(
       |  id int unsigned NOT NULL AUTO_INCREMENT,
       |  uuid varchar(36) NOT NULL UNIQUE,
       |  bu_application_id int unsigned DEFAULT NULL,
       |  user_id int unsigned DEFAULT NULL,
       |  contact_type varchar(30) NOT NULL,
       |  name varchar(256) NOT NULL,
       |  middle_name varchar(256)  NULL,
       |  surname varchar(256) NOT NULL,
       |  phone_number varchar(256) NOT NULL,
       |  email varchar(256) NOT NULL,
       |  id_type varchar(50) NOT NULL,
       |  created_by varchar(50) NOT NULL,
       |  created_at datetime DEFAULT CURRENT_TIMESTAMP,
       |  updated_by varchar(50) DEFAULT NULL,
       |  updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       |  is_active tinyint(1) DEFAULT 1,
       |  vp_user_id int unsigned DEFAULT NULL,
       |  PRIMARY KEY (id),
       |  FOREIGN KEY(bu_application_id) REFERENCES business_user_applications(id),
       |  FOREIGN KEY(user_id) REFERENCES users(id)
       |);
       |
       |INSERT INTO vp_users
       |(id, uuid, user_id, name, middle_name, surname, msisdn, email, password, created_by, created_at, updated_by, updated_at, username, role, last_login_at, status)
       |VALUES
       |(1, 'fa7fc119-b465-46cb-b271-260725f7c782', 1, 'Tanmoy', null, 'Thakur', '97111110444', 't.thakur@pegb.tech', 'tanmoy', 'pegbuser', '2020-03-03 00:00:00', 'pegbuser', '2020-03-03 00:00:00', 'pacman', 'admin', '2020-03-03 00:00:00', 'active');
       |
       |INSERT INTO contact_persons
       |(id, uuid, bu_application_id, user_id, contact_type, name, middle_name, surname, phone_number, email, id_type, created_by, created_at, updated_by, updated_at, vp_user_id, is_active)
       |VALUES
       |('1', 'e966e237-b453-4fde-a141-f529de45b527', '1', null, 'owner', 'Ornel Lloyd', 'Pepito', 'Edano', '0544451679', 'o.lloyd@pegb.tech', 'type1', 'pegbuser', '2018-01-02 00:00:00', null, null, null, 1),
       |
       |('2', '8ff72391-7f49-4e37-a688-7f37a7864532', '2', null, 'admin', 'David', null, 'Salgado', '0544410338', 'd.salgado@pegb.tech', 'type2', 'pegbuser', '2019-01-03 00:00:00', null, null, null, 1),
       |
       |('3', '94ede5d6-3a6c-41f8-bb0f-63bc99c56436', null, '1', 'operator', 'Ujali', null, 'Tyagi', '0544130989', 'u.tyagi@pegb.tech', 'type2', 'pegbuser', '2020-01-01 00:00:00', null, null, null, 1),
       |
       |('4', 'e24c8971-c8e1-49da-94d5-73062957846c', '1', null, 'admin', 'Tanmoy', null, 'Thakur', '97111110444', 't.thakur@pegb.tech', 'type2', 'pegbuser', '2020-03-03 00:00:00', null, null, 1, 1),
       |
     """.stripMargin

  "ContactsSqlDao" should {
    val dao = inject[ContactsSqlDao]

    "get contact by uuid" in {
      val result = dao.get("8ff72391-7f49-4e37-a688-7f37a7864532")(None)
      result.right.get.map(_.name).contains("David") mustBe true
      result.right.get.map(_.surname).contains("Salgado") mustBe true
      result.right.get.map(_.phoneNumber).contains("0544410338") mustBe true
      result.right.get.map(_.email).contains("d.salgado@pegb.tech") mustBe true
    }

    "insert contacts" in {

      val dto = ContactToInsert(
        uuid = "c7e0f14c-4904-4abd-8811-c4ef2ab24a12",
        userUuid = "bcc32571-cf16-4abc-ac38-38d58f9cbab5",
        contactType = "secret contact",
        name = "George",
        middleName = None,
        surname = "Ogalo",
        phoneNumber = "0547893121",
        email = "g.ogalo@pegb.tech",
        idType = "Not Available",
        createdBy = "test_user",
        createdAt = LocalDateTime.now,
        isActive = true)

      val result = dao.insert(dto)(None)
      result.right.get.uuid == dto.uuid
      result.right.get.userId == Some(1)
      result.right.get.name == dto.name
    }

    "update contacts" in {
      val dto = ContactToUpdate(
        surname = Some("Parvatham"),
        phoneNumber = Some("123456789"),
        email = Some("u.parvatham@pegb.tech"),
        updatedBy = "unit_test",
        updatedAt = LocalDateTime.of(2019, 2, 20, 10, 30, 15),
        lastUpdatedAt = None)
      val result = dao.update("94ede5d6-3a6c-41f8-bb0f-63bc99c56436", dto)

      result.right.get.map(_.uuid) mustBe Some("94ede5d6-3a6c-41f8-bb0f-63bc99c56436")
      result.right.get.map(_.surname) mustBe Some("Parvatham")
      result.right.get.map(_.phoneNumber) mustBe Some("123456789")
      result.right.get.map(_.email) mustBe Some("u.parvatham@pegb.tech")
    }

    "fail to update contacts if last_updated_at is not equal to updated_at" in {
      val dto = ContactToUpdate(
        surname = Some("Jollibee"),
        updatedBy = "unit_test",
        updatedAt = LocalDateTime.now(),
        lastUpdatedAt = None)
      val result = dao.update("94ede5d6-3a6c-41f8-bb0f-63bc99c56436", dto)

      result.left.get.message mustBe "Update failed. Contact with id [94ede5d6-3a6c-41f8-bb0f-63bc99c56436] has been modified by another process."
    }

    "get contacts by user uuid" in {
      val criteria = ContactsCriteria(userUuid = Some(CriteriaField[String]("uuid", "bcc32571-cf16-4abc-ac38-38d58f9cbab5")))
      val results = dao.getByCriteria(criteria, None, None, None)
      val expected = Seq(
        ("94ede5d6-3a6c-41f8-bb0f-63bc99c56436", "Parvatham"),
        ("c7e0f14c-4904-4abd-8811-c4ef2ab24a12", "Ogalo")
      )
      results.right.get.map(c ⇒ (c.uuid, c.surname)) mustBe expected
    }

    "get contacts by business application id" in {
      val criteria = ContactsCriteria(buApplicUuid = Some(CriteriaField[String]("business_user_applications.uuid", "926551f7-84f6-459e-bf15-a76d07d1b712")))
      val results = dao.getByCriteria(criteria, None, None, None)
      val expected = Seq(
        ("8ff72391-7f49-4e37-a688-7f37a7864532", "Salgado"))
      results.right.get.map(c ⇒ (c.uuid, c.surname)) mustBe expected
    }

    "filter contacts by partial match on phone_number and order by created_at descending and surname ascending" in {
      val criteria = ContactsCriteria(phoneNumber = Some(CriteriaField[String]("phone_number", "054", MatchTypes.Partial)))
      val ordering = OrderingSet(Seq("created_at" → "DESC", "surname" → "ASC"))
      val results = dao.getByCriteria(criteria, Some(ordering), None, None)

      val expected = Seq(
        ("c7e0f14c-4904-4abd-8811-c4ef2ab24a12", "George"),
        ("8ff72391-7f49-4e37-a688-7f37a7864532", "David"),
        ("e966e237-b453-4fde-a141-f529de45b527", "Ornel Lloyd"),
      )

      results.right.get.map(c⇒ (c.uuid, c.name)) mustBe expected
    }

    "get all contacts and paginate with limit 3 and offset 2" in {
      val results = dao.getByCriteria(ContactsCriteria(), None, Some(3), Some(2))

      val expected = Seq(
        ("94ede5d6-3a6c-41f8-bb0f-63bc99c56436", "Ujali"),
        ("e24c8971-c8e1-49da-94d5-73062957846c", "Tanmoy"),
        ("c7e0f14c-4904-4abd-8811-c4ef2ab24a12", "George")
      )

      results.right.get.map(c⇒ (c.uuid, c.name)) mustBe expected
    }

    "filter with vp_user_id" in {
      val criteria = ContactsCriteria(vpUserId = Some(CriteriaField[Int]("vp_user_id", 1)))
      val results = dao.getByCriteria(criteria, None, None, None)

      val expected = Seq(
        ("e24c8971-c8e1-49da-94d5-73062957846c", "Tanmoy"))

      results.right.get.map(c⇒ (c.uuid, c.name)) mustBe expected
    }
  }

}
