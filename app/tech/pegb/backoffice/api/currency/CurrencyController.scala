package tech.pegb.backoffice.api.currency

import com.google.inject.ImplementedBy
import play.api.mvc.{Action, AnyContent}

@ImplementedBy(classOf[controllers.CurrencyController])
trait CurrencyController {

  def fetchCurrenciesWithId: Action[AnyContent]

}
