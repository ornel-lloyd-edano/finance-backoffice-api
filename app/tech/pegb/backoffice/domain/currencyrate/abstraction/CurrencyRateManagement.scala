package tech.pegb.backoffice.domain.currencyrate.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.currencyrate.dto.CurrencyRateToUpdate
import tech.pegb.backoffice.domain.currencyrate.implementation.CurrencyRateMgmtService
import tech.pegb.backoffice.domain.currencyrate.model.CurrencyRate
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[CurrencyRateMgmtService])
trait CurrencyRateManagement {

  def getCurrencyRateList(sorter: Option[Ordering], showEmpty: Option[Boolean]): Future[ServiceResponse[Seq[CurrencyRate]]]

  def updateCurrencyRateList(
    id: Int,
    lastUpdatedAt: Option[LocalDateTime],
    currencyToUpdate: CurrencyRateToUpdate): Future[ServiceResponse[Seq[CurrencyRate]]]

  def getCurrencyRateById(currencyId: Long): Future[ServiceResponse[CurrencyRate]]
}

