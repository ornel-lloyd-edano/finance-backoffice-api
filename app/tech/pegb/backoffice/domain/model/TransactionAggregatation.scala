package tech.pegb.backoffice.domain.model

import java.time.{LocalDate, LocalDateTime}
import java.util.Currency

import tech.pegb.backoffice.domain.transaction.model.TransactionStatus

case class TransactionAggregatation(
    direction: Option[String] = None,
    `type`: Option[String] = None,
    amount: Option[BigDecimal] = None,
    currency: Option[Currency] = None,
    exchangedCurrency: Option[Currency] = None,
    channel: Option[String] = None,
    effectiveRate: Option[BigDecimal] = None,
    costRate: Option[BigDecimal] = None,
    status: Option[TransactionStatus] = None,
    instrument: Option[String] = None,
    createdAt: Option[LocalDateTime] = None,
    override val uniqueId: String,
    override val date: Option[LocalDate] = None,
    override val day: Option[Int] = None,
    override val month: Option[Int] = None,
    override val year: Option[Int] = None,
    override val hour: Option[Int] = None,
    override val minute: Option[Int] = None,
    sum: Option[BigDecimal],
    count: Option[Long]) extends Aggregatation
