package tech.pegb.backoffice.api.swagger.model

case class FeeProfilePaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.fee.dto.FeeProfileToRead])
