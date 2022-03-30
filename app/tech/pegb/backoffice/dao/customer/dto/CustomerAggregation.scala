package tech.pegb.backoffice.dao.customer.dto

import tech.pegb.backoffice.dao.model.Aggregation
import java.time.{LocalDate, LocalDateTime}

case class CustomerAggregation(
    userName: Option[String] = None,
    tier: Option[String] = None,
    segment: Option[String] = None,
    subscription: Option[String] = None,
    email: Option[String] = None,
    status: Option[String] = None,
    msisdn: Option[String] = None,
    `type`: Option[String] = Option("individual_user"),
    name: Option[String] = None,
    fullName: Option[String] = None,
    gender: Option[String] = None,
    personId: Option[String] = None,
    documentNumber: Option[String] = None,
    documentType: Option[String] = None,
    birthDate: Option[LocalDate] = None,
    birthPlace: Option[String] = None,
    nationality: Option[String] = None,
    occupation: Option[String] = None,
    companyName: Option[String] = None,
    employer: Option[String] = None,
    createdAt: Option[LocalDateTime] = None,
    activatedAt: Option[LocalDateTime] = None,
    isActivated: Option[Boolean] = Option(false),
    isActive: Option[Boolean] = Option(false),
    applicationStatus: Option[String] = None,
    date: Option[LocalDate] = None,
    day: Option[Int] = None,
    month: Option[Int] = None,
    year: Option[Int] = None,
    hour: Option[Int] = None,
    minute: Option[Int] = None,
    sum: Option[BigDecimal] = Option(BigDecimal(0.0)),
    count: Option[Long] = Option(0L)) extends Aggregation
