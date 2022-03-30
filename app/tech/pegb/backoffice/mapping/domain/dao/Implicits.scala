package tech.pegb.backoffice.mapping.domain.dao

import java.util.UUID

import tech.pegb.backoffice.dao.Dao.{EntityId, IntEntityId, UUIDEntityId}
import tech.pegb.backoffice.domain.model.{Order ⇒ DomainOrder, Ordering ⇒ DomainOrdering}
import tech.pegb.backoffice.dao.model.{MatchType, MatchTypes, Order, Ordering, OrderingSet ⇒ DaoOrdering}

import scala.collection.mutable

object Implicits {

  implicit class PartialMatchAdapter(val partialMatchFields: Set[String]) extends AnyVal {

    def withMatchType(field: String): MatchType =
      if (partialMatchFields.contains(field)) MatchTypes.Partial else MatchTypes.Exact
  }

  implicit class OrderAdapter(val arg: DomainOrder) extends AnyVal {
    def asDao: Order = if (arg == DomainOrdering.ASCENDING) Ordering.ASC else Ordering.DESC
  }

  implicit class OrderingSeqAdapter(val arg: Seq[DomainOrdering]) extends AnyVal {
    def asDao: Option[DaoOrdering] = {
      val result: mutable.LinkedHashSet[Ordering] = arg.map(o ⇒ Ordering(o.field, o.order.asDao))
        .foldLeft(mutable.LinkedHashSet.empty[Ordering]) {
          (accumulated, current) ⇒
            {
              accumulated += current
            }
        }
      if (arg.nonEmpty) Option(DaoOrdering(result)) else None
    }
  }

  implicit class OrderingAdapter(val o: DomainOrdering) extends AnyVal {
    def asDao: Ordering = Ordering(o.field, o.order.asDao)
  }

  implicit class UUIDConverter(val id: UUID) extends AnyVal {
    def asEntityId: EntityId = UUIDEntityId(id)
  }

  implicit class IntConverter(val id: Int) extends AnyVal {
    def asEntityId: EntityId = IntEntityId(id)
  }
}
