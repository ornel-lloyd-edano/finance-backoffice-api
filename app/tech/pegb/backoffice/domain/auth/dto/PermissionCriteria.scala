package tech.pegb.backoffice.domain.auth.dto

import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class PermissionCriteria(
    id: Option[UUIDLike] = None,
    businessId: Option[UUIDLike] = None,
    roleId: Option[UUIDLike] = None,
    userId: Option[UUIDLike] = None,
    scopeId: Option[UUIDLike] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch

