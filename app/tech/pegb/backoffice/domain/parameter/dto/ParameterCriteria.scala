package tech.pegb.backoffice.domain.parameter.dto

import java.util.UUID

import tech.pegb.backoffice.domain.parameter.model.Platform
import tech.pegb.backoffice.util.HasPartialMatch

case class ParameterCriteria(
    id: Option[UUID] = None,
    key: Option[String] = None,
    metadataId: Option[String] = None,
    platforms: Option[Seq[Platform]] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch
