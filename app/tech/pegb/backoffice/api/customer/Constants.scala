package tech.pegb.backoffice.api.customer

import tech.pegb.backoffice.util.Constants._

object Constants {

  val validIndividualUsersPartialMatchFields = Set(disabled, userId, msisdn, fullName)
  val validAccountsPartialMatchFields = Set(disabled, customerId, accountNumber, customerFullName, anyCustomerName, msisdn)
  val validDocumentMgmntPartialMatchFields = Set(disabled, customerId, customerFullName, msisdn)

}
