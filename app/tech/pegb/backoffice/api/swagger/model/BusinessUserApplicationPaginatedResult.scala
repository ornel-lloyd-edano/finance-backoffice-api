package tech.pegb.backoffice.api.swagger.model

case class BusinessUserApplicationPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.businessuserapplication.dto.BusinessUserApplicationToRead])
