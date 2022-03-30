package tech.pegb.backoffice.domain.currency.implementation

import java.util.Currency

import javax.inject.Inject
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.mapping.dao.domain.Implicits._

import scala.util.Try

class CurrencyService @Inject() (
    currencyDao: CurrencyDao)
  extends tech.pegb.backoffice.domain.currency.CurrencyService {

  override def fetchCurrencies: ServiceResponse[Set[Currency]] = {
    for {
      names ← currencyDao.getAllNames.asServiceResponse
    } yield names.map(Currency.getInstance)
  }

  override def fetchCurrenciesWithIds: ServiceResponse[List[(Int, Currency)]] = {
    for {
      namesWithIds ← currencyDao.getCurrenciesWithId().asServiceResponse
    } yield {
      namesWithIds.flatMap { ni ⇒
        val code = ni._2
        val currencyOrError = Try(Currency.getInstance(code)).map(ni._1 → _)
        currencyOrError.failed.foreach(exc ⇒ logger.warn(s"Couldn't convert $code to currency", exc))
        currencyOrError.toOption
      }
    }
  }

  override def fetchCurrenciesWithIdsExtended: ServiceResponse[List[(Int, Currency, String)]] = {
    for {
      namesWithIds ← currencyDao.getCurrenciesWithIdExtended.asServiceResponse
    } yield {
      namesWithIds.flatMap { ni ⇒
        val code = ni._2
        val currencyOrError = Try(Currency.getInstance(code)).map(c ⇒ (ni._1, c, ni._3))
        currencyOrError.failed.foreach(exc ⇒ logger.warn(s"Couldn't convert $code to currency", exc))
        currencyOrError.toOption
      }
    }
  }

}
