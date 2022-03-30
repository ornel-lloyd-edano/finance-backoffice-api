package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

case class GenericUser(
    id: Int,
    uuid: String,
    userName: String,
    password: Option[String],
    customerType: Option[String],
    tier: Option[String],
    segment: Option[String],
    subscription: Option[String],
    email: Option[String],
    status: Option[String],
    activatedAt: Option[LocalDateTime],
    passwordUpdatedAt: Option[LocalDateTime],
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],

    customerName: Option[String],
    businessUserFields: Option[BusinessUser],
    individualUserFields: Option[IndividualUser])
