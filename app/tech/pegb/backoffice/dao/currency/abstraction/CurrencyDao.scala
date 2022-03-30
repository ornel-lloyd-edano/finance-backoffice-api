package tech.pegb.backoffice.dao.currency.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao

import tech.pegb.backoffice.dao.currency.dto.{CurrencyToUpdate, CurrencyToUpsert}
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao

@ImplementedBy(classOf[CurrencySqlDao])
trait CurrencyDao extends Dao {
  def update(id: Int, currencyToUpdate: CurrencyToUpdate): DaoResponse[Option[Currency]]

  def getAll: DaoResponse[Set[Currency]]

  def getAllNames: DaoResponse[Set[String]]

  def getCurrenciesWithId(hasIsActiveFilter: Option[Boolean] = None): DaoResponse[List[(Int, String)]]

  def getCurrenciesWithIdExtended: DaoResponse[List[(Int, String, String)]]

  def isCurrencyActive(currencyName: String): DaoResponse[Boolean]

  def bulkUpsert(dto: Seq[CurrencyToUpsert], createdAt: LocalDateTime, createdBy: String): DaoResponse[Seq[Currency]]
}
