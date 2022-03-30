package tech.pegb.backoffice.domain.currency.model

case class Currency(
    id: Int,
    code: String,
    description: Option[String])
