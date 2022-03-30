package tech.pegb.backoffice.domain.businessuserapplication.abstraction

object VelocityUserLevels {
  lazy val toSeq = Seq(Admin, Operator, Viewer)
  val Admin = "admin"
  val Operator = "operator"
  val Viewer = "viewer"
}
