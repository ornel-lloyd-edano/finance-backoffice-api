package tech.pegb.backoffice.api.swagger.model

case class CommissionProfilePaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.commission.dto.CommissionProfileToRead])
