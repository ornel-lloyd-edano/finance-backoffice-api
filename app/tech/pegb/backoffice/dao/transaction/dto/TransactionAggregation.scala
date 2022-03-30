package tech.pegb.backoffice.dao.transaction.dto

import java.time.{LocalDate, LocalDateTime}

import tech.pegb.backoffice.dao.model.Aggregation

case class TransactionAggregation(
    direction: Option[String] = None,
    `type`: Option[String] = None,
    amount: Option[BigDecimal] = None,
    currency: Option[String] = None,
    exchangedCurrency: Option[String] = None,
    channel: Option[String] = None,
    effectiveRate: Option[BigDecimal] = None,
    costRate: Option[BigDecimal] = None,
    status: Option[String] = None,
    instrument: Option[String] = None,
    createdAt: Option[LocalDateTime] = None,
    override val date: Option[LocalDate] = None,
    override val day: Option[Int] = None,
    override val month: Option[Int] = None,
    override val year: Option[Int] = None,
    override val hour: Option[Int] = None,
    override val minute: Option[Int] = None,
    override val sum: Option[BigDecimal] = Option(BigDecimal(0.0)),
    override val count: Option[Long] = Option(0L)) extends Aggregation
