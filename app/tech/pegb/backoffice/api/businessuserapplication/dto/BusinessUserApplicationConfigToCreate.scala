package tech.pegb.backoffice.api.businessuserapplication.dto

case class BusinessUserApplicationConfigToCreate(
    transactionConfig: Seq[TransactionConfig],
    accountConfig: Seq[AccountConfig],
    externalAccounts: Seq[ExternalAccount]) extends BusinessUserApplicationConfigToCreateT

case class TransactionConfig(
    transactionType: String,
    currencyCode: String) extends TransactionConfigT

case class AccountConfig(
    accountType: String,
    accountName: String,
    currencyCode: String,
    isDefault: Boolean) extends AccountConfigT

case class ExternalAccount(
    provider: String,
    accountNumber: String,
    accountHolder: String,
    currencyCode: String) extends ExternalAccountT

trait BusinessUserApplicationConfigToCreateT {
  def transactionConfig: Seq[TransactionConfigT]
  def accountConfig: Seq[AccountConfigT]
  def externalAccounts: Seq[ExternalAccountT]
}

trait TransactionConfigT {
  def transactionType: String
  def currencyCode: String
}

trait AccountConfigT {
  def accountType: String
  def accountName: String
  def currencyCode: String
  def isDefault: Boolean
}

trait ExternalAccountT {
  def provider: String
  def accountNumber: String
  def accountHolder: String
  def currencyCode: String
}
