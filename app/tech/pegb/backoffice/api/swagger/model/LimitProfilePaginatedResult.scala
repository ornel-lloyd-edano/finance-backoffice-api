package tech.pegb.backoffice.api.swagger.model

case class LimitProfilePaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.limit.dto.LimitProfileToRead])
