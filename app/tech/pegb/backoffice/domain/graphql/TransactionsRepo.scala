package tech.pegb.backoffice.domain.graphql

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import com.google.inject.Inject
import com.google.inject.name.Named
import tech.pegb.backoffice.dao.model.GroupingField
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.model.TransactionAggregatation
import tech.pegb.backoffice.domain.settings.abstraction.SystemSettingService
import tech.pegb.backoffice.domain.settings.dto.SystemSettingsCriteria
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionService
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.domain.transaction.model.{Transaction, TransactionStatus}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, Logging, WithExecutionContexts}

import scala.concurrent.Future

object TransactionDirection extends Enumeration {
  val debit, credit = Value
}

case class TransactionQueryArgs(
    limit: Option[Int] = None,
    offset: Option[Int] = None,
    status: Option[String],
    `type`: Option[String],
    direction: Option[String],
    from_date: Option[String],
    to_date: Option[String],
    customerId: Option[UUID],
    accountId: Option[UUID],
    channel: Option[String],
    currency: Option[String],
    onlyFloatAccounts: Boolean = false,
    accountType: Option[String])

class TransactionRepo @Inject() (
    @Named("TransactionsReportingService") transactionService: TransactionService,
    appConfig: AppConfig,
    systemSettingService: SystemSettingService,
    executionContexts: WithExecutionContexts) extends Logging {

  import TransactionRepo._

  val DefaultOrdering = "created_at,sequence"

  implicit val ec = executionContexts.genericOperations

  def transaction(id: String): Future[Option[Transaction]] =
    transactionService.getById(id).map {
      serviceResponse ⇒
        serviceResponse.fold(
          _ ⇒ None,
          transaction ⇒ transaction.some)
    }

  def transactions(args: TransactionQueryArgs): Future[Seq[Transaction]] = {
    val criteria = buildCriteria(args)
    val ordering = DefaultOrdering.asDomain
    if (args.onlyFloatAccounts) {
      val getSystemSetting = systemSettingService.getSystemSettingsByCriteria(
        SystemSettingsCriteria(key = Some(appConfig.FloatAccountNumbersKey)).toOption)
      getSystemSetting
        .flatMap(c ⇒ c.fold(
          e ⇒ {
            logger.error(s"failed to fetch system setting ${appConfig.FloatAccountNumbersKey}: $e")
            Future.successful(List.empty)
          },
          mayBeSystemSetting ⇒ {
            mayBeSystemSetting.headOption.fold[Future[Seq[Transaction]]] {
              logger.error(s"no system settings found for key ${appConfig.FloatAccountNumbersKey}")
              Future.successful(List.empty)
            } {
              systemSetting ⇒
                val accountNumbers1 = systemSetting.value
                  .replaceAll("[\\[\\]]", "")
                  .split(",").toSeq
                  .filterNot(_ === "")
                transactionService
                  .getTransactionsByCriteria(criteria.copy(accountNumbers = accountNumbers1), ordering,
                    limit = args.limit, offset = args.offset)
                  .map { serviceResponse ⇒
                    serviceResponse.fold(_ ⇒ List.empty, transactionsList ⇒ transactionsList)
                  }
            }
          }))
    } else {
      transactionService
        .getTransactionsByCriteria(criteria, ordering, limit = args.limit, offset = args.offset)
        .map { serviceResponse ⇒ serviceResponse.fold(_ ⇒ List.empty, transactionsList ⇒ transactionsList) }
    }
  }

  def sum(args: TransactionQueryArgs): Future[BigDecimal] = {
    transactionService.sumTransactionsByCriteria(buildCriteria(args)).map { serviceResponse ⇒
      serviceResponse.fold(
        error ⇒ {
          logger.error(s"Error in sum. $error")
          BigDecimal(0.0)
        },
        sum ⇒ sum)
    }
  }

  def count(args: TransactionQueryArgs): Future[Int] = {
    transactionService.countTransactionsByCriteria(buildCriteria(args)).map { serviceResponse ⇒
      serviceResponse.fold(
        error ⇒ {
          logger.error(s"Error in count. $error")
          0
        },
        count ⇒ count)
    }
  }

  def aggregate(args: TransactionQueryArgs, groupings: Seq[GroupingField]): Future[Seq[TransactionAggregatation]] = {
    transactionService.aggregateTransactionByCriteriaAndPivots(buildCriteria(args), groupings).map { serviceResponse ⇒
      serviceResponse.fold(
        error ⇒ {
          logger.error(s"Error in aggregation. $error")
          Seq()
        },
        aggregates ⇒ aggregates)
    }
  }
}

object TransactionRepo {
  def buildCriteria(args: TransactionQueryArgs): TransactionCriteria = {
    TransactionCriteria(
      customerId = args.customerId.map(_.toUUIDLike),
      accountId = args.accountId.map(_.toUUIDLike),
      startDate = args.from_date.map(a ⇒ LocalDateTime.parse(a + "T00:00:00")),
      endDate = args.to_date.map(a ⇒ LocalDateTime.parse(a + "T23:59:59")),
      transactionType = args.`type`.map(_.sanitize),
      channel = args.channel.map(_.sanitize),
      status = args.status.map(s ⇒ TransactionStatus(s.sanitize)),
      direction = args.direction,
      currencyCode = args.currency,
      accountType = args.accountType)
  }
}
