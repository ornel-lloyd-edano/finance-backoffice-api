package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import cats.implicits._
import tech.pegb.backoffice.dao.customer.abstraction.VelocityPortalUserDao
import tech.pegb.backoffice.dao.customer.dto.VelocityPortalUsersCriteria
import tech.pegb.backoffice.dao.customer.entity.VelocityPortalUser
import tech.pegb.backoffice.dao.model.{MatchTypes, Ordering, OrderingSet}
import tech.pegb.core.PegBTestApp

class VelocityPortalUserSqlDaoSpec extends PegBTestApp {

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  private val dao = inject[VelocityPortalUserDao]

  override def initSql =
    s"""
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('1', '5744db00-2b50-4a34-8348-35f0030c3b1d', 'user01', 'pword', 'george@gmail.com',  '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null),
       |('2', '4634db00-2b61-4a23-8348-35f0030c3b1d', 'user02', 'pword', 'ujali@gmail.com',   '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null),
       |('3', '3be6a9e7-52ca-4e2c-a89c-35b480fdfdca', 'user03', 'pword', 'david@gmail.com',   '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null),
       |('4', '594c8e61-5e78-40ab-b85d-9556533b9c6f', 'user04', 'pword', 'alex@gmail.com',    '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null);
       |
       |INSERT INTO vp_users
       |(id, uuid, user_id, name, middle_name, surname, msisdn, email, password, created_by, created_at, updated_by, updated_at, username, role, last_login_at, status)
       |VALUES
       |(1, 'fa7fc119-b465-46cb-b271-260725f7c782', 1, 'Manny', 'Pacman', 'Pacquiao', '+63911111', 'pacman@gmail.com', 'jinky', 'pegbuser', '$now', 'pegbuser', '$now', 'pacman', 'admin', '$now', 'active'),
       |(2, '942aa4ae-55e5-4081-9a81-ce9672f36001', 1, 'Jinky', null, 'Pacquiao', '+63922222', 'jinky@gmail.com', 'mayweather', 'pegbuser', '$now', 'pegbuser', '$now', 'jinkyP', 'admin', '$now', 'active'),
       |(3, 'daa76627-198d-44c8-80d1-90aa2ecf2500', 1, 'Dionesia', 'Pacquiao', 'Tan', '+63933333', 'mommyd@gmail.com', 'diony', 'pegbuser', '$now', 'pegbuser', '$now', 'mommyD', 'admin', '$now', 'active'),
       |(4, 'fbc1b8f1-161e-4acc-82e8-9d168c77e186', 2, 'Henry', 'Tan', 'Sy', '+63944444', 'henrysy@gmail.com', 'megamall', 'pegbuser', '$now', 'pegbuser', '$now', 'sm_boss', 'admin', '$now', 'active');

     """.stripMargin

  val vp1 = VelocityPortalUser(
    id = 1,
    uuid = "fa7fc119-b465-46cb-b271-260725f7c782",
    userId = 1,
    userUUID = "5744db00-2b50-4a34-8348-35f0030c3b1d",
    name = "Manny",
    middleName = "Pacman".some,
    surname = "Pacquiao",
    msisdn = "+63911111",
    email = "pacman@gmail.com",
    username = "pacman",
    role = "admin",
    status = "active",
    lastLoginAt = now.some,
    createdBy = "pegbuser",
    createdAt = now,
    updatedBy = "pegbuser".some,
    updatedAt = now.some)

  val vp2 = VelocityPortalUser(
    id = 2,
    uuid = "942aa4ae-55e5-4081-9a81-ce9672f36001",
    userId = 1,
    userUUID = "5744db00-2b50-4a34-8348-35f0030c3b1d",
    name = "Jinky",
    middleName = None,
    surname = "Pacquiao",
    msisdn = "+63922222",
    email = "jinky@gmail.com",
    username = "jinkyP",
    role = "admin",
    status = "active",
    lastLoginAt = now.some,
    createdBy = "pegbuser",
    createdAt = now,
    updatedBy = "pegbuser".some,
    updatedAt = now.some)

  val vp3 = VelocityPortalUser(
    id = 3,
    uuid = "daa76627-198d-44c8-80d1-90aa2ecf2500",
    userId = 1,
    userUUID = "5744db00-2b50-4a34-8348-35f0030c3b1d",
    name = "Dionesia",
    middleName = "Pacquiao".some,
    surname = "Tan",
    msisdn = "+63933333",
    email = "mommyd@gmail.com",
    username = "mommyD",
    role = "admin",
    status = "active",
    lastLoginAt = now.some,
    createdBy = "pegbuser",
    createdAt = now,
    updatedBy = "pegbuser".some,
    updatedAt = now.some)

  val vp4 = VelocityPortalUser(
    id = 4,
    uuid = "fbc1b8f1-161e-4acc-82e8-9d168c77e186",
    userId = 2,
    userUUID = "4634db00-2b61-4a23-8348-35f0030c3b1d",
    name = "Henry",
    middleName = "Tan".some,
    surname = "Sy",
    msisdn = "+63944444",
    email = "henrysy@gmail.com",
    username = "sm_boss",
    role = "admin",
    status = "active",
    lastLoginAt = now.some,
    createdBy = "pegbuser",
    createdAt = now,
    updatedBy = "pegbuser".some,
    updatedAt = now.some)

  "Velocity Portal User Dao" should {
    "return count of all vp_users in countVelocityPortalUsersByCriteria - no filter " in {
      val criteria = VelocityPortalUsersCriteria()

      val resp = dao.countVelocityPortalUserByCriteria(criteria)

      resp mustBe Right(4)
    }
    "return count of all vp_users in countVelocityPortalUsersByCriteria - filter by user_id " in {
      val criteria = VelocityPortalUsersCriteria(
        userId = model.CriteriaField("", 1).some)

      val resp = dao.countVelocityPortalUserByCriteria(criteria)

      resp mustBe Right(3)
    }
    "return all vp_users in getVelocityPortalUsersByCriteria - no filter " in {
      val criteria = VelocityPortalUsersCriteria()

      val orderingSet = OrderingSet(Ordering("id", Ordering.ASC)).some
      val resp = dao.getVelocityPortalUsersByCriteria(criteria, orderingSet, None, None)

      resp mustBe Right(Seq(vp1, vp2, vp3, vp4))
    }
    "return all vp_users in getVelocityPortalUsersByCriteria - filter by user_id " in {
      val criteria = VelocityPortalUsersCriteria(
        userId = model.CriteriaField("", 1).some)

      val orderingSet = OrderingSet(Ordering("name", Ordering.DESC)).some
      val resp = dao.getVelocityPortalUsersByCriteria(criteria, orderingSet, None, None)

      resp mustBe Right(Seq(vp1, vp2, vp3))
    }
    "return all vp_users in getVelocityPortalUsersByCriteria - filter by name " in {
      val criteria = VelocityPortalUsersCriteria(
        name = model.CriteriaField("", "Tan", MatchTypes.Partial).some)

      val orderingSet = OrderingSet(Ordering("username", Ordering.DESC)).some
      val resp = dao.getVelocityPortalUsersByCriteria(criteria, orderingSet, None, None)

      resp mustBe Right(Seq(vp4, vp3))
    }
    "return all vp_users in getVelocityPortalUsersByCriteria - filter by username " in {
      val criteria = VelocityPortalUsersCriteria(
        username = model.CriteriaField("", "boss", MatchTypes.Partial).some)

      val resp = dao.getVelocityPortalUsersByCriteria(criteria, None, None, None)

      resp mustBe Right(Seq(vp4))
    }

    "return all vp_users in getVelocityPortalUsersByCriteria - filter by uuid" in {
      val criteria = VelocityPortalUsersCriteria(
        uuid = model.CriteriaField("", s"${vp1.uuid}", MatchTypes.Partial).some)

      val resp = dao.getVelocityPortalUsersByCriteria(criteria, None, None, None)

      resp mustBe Right(Seq(vp1))
    }

  }
}
