package tech.pegb.backoffice.domain.aggregations.implementation

import java.time.{LocalDate, LocalDateTime}

import tech.pegb.backoffice.domain.aggregations.abstraction.BalanceCalculator
import tech.pegb.backoffice.domain.aggregations.dto.{TransactionAggregationResult, TransactionGrouping}
import tech.pegb.backoffice.util.Logging
import tech.pegb.backoffice.util.time._

import cats.implicits._
import scala.util.Try

class BalanceCalculatorService extends BalanceCalculator with Logging {

  def computeBalancePerTimePeriod(
    rawAggregationResult: Seq[TransactionAggregationResult],
    transactionGrouping: Option[TransactionGrouping],
    dateFrom: Option[LocalDateTime],
    dateTo: Option[LocalDateTime]): Seq[TransactionAggregationResult] = {

    val zeroTransactionAggregationResult = TransactionAggregationResult(sumAmount = BigDecimal(0).some)

    (transactionGrouping) match {
      case Some(txnGroup) if txnGroup.daily ⇒
        logger.debug("[computeBalancePerTimePeriod] group by daily found, computing")
        //sort per day transaction aggregation by date
        val dateSorted = rawAggregationResult.collect { case p if p.date.isDefined ⇒ p.date.get → p }.sortBy(_._1)(math.Ordering.by(_.toEpochDay)).map(_._2)
        logger.debug(s"[computeBalancePerTimePeriod] rawAggregationResul sorted => ${dateSorted}")

        //add previous day balance to today's transaction
        val endOfDaySeq = computeEndOfDayBalance(dateSorted)
        logger.debug(s"[computeBalancePerTimePeriod] computed endOfDaySeq from day 0 => ${endOfDaySeq}")

        //create a range of dates (from, to)
        val range = DateTimeRangeUtil.createDateRange(dateFrom.map(_.toLocalDate), dateTo.map(_.toLocalDate), Daily)

        //take last day with transaction before range startdate
        //if no day before range startdate with transaction, use zero
        val lastDayWithTxnBeforeRangeStart = range.headOption match {
          case None ⇒ zeroTransactionAggregationResult
          case Some(rangeHead) ⇒ endOfDaySeq.takeWhile {
            _.date.flatMap(date ⇒ Try(LocalDate.parse(rangeHead)).map(hDate ⇒ date.isBefore(hDate)).toOption).contains(true)
          }.reverse.headOption.getOrElse(zeroTransactionAggregationResult)
        }

        //function for constructing days without transaction
        val createTransactionAggregationResultFromPrevious = (today: String, yesterday: TransactionAggregationResult) ⇒ {
          yesterday.copy(date = Try(LocalDate.parse(today)).toOption)
        }

        //create lookupMap for end of day balance
        val endOfDayMap = endOfDaySeq.collect { case p if p.date.isDefined ⇒ p.date.get.toString → p }.toMap
        //get all before dateFrom
        filterEndOfDayBalanceByDateRange(endOfDayMap, range, lastDayWithTxnBeforeRangeStart, createTransactionAggregationResultFromPrevious)

      case Some(txnGroup) if txnGroup.weekly ⇒
        logger.debug("[computeBalancePerTimePeriod] group by weekly found, computing")
        //sort per week transaction aggregation by date
        val dateSorted = rawAggregationResult.collect { case p if p.week.isDefined ⇒ p.week.get → p }.sortBy(d ⇒ (d._1.year, d._1.weekNumber)).map(_._2)
        logger.debug(s"[computeBalancePerTimePeriod] rawAggregationResul sorted => ${dateSorted}")

        //add previous day balance to today's transaction
        val endOfDaySeq = computeEndOfDayBalance(dateSorted)
        logger.debug(s"[computeBalancePerTimePeriod] computed endOfDaySeq from day 0 => ${endOfDaySeq}")

        //create a range of dates (from, to)
        val range = DateTimeRangeUtil.createDateRange(dateFrom.map(_.toLocalDate), dateTo.map(_.toLocalDate), Weekly)

        //take last day with transaction before range startdate
        //if no day before range startdate with transaction, use zero
        val lastDayWithTxnBeforeRangeStart = range.headOption match {
          case None ⇒ zeroTransactionAggregationResult
          case Some(rangeHead) ⇒ endOfDaySeq.takeWhile {
            _.week.flatMap(week ⇒ Try(Week.parse(rangeHead)).map(hDate ⇒ week.isBefore(hDate)).toOption).contains(true)
          }.reverse.headOption.getOrElse(zeroTransactionAggregationResult)
        }

        //function for constructing days without transaction
        val createTransactionAggregationResultFromPrevious = (thisWeek: String, lastWeek: TransactionAggregationResult) ⇒ {
          lastWeek.copy(week = Try(Week.parse(thisWeek)).toOption)
        }

        //create lookupMap for end of day balance
        val endOfDayMap = endOfDaySeq.collect { case p if p.week.isDefined ⇒ p.week.get.toString → p }.toMap
        //get all before dateFrom
        filterEndOfDayBalanceByDateRange(endOfDayMap, range, lastDayWithTxnBeforeRangeStart, createTransactionAggregationResultFromPrevious)

      case Some(txnGroup) if txnGroup.monthly ⇒
        logger.debug("[computeBalancePerTimePeriod] group by monthly found, computing")
        //sort per week transaction aggregation by date
        val dateSorted = rawAggregationResult.collect { case p if p.month.isDefined ⇒ p.month.get → p }.sortBy(d ⇒ (d._1.year, d._1.monthNumber)).map(_._2)
        logger.debug(s"[computeBalancePerTimePeriod] rawAggregationResul sorted => ${dateSorted}")

        //add previous day balance to today's transaction
        val endOfDaySeq = computeEndOfDayBalance(dateSorted)
        logger.debug(s"[computeBalancePerTimePeriod] computed endOfDaySeq from day 0 => ${endOfDaySeq}")

        //create a range of dates (from, to)
        val range = DateTimeRangeUtil.createDateRange(dateFrom.map(_.toLocalDate), dateTo.map(_.toLocalDate), Monthly)

        //take last day with transaction before range startdate
        //if no day before range startdate with transaction, use zero
        val lastDayWithTxnBeforeRangeStart = range.headOption match {
          case None ⇒ zeroTransactionAggregationResult
          case Some(rangeHead) ⇒ endOfDaySeq.takeWhile {
            _.month.flatMap(month ⇒ Try(Month.parse(rangeHead)).map(hDate ⇒ month.isBefore(hDate)).toOption).contains(true)
          }.reverse.headOption.getOrElse(zeroTransactionAggregationResult)
        }

        //function for constructing days without transaction
        val createTransactionAggregationResultFromPrevious = (thisMonth: String, lastMonth: TransactionAggregationResult) ⇒ {
          lastMonth.copy(month = Try(Month.parse(thisMonth)).toOption)
        }

        //create lookupMap for end of day balance
        val endOfDayMap = endOfDaySeq.collect { case p if p.month.isDefined ⇒ p.month.get.toString → p }.toMap
        //get all before dateFrom
        filterEndOfDayBalanceByDateRange(endOfDayMap, range, lastDayWithTxnBeforeRangeStart, createTransactionAggregationResultFromPrevious)

      case _ ⇒
        logger.debug("[computeBalancePerTimePeriod] no group by time period. Returning daoAggregationResult")
        rawAggregationResult

    }
  }

  private def computeEndOfDayBalance(dateSorted: Seq[TransactionAggregationResult]): Seq[TransactionAggregationResult] = {
    dateSorted.headOption match {
      case Some(transactionAggregationDay1) ⇒
        val tailCalculation = dateSorted.tail.foldLeft[List[TransactionAggregationResult]](List(transactionAggregationDay1)) { (acc, head) ⇒
          val endOfDayBalance = acc.head.sumAmount |+| head.sumAmount
          head.copy(sumAmount = endOfDayBalance) :: acc
        }.reverse

        transactionAggregationDay1 :: tailCalculation

      case None ⇒
        logger.info("[computeEndOfDayBalance] transactionAggregationResult is empty, returning empty end of day balance")
        Nil
    }
  }

  private def filterEndOfDayBalanceByDateRange(
    endOfDayBalanceMap: Map[String, TransactionAggregationResult],
    dateRange: Seq[String],
    lastDayWithTxnBeforeRangeStart: TransactionAggregationResult,
    createTransactionAggregationResultFromPrevious: (String, TransactionAggregationResult) ⇒ TransactionAggregationResult): Seq[TransactionAggregationResult] = {

    logger.info(s"[filterEndOfDayBalanceByDateRange] first date of range = ${dateRange.head}")
    logger.info(s"[filterEndOfDayBalanceByDateRange] last date with transaction before first day of range = ${lastDayWithTxnBeforeRangeStart}")

    //constructFinalList
    def constructFinalList(range: Seq[String], lastDateWithValue: TransactionAggregationResult, acc: List[TransactionAggregationResult]): Seq[TransactionAggregationResult] = {
      range match {
        case head :: tail ⇒
          endOfDayBalanceMap.get(head) match {
            case Some(txnAggResult) ⇒
              constructFinalList(tail, txnAggResult, txnAggResult :: acc)
            case None ⇒
              //No transactionAggResult for the day, use previous date with value
              val todayEndOfBalance = createTransactionAggregationResultFromPrevious(head, lastDateWithValue)
              constructFinalList(tail, todayEndOfBalance, todayEndOfBalance :: acc)
          }
        case Nil ⇒
          acc.reverse
      }
    }

    constructFinalList(dateRange, lastDayWithTxnBeforeRangeStart, Nil)

  }

}
