package tech.pegb.backoffice.domain.businessuserapplication.abstraction

object AddressTypes {
  lazy val toSeq = Seq(Primary, Secondary)
  val Primary = "primary_address"
  val Secondary = "secondary_address"
}
