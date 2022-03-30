package tech.pegb.backoffice.domain.model

import org.scalatest.Ignore
import org.scalatestplus.play.PlaySpec

@Ignore
class OrderingTest extends PlaySpec {

  "Ordering" should {
    "convert a comma delimited string to Set[Ordering] using .asDomain" in {

      val orderByQueryParam = "-msisdn, status, -created_at, updated_by"

      import tech.pegb.backoffice.domain.model._
      import tech.pegb.backoffice.domain.model.Ordering._

      val expected = Set(
        Ordering("msisdn", Ordering.DESCENDING),
        Ordering("status", Ordering.ASCENDING),
        Ordering("created_at", Ordering.DESCENDING),
        Ordering("updated_by", Ordering.ASCENDING))

      orderByQueryParam.asDomain mustBe expected
    }

    "still properly convert a comma delimited string to Set[Ordering] using .asDomain even with missing items in list" in {

      val orderByQueryParam = "-msisdn, , status, -created_at, , updated_by"

      import tech.pegb.backoffice.domain.model._
      import tech.pegb.backoffice.domain.model.Ordering._

      val expected = Set(
        Ordering("msisdn", Ordering.DESCENDING),
        Ordering("status", Ordering.ASCENDING),
        Ordering("created_at", Ordering.DESCENDING),
        Ordering("updated_by", Ordering.ASCENDING))

      orderByQueryParam.asDomain mustBe expected
    }

    "still properly convert a comma delimited string to Set[Ordering] using .asDomain even with minus sign only in the list" in {

      val orderByQueryParam = "-msisdn, -, status, -created_at, -, updated_by"

      import tech.pegb.backoffice.domain.model._
      import tech.pegb.backoffice.domain.model.Ordering._

      val expected = Set(
        Ordering("msisdn", Ordering.DESCENDING),
        Ordering("status", Ordering.ASCENDING),
        Ordering("created_at", Ordering.DESCENDING),
        Ordering("updated_by", Ordering.ASCENDING))

      orderByQueryParam.asDomain mustBe expected
    }

    "convert a comma delimited string to Set[Ordering] using .asDomain, take ordering of last item if duplicate found in the list" in {

      val orderByQueryParam = "-msisdn, msisdn, status, -created_at, created_at, updated_by"

      import tech.pegb.backoffice.domain.model._
      import tech.pegb.backoffice.domain.model.Ordering._

      val expected = Set(
        Ordering("msisdn", Ordering.ASCENDING),
        Ordering("status", Ordering.ASCENDING),
        Ordering("created_at", Ordering.ASCENDING),
        Ordering("updated_by", Ordering.ASCENDING))

      orderByQueryParam.asDomain mustBe expected
    }
  }

}
