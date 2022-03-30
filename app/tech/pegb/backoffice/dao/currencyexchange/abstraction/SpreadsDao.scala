package tech.pegb.backoffice.dao.currencyexchange.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.currencyexchange.dto.{SpreadCriteria, SpreadToInsert, SpreadUpdateDto}
import tech.pegb.backoffice.dao.currencyexchange.entity.Spread
import tech.pegb.backoffice.dao.currencyexchange.sql.SpreadsSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[SpreadsSqlDao])
trait SpreadsDao extends Dao {

  def getSpread(id: UUID): DaoResponse[Option[Spread]]

  def getSpreadsByCriteria(criteria: SpreadCriteria, ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[Spread]]

  def countSpreadsByCriteria(criteria: SpreadCriteria): DaoResponse[Int]

  def createSpread(spreadToInsert: SpreadToInsert): DaoResponse[Spread]

  def update(id: UUID, dto: SpreadUpdateDto)(implicit requestId: UUID): DaoResponse[Spread]

  @deprecated("use update with deletedAt set in SpreadUpdateDto", "")
  def delete(id: UUID, uAt: LocalDateTime, uBy: String)(implicit requestId: UUID): DaoResponse[Spread]
}
