package tech.pegb.backoffice.api.customer.dto

import java.time.ZonedDateTime
import com.fasterxml.jackson.annotation.JsonProperty

case class ContactToUpdate(
    contactType: Option[String] = None,
    name: Option[String] = None,
    middleName: Option[String] = None,
    surname: Option[String] = None,
    phoneNumber: Option[String] = None,
    email: Option[String] = None,
    idType: Option[String] = None,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
