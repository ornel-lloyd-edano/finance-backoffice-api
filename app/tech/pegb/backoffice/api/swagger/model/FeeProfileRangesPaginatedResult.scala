package tech.pegb.backoffice.api.swagger.model

case class FeeProfileRangesPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.fee.dto.FeeProfileRangeToRead])

