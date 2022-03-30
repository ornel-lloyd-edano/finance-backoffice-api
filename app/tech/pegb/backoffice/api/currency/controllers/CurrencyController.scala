package tech.pegb.backoffice.api.currency.controllers

import com.google.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.api.currency.dto.ExtendedCurrency
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.currency.CurrencyService
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.util.AppConfig

class CurrencyController @Inject() (
    controllerComponents: ControllerComponents,
    implicit val appConfig: AppConfig,
    currencyService: CurrencyService)
  extends ApiController(controllerComponents) with tech.pegb.backoffice.api.currency.CurrencyController
  with RequiredHeaders with ConfigurationHeaders {

  override def fetchCurrenciesWithId: Action[AnyContent] = Action { implicit ctx ⇒
    implicit val id = getRequestId
    handleApiResponse {
      currencyService.fetchCurrenciesWithIdsExtended
        .map(_.map(ExtendedCurrency.from)).map(_.toJsonStr).left.map(_.asApiError())
    }
  }

}
