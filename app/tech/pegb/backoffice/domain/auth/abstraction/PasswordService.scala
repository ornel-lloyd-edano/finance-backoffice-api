package tech.pegb.backoffice.domain.auth.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.auth.implementation.PasswordServiceImpl

@ImplementedBy(classOf[PasswordServiceImpl])
trait PasswordService extends BaseService {

  def nDigits: Int
  def nLowercase: Int
  def nUppercase: Int
  def nSpecial: Int
  def minPasswordLength: Int
  def duplicateCharAllowed: Boolean
  protected val defaultPassword: String

  def hashPassword(password: String): String

  def generatePassword: String

  def validatePassword(oldPassword: Option[String], password: String): ServiceResponse[String]

}
