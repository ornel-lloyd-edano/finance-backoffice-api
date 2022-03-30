package tech.pegb.backoffice.api.currencyexchange.controller

import java.util.Currency
import scala.language.implicitConversions

object TestHelper {

  implicit def stringToCurrency(currencyCode: String): Currency = {
    Currency.getInstance(currencyCode)
  }

}
