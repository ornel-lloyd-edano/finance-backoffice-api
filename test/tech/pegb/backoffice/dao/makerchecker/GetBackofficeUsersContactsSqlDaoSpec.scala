package tech.pegb.backoffice.dao.makerchecker

import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.makerchecker.abstraction.GetBackofficeUsersContactsDao
import tech.pegb.core.PegBTestApp

class GetBackofficeUsersContactsSqlDaoSpec extends PegBTestApp with MockFactory {

  override def initSql =
    s"""
       |INSERT INTO business_units
       |(id, name, is_active, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('925c8d2f-0501-4b62-8c6f-39d0fbaa48ab', 'Finance Dept', '1', 'admin', null, '2019-01-01 00:00:00', null),
       |('231c8d2f-0501-4b62-8c6f-39d0fbaa4801', 'Support Dept', '1', 'admin', null, '2019-01-01 00:00:00', null);;
       |
       |INSERT INTO roles
       |(id, name, is_active, created_by, updated_by, created_at, updated_at, level)
       |VALUES
       |('06d18f41-1abf-4507-afab-5f8e1c7a1601', 'Department Sub Head', '1', 'admin', null, '2019-01-01 00:00:00', null, '2'),
       |('17da8f41-1abf-4507-afab-5f8e1c7a1311', 'Department Head',     '1', 'admin', null, '2019-01-01 00:00:00', null, '1'),
       |('06a27f40-1abf-4507-afab-5f8e1c7a1aaa', 'normal employee',     '1', 'admin', null, '2019-01-01 00:00:00', null, '3');
       |
       |INSERT INTO back_office_users (id,userName,password,businessUnitId,roleId,email,phoneNumber,firstName,middleName,lastName,description,homePage,is_active,activeLanguage,customData,lastLoginTimestamp,created_at,updated_at,created_by,updated_by) VALUES
       |(uuid(),'pegbuser1','4A0F346D7A83912ED6E28BEC4E3014FA8F242D745620ACE9D930B287288C529B',
       |'925c8d2f-0501-4b62-8c6f-39d0fbaa48ab','06d18f41-1abf-4507-afab-5f8e1c7a1601',   'pegbuser1@pegb.tech','971557200221',
       |'Lloyd','P.','Edano','some description test','https://pegb.tech',1,NULL,NULL,1546940581250,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'system','system'),
       |
       |(uuid(),'pegbuser2','4A0F346D7A83912ED6E28BEC4E3014FA8F242D745620ACE9D930B287288C529B',
       |'925c8d2f-0501-4b62-8c6f-39d0fbaa48ab','17da8f41-1abf-4507-afab-5f8e1c7a1311',  'pegbuser2@pegb.tech','971557200222',
       |'George',null,'Ogalo','some description test','https://pegb.tech',1,NULL,NULL,1546940581250,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'system','system'),
       |
       |(uuid(),'pegbuser3','4A0F346D7A83912ED6E28BEC4E3014FA8F242D745620ACE9D930B287288C529B',
       |'925c8d2f-0501-4b62-8c6f-39d0fbaa48ab','06a27f40-1abf-4507-afab-5f8e1c7a1aaa',  'pegbuser3@pegb.tech','971557200223',
       |'Alex',null,'Kim','some description test','https://pegb.tech',1,NULL,NULL,1546940581250,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'system','system');
    """.stripMargin

  override def cleanupSql: String =
    s"""
       |DELETE FROM business_units;
       |DELETE FROM roles;
       |DELETE FROM back_office_users;
     """.stripMargin

  val getBackofficeUserContactsDao = inject[GetBackofficeUsersContactsDao]

  "GetBackofficeUsersContactsSqlDao" should {
    "get all backoffice user id, email and phone number" in {

      val result = getBackofficeUserContactsDao.getBackofficeUsersContactsByRoleLvlAndBusinessUnit(2, "Finance Dept")

      result.isRight mustBe true
      result.right.get.size mustBe 2
    }
  }

}
