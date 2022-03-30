package tech.pegb.backoffice.domain.currency

import java.util.Currency

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService

@ImplementedBy(classOf[implementation.CurrencyService])
trait CurrencyService extends BaseService {

  def fetchCurrencies: ServiceResponse[Set[Currency]]

  def fetchCurrenciesWithIds: ServiceResponse[List[(Int, Currency)]]

  def fetchCurrenciesWithIdsExtended: ServiceResponse[List[(Int, Currency, String)]]

}
