package tech.pegb.backoffice.domain.makerchecker.dto

import java.time.{LocalDateTime}

import tech.pegb.backoffice.domain.makerchecker.model.Status
import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class MakerCheckerCriteria(
    id: Option[UUIDLike] = None,
    module: Option[String] = None,
    status: Option[Status] = None,
    createdAtFrom: Option[LocalDateTime] = None,
    createdAtTo: Option[LocalDateTime] = None,
    isAllowedToCheck: Option[Boolean] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch {

}
