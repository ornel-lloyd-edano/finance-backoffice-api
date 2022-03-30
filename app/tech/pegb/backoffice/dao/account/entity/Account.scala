package tech.pegb.backoffice.dao.account.entity

import java.time.LocalDateTime

import julienrf.json.derived
import tech.pegb.backoffice.util.Constants

case class Account(
    id: Int,
    uuid: String,
    accountNumber: String,
    userId: Int,
    userUuid: String,
    userType: String,
    anyCustomerName: Option[String],
    brandName: Option[String],
    userName: Option[String],
    individualUserName: Option[String],
    individualUserFullName: Option[String],
    msisdn: Option[String],
    accountName: String,
    accountType: String,
    isMainAccount: Option[Boolean],
    currency: String,
    balance: Option[BigDecimal],
    blockedBalance: Option[BigDecimal],
    status: Option[String],
    closedAt: Option[LocalDateTime],
    lastTransactionAt: Option[LocalDateTime],
    mainType: String,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

object Account {
  import play.api.libs.json._

  def getEmpty = Account(id = 0, uuid = "", accountNumber = "", userId = 0, userUuid = "", userType = "",
    anyCustomerName = None, brandName = None, userName = None, individualUserName = None, individualUserFullName = None,
    msisdn = None, accountName = "", accountType = "", isMainAccount = Some(true), currency = "",
    balance = Some(BigDecimal(0)), blockedBalance = Some(BigDecimal(0)), status = Some(""), closedAt = None,
    lastTransactionAt = None, mainType = Constants.Liability, createdAt = LocalDateTime.now, createdBy = "",
    updatedAt = None, updatedBy = None)

  implicit val format: Format[Account] = derived.oformat()
}
