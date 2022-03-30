package tech.pegb.backoffice.dao.aggregations.sql

import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.aggregations.abstraction.GenericAggregationDao
import tech.pegb.backoffice.dao.aggregations.dto.{AggregationInput, AggregationResult, Entity, GroupByInput}

import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}

@Singleton
class AggNdbDao @Inject() (val dbApi: DBApi) extends GenericAggregationDao with SqlDao {

  def aggregate(
    entity: Seq[Entity],
    expressionsToAggregate: Seq[AggregationInput],
    criteria: Seq[CriteriaField[_]],
    groupBy: Seq[GroupByInput],
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[AggregationResult]] = ???
}

