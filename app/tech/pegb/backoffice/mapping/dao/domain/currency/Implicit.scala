package tech.pegb.backoffice.mapping.dao.domain.currency

import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.domain.currency.model

object Implicit {

  implicit class CurrencyAdapter(val arg: Currency) extends AnyVal {
    def asDomain = {
      model.Currency(
        id = arg.id,
        code = arg.name,
        description = arg.description)
    }
  }
}
