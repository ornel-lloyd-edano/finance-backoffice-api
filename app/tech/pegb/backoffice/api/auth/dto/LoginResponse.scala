package tech.pegb.backoffice.api.auth.dto

case class LoginResponse(token: String, user: BackOfficeUserToRead)
