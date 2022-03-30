package tech.pegb.backoffice

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.db.{DBApi, Database}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, route, _}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.api.json.Implicits._
import org.scalatest.Matchers._
import tech.pegb.backoffice.api.customer.controllers.BusinessUserController

class BusinessUserIntegrationTest extends PlayIntegrationTest with MockFactory with ScalaFutures {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  val dbNow = now.toZonedDateTimeUTC.toLocalDateTimeUTC

  override def initSql =
    s"""
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('16', '5744db00-2b50-4a34-8348-35f0030c3b1d', 'george_bu', 'pword', 'george@gmail.com',  '$dbNow', null, 'sub_one',  'business', 'active', null, null, 'SuperUser', '$dbNow', 'pegbuser', '$dbNow'),
       |('17', '3be6a9e7-52ca-4e2c-a89c-35b480fdfdca', 'dave_iu', 'pword', 'david@gmail.com', '$dbNow', null, 'sub_one',  'individual', 'active', null, null, 'SuperUser', '$dbNow', 'pegbuser', '$dbNow'),
       |('18', '370847b2-5993-435f-8c56-086f8726c0c7', 'sm_bu', 'pword', 'sm_supermall@gmail.com',  '$dbNow', null, 'sub_one',  'business', 'active', null, null, 'SuperUser', '$dbNow', 'pegbuser', '$dbNow');
       |
       |INSERT INTO vp_users
       |(id, uuid, user_id, name, middle_name, surname, msisdn, email, password, created_by, created_at, updated_by, updated_at, username, role, last_login_at, status)
       |VALUES
       |(1, 'fa7fc119-b465-46cb-b271-260725f7c782', 16, 'Manny', 'Pacman', 'Pacquiao', '+63911111', 'pacman@gmail.com', 'jinky', 'pegbuser', '$dbNow', 'pegbuser', '$dbNow', 'pacman', 'admin', '$dbNow', 'active'),
       |(2, '942aa4ae-55e5-4081-9a81-ce9672f36001', 16, 'Jinky', null, 'Pacquiao', '+63922222', 'jinky@gmail.com', 'mayweather', 'pegbuser', '$dbNow', 'pegbuser', '$dbNow', 'jinkyP', 'admin', '$dbNow', 'active'),
       |(3, 'daa76627-198d-44c8-80d1-90aa2ecf2500', 16, 'Dionesia', 'Pacquiao', 'Tan', '+63933333', 'mommyd@gmail.com', 'diony', 'pegbuser', '$dbNow', 'pegbuser', '$dbNow', 'mommyD', 'admin', '$dbNow', 'active'),
       |(4, 'fbc1b8f1-161e-4acc-82e8-9d168c77e186', 18, 'Henry', 'Tan', 'Sy', '+63944444', 'henrysy@gmail.com', 'megamall', 'pegbuser', '$dbNow', 'pegbuser', '$dbNow', 'sm_boss', 'admin', '$dbNow', 'active');
       |""".stripMargin

  override val endpoint = s"/api/${inject[BusinessUserController].getRoute}"

  val db: Database = inject[DBApi].database("backoffice")


  "BusinessUser api" should {
    "get velocity portal users in GET /api/business_users/:id/velocity_portal_users" in {
      val fakeRequest = FakeRequest(GET, s"$endpoint/5744db00-2b50-4a34-8348-35f0030c3b1d/velocity_portal_users?order_by=name").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{
           |"total":3,
           |"results":[
           |{"id":"daa76627-198d-44c8-80d1-90aa2ecf2500",
           |"name":"Dionesia",
           |"middle_name":"Pacquiao",
           |"surname":"Tan",
           |"full_name":"Dionesia Pacquiao Tan",
           |"msisdn":"+63933333",
           |"email":"mommyd@gmail.com",
           |"username":"mommyD",
           |"role":"admin",
           |"status":"active",
           |"last_login_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"created_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${dbNow.toZonedDateTimeUTC.toJsonStr}
           |},
           |{"id":"942aa4ae-55e5-4081-9a81-ce9672f36001",
           |"name":"Jinky",
           |"middle_name":null,
           |"surname":"Pacquiao",
           |"full_name":"Jinky Pacquiao",
           |"msisdn":"+63922222",
           |"email":"jinky@gmail.com",
           |"username":"jinkyP",
           |"role":"admin",
           |"status":"active",
           |"last_login_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"created_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${dbNow.toZonedDateTimeUTC.toJsonStr}
           |},
           |{"id":"fa7fc119-b465-46cb-b271-260725f7c782",
           |"name":"Manny",
           |"middle_name":"Pacman",
           |"surname":"Pacquiao",
           |"full_name":"Manny Pacman Pacquiao",
           |"msisdn":"+63911111",
           |"email":"pacman@gmail.com",
           |"username":"pacman",
           |"role":"admin",
           |"status":"active",
           |"last_login_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"created_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${dbNow.toZonedDateTimeUTC.toJsonStr}
           |}],
           |"limit":null,
           |"offset":null}""".stripMargin.replace(System.lineSeparator(), "")
      contentAsString(resp) mustBe expectedJson

    }
    "return error GET /api/business_users/:id/velocity_portal_users if user.type is not business" in {
      val fakeRequest = FakeRequest(GET, s"$endpoint/3be6a9e7-52ca-4e2c-a89c-35b480fdfdca/velocity_portal_users").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      contentAsString(resp) should include("User 3be6a9e7-52ca-4e2c-a89c-35b480fdfdca is not a business user")
    }
  }
}
