package tech.pegb.backoffice.domain.auth.dto

import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class BackOfficeUserCriteria(
    id: Option[UUIDLike] = None,
    userName: Option[String] = None,
    hashedPassword: Option[String] = None,
    roleId: Option[UUIDLike] = None,
    roleName: Option[String] = None,
    roleLevel: Option[Int] = None,
    businessUnitName: Option[String] = None,
    businessUnitId: Option[UUIDLike] = None,
    scopeId: Option[UUIDLike] = None,
    scopeName: Option[String] = None,
    email: Option[Email] = None,
    phoneNumber: Option[String] = None,
    firstName: Option[String] = None,
    middleName: Option[String] = None,
    lastName: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch {

}
