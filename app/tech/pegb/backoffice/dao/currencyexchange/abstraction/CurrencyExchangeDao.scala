package tech.pegb.backoffice.dao.currencyexchange.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao

import tech.pegb.backoffice.dao.currencyexchange.dto.CurrencyExchangeCriteria
import tech.pegb.backoffice.dao.currencyexchange.entity.CurrencyExchange
import tech.pegb.backoffice.dao.currencyexchange.sql.CurrencyExchangeSqlDao
import tech.pegb.backoffice.dao.model.Ordering

@ImplementedBy(classOf[CurrencyExchangeSqlDao])
trait CurrencyExchangeDao extends Dao {

  def getDailyAmount(targetCurrencyAccountId: Long, baseCurrencyAccountId: Long): DaoResponse[Option[BigDecimal]]

  def countTotalCurrencyExchangeByCriteria(criteria: CurrencyExchangeCriteria): DaoResponse[Int]

  def getCurrencyExchangeByCriteria(criteria: CurrencyExchangeCriteria, orderBy: Seq[Ordering] = Nil, limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[CurrencyExchange]]

  def findById(id: UUID): DaoResponse[CurrencyExchange]

  def findById(id: Int): DaoResponse[CurrencyExchange]

  def findByMultipleUuid(uuids: Seq[String]): DaoResponse[Seq[CurrencyExchange]]

  def updateCurrencyExchangeStatus(id: Int, status: String): DaoResponse[Boolean]
}
