package tech.pegb.backoffice.api.swagger.model

case class ApplicationPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[tech.pegb.backoffice.api.application.dto.WalletApplicationToRead])
