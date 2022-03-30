package tech.pegb.backoffice.domain.model

import java.time.{LocalDate, LocalDateTime}

import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._

case class CustomerAggregation(
    userName: Option[String] = None,
    tier: Option[CustomerTier] = None,
    segment: Option[CustomerSegment] = None,
    subscription: Option[CustomerSubscription] = None,
    status: Option[CustomerStatus] = None,
    msisdn: Option[Msisdn] = None,
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
    override val uniqueId: String,
    override val date: Option[LocalDate] = None,
    override val day: Option[Int] = None,
    override val month: Option[Int] = None,
    override val year: Option[Int] = None,
    override val hour: Option[Int] = None,
    override val minute: Option[Int] = None,
    sum: Option[BigDecimal],
    count: Option[Long]) extends Aggregatation
