package test.tech.pegb.backoffice.api.json

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import org.scalatestplus.play.PlaySpec
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.util.Implicits._

class BackOfficeApiJsonTest extends PlaySpec {

  "backoffice.api.json.Implicits" should {
    "write ZoneDateTime from LocalDateTime without milliseconds" in {

      val zdt = LocalDateTime.of(2019, 5, 23, 10, 30, 16, 23567493).toZonedDateTimeUTC

      zdt.toJsonStr mustBe "\"2019-05-23T10:30:16Z\""
    }

    "read LocalDateTime from ZoneDateTime Africa/Nairobi without milliseconds" in {

      val ldt = ZonedDateTime.of(2019, 5, 23, 10, 30, 16, 23567493, ZoneId.of("Africa/Nairobi")).toLocalDateTimeUTC

      ldt mustBe LocalDateTime.of(2019, 5, 23, 7, 30, 16)
    }

    "read LocalDateTime from ZoneDateTime UTC without milliseconds" in {

      val ldt = ZonedDateTime.of(2019, 5, 23, 10, 30, 16, 23567493, ZoneId.of("UTC")).toLocalDateTimeUTC

      ldt mustBe LocalDateTime.of(2019, 5, 23, 10, 30, 16)
    }
  }

}
