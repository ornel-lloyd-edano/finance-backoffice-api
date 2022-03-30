package test.tech.pegb.backoffice.util

import java.util.UUID

import tech.pegb.backoffice.util.UUIDLike
import tech.pegb.core.PegBNoDbTestApp

import scala.util.Try

class UUIDLikeTest extends PegBNoDbTestApp {

  "UUIDLike" should {
    "read a partial UUID" in {
      val success = Try(UUIDLike("f8c4ec75-e714-"))
      success.isSuccess mustBe true
    }

    "unable to read a string that is less than 4 characters" in {
      val fail = Try(UUIDLike("f8c"))
      fail.isFailure mustBe true
    }

    "unable to read a string which is not a partial UUID" in {
      val fail = Try(UUIDLike("f8c4ec@#$%!"))
      fail.isFailure mustBe true
    }

    "able to convert a partial string like UUID to actual UUID" in {
      val uuid = UUIDLike("f8c4ec75-e714-").toUUID.get
      val expected = UUID.fromString("f8c4ec75-e714-0000-0000-000000000000")
      uuid mustBe expected
    }
  }
}
