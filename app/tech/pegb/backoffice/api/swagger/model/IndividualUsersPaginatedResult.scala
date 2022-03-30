package tech.pegb.backoffice.api.swagger.model

case class IndividualUsersPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[tech.pegb.backoffice.api.customer.dto.IndividualUserResponse])
