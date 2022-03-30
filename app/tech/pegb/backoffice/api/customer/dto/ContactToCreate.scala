package tech.pegb.backoffice.api.customer.dto

case class ContactToCreate(
    contactType: String,
    name: String,
    middleName: Option[String],
    surname: String,
    phoneNumber: String,
    email: String,
    idType: String)
