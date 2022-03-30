package tech.pegb.backoffice.domain.auth.dto

import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class ScopeCriteria(
    id: Option[UUIDLike] = None,
    parentId: Option[UUIDLike] = None,
    name: Option[String] = None,
    description: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch
