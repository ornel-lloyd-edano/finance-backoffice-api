package tech.pegb.backoffice.api.swagger.model

case class BusinessUnitPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[tech.pegb.backoffice.api.auth.dto.BusinessUnitToRead])
