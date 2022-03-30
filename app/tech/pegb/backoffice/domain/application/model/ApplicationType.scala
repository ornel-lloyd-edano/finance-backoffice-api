package tech.pegb.backoffice.domain.application.model

sealed trait ApplicationType {
  def isWallet: Boolean = false
  def isBusiness: Boolean = false
}

object ApplicationTypes {
  case object WalletApplication extends ApplicationType {
    override def toString: String = "wallet_application"
    override def isWallet = true
  }

  case object BusinessUserApplication extends ApplicationType {
    override def toString: String = "business_user_application"
    override def isBusiness: Boolean = true
  }
}
