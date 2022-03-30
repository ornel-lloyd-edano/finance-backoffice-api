package tech.pegb.backoffice.mapping.dao.domain.customer.dto

import java.util.UUID

import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.model.CustomerAggregation

import scala.util.Try

object Implicits {
  implicit class CustomerAggregationAdapter(arg: tech.pegb.backoffice.dao.customer.dto.CustomerAggregation) {
    def asDomain: Try[CustomerAggregation] = Try {
      CustomerAggregation(
        uniqueId = UUID.randomUUID().toString,
        userName = arg.userName,
        tier = arg.tier.map(CustomerTier),
        segment = arg.segment.map(CustomerSegment),
        subscription = arg.subscription.map(CustomerSubscription),
        status = arg.status.map(CustomerStatus(_)),
        msisdn = arg.msisdn.map(Msisdn(_)),
        name = arg.name,
        fullName = arg.fullName,
        gender = arg.gender,
        personId = arg.personId,
        documentNumber = arg.documentNumber,
        documentType = arg.documentType,
        birthDate = arg.birthDate,
        birthPlace = arg.birthPlace,
        nationality = arg.nationality,
        occupation = arg.occupation,
        companyName = arg.companyName,
        employer = arg.employer,
        createdAt = arg.createdAt,
        activatedAt = arg.activatedAt,
        isActivated = arg.isActivated,
        isActive = arg.isActive,
        applicationStatus = arg.applicationStatus,
        sum = arg.sum,
        count = arg.count,
        date = arg.date,
        day = arg.day,
        month = arg.month,
        year = arg.year,
        hour = arg.hour,
        minute = arg.minute)
    }
  }
}
