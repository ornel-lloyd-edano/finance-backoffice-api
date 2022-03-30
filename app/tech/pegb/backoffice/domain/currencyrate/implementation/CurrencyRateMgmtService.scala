package tech.pegb.backoffice.domain.currencyrate.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.core.integration.abstraction.CurrencyExchangeCoreApiClient
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.currencyexchange.abstraction.CurrencyExchangeDao
import tech.pegb.backoffice.domain.currencyexchange.dto.CurrencyExchangeCriteria
import tech.pegb.backoffice.dao.model.Ordering
import tech.pegb.backoffice.domain.currencyrate.abstraction.CurrencyRateManagement
import tech.pegb.backoffice.domain.currencyrate.dto.CurrencyRateToUpdate
import tech.pegb.backoffice.domain.currencyrate.model.{CurrencyRate, ExchangeRate, Rate}
import tech.pegb.backoffice.domain.{BaseService, ServiceError, model}
import tech.pegb.backoffice.mapping.dao.domain.currency.Implicit._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.currencyexchange.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

class CurrencyRateMgmtService @Inject() (
    conf: AppConfig,
    currencyDao: CurrencyDao,
    currencyExchangeDao: CurrencyExchangeDao,
    executionContexts: WithExecutionContexts,
    fxApiClient: CurrencyExchangeCoreApiClient) extends CurrencyRateManagement with BaseService {

  import tech.pegb.backoffice.util.Implicits._

  private implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  def getCurrencyRateList(sorter: Option[model.Ordering], showEmpty: Option[Boolean]): Future[ServiceResponse[Seq[CurrencyRate]]] = {
    (for {
      currencySet ← EitherT.fromEither[Future](currencyDao.getAll.asServiceResponse)
      allCurrencyExchange ← EitherT.fromEither[Future](
        currencyExchangeDao.getCurrencyExchangeByCriteria(
          CurrencyExchangeCriteria().asDao,
          Seq(Ordering("base_currency", Ordering.DESC)), None, None).asServiceResponse) //sorted by base_currency
    } yield {
      val activeCurrencySet = currencySet.filter(_.isActive)
      val currencyLookUpMap = activeCurrencySet.map(c ⇒ c.id.toLong → c).toMap
      val sortedCurrencyList = sorter match {
        case Some(model.Ordering("code", model.Ordering.ASCENDING)) ⇒ activeCurrencySet.toSeq.sortBy(_.name)
        case Some(model.Ordering("code", model.Ordering.DESCENDING)) ⇒ activeCurrencySet.toSeq.sortBy(_.name)(math.Ordering[String].reverse)
        case Some(model.Ordering("id", model.Ordering.DESCENDING)) ⇒ activeCurrencySet.toSeq.sortBy(_.id)(math.Ordering[Int].reverse)
        case _ ⇒ activeCurrencySet.toSeq.sortBy(_.id) //default is sort by ID (KES == 1)
      }
      val currencyRateList = sortedCurrencyList.map({ currency ⇒
        val exchangeWithCurrencyAsTarget = allCurrencyExchange.filter(_.currencyId == currency.id) //get all fx where targetCurrency is the currency (e.g. USDKES, GBPKES, EURKES)
        val exchangeWithCurrencyAsBaseMap = allCurrencyExchange.filter(_.baseCurrencyId == currency.id) //get all fx where baseCurrency is the currency (e.g. KESUSD, KESGBP, KESEUR)
          .map(fx ⇒ fx.currencyId → fx).toMap //create a map where key is the targetCurrency (USD -> KESUSD, GBP -> KESGBP)

        val rate = exchangeWithCurrencyAsTarget.flatMap({ fx ⇒
          exchangeWithCurrencyAsBaseMap.get(fx.baseCurrencyId) match {
            case None ⇒
              logger.warn(s"Currency Exchange $currency to ${fx.baseCurrency} not found.")
              none[Rate]
            case Some(reverseFx) ⇒
              Rate(
                code = fx.baseCurrency,
                description = currencyLookUpMap.get(fx.baseCurrencyId).flatMap(_.description),
                buyRate = ExchangeRate(id = UUID.fromString(fx.uuid), rate = fx.rate),
                sellRate = ExchangeRate(id = UUID.fromString(reverseFx.uuid), rate = reverseFx.rate.getReciprocal)).some
          }
        })

        CurrencyRate(
          mainCurrency = currency.asDomain,
          rates = rate)
      })
      showEmpty match {
        case Some(true) ⇒ currencyRateList
        case _ ⇒ currencyRateList.filter(_.rates.nonEmpty)
      }
    }).value
  }

  def getCurrencyRateById(currencyId: Long): Future[ServiceResponse[CurrencyRate]] = {

    (for {
      currencyList ← EitherT(getCurrencyRateList(None, None))
      currency ← EitherT.fromOption[Future](currencyList.find(c ⇒ c.mainCurrency.id == currencyId), notFoundError(s"CurrencyId $currencyId not found"))
    } yield {
      currency
    }).value
  }

  def updateCurrencyRateList(
    id: Int,
    lastUpdatedAt: Option[LocalDateTime],
    currencyToUpdate: CurrencyRateToUpdate): Future[ServiceResponse[Seq[CurrencyRate]]] = {

    implicit val fakeRequestUuid: UUID = UUID.randomUUID()

    val allRates = currencyToUpdate.rates.map(currencyRate ⇒
      currencyRate.copy(sellRate = currencyRate.sellRate.copy(rate = currencyRate.sellRate.rate.getReciprocal)))
    val allExchangeRates = allRates.map(_.sellRate) union allRates.map(_.buyRate)

    (for {
      allFx ← EitherT.fromEither[Future] {
        val allUuids = allExchangeRates.map(_.id.toString)
        currencyExchangeDao.findByMultipleUuid(allUuids).asServiceResponse
      }

      jsValue ← EitherT.fromEither[Future] {
        val jsValueEither = allExchangeRates.map(cr ⇒
          allFx.find(_.uuid == cr.id.toString).fold[Either[ServiceError, (String, BigDecimal)]](Left(notFoundError(s"no relative id found for uuid $cr")))(fx ⇒ Right((fx.id.toString, cr.rate))))

        sequence(jsValueEither).map(_.toMap)
      }

      fxApiResult ← EitherT.liftF(fxApiClient.batchUpdateFxStatus(Seq.empty, "",
        "updated currency rates", currencyToUpdate.updatedBy, lastUpdatedAt, jsValue))

      allCurrencyList ← EitherT {
        val result: Future[ServiceResponse[Seq[CurrencyRate]]] = if (fxApiResult.isRight) {

          getCurrencyRateList(none, none)
        } else {
          Future.successful(
            Left(ServiceError.unknownError("Error from wallet core api. Please check logs.", fakeRequestUuid.toOption)))
        }

        result
      }
    } yield allCurrencyList).value

  }

  private def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, Seq[B]]) {
      (e, acc) ⇒ for (xs ← acc.right; x ← e.right) yield x +: xs
    }
}
