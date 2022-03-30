package tech.pegb.backoffice.api.auth.dto

case class CredentialsToRead(
    user: String,
    password: String,
    captcha: Option[String])
