package tech.pegb.backoffice.api.swagger.model

case class ContactAddressToReadPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.customer.dto.ContactToRead])
