package tech.pegb.backoffice.api.currency.dto

import java.util.Currency

case class ExtendedCurrency(
    id: Int,
    code: String,
    description: String)

object ExtendedCurrency {

  def from(t: (Int, Currency, String)): ExtendedCurrency = {
    ExtendedCurrency(
      id = t._1,
      code = t._2.getCurrencyCode,
      description = t._3)
  }

}
