package tech.pegb.backoffice.domain.auth.dto

import java.util.UUID

case class BusinessUnitCriteria(id: Option[UUID] = None, name: Option[String] = None)
