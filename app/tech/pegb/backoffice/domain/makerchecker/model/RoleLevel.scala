package tech.pegb.backoffice.domain.makerchecker.model

trait RoleLevel {
  val underlying: Int

  def isHighestLevel: Boolean

  def isDepartmentApproverLevel: Boolean

  def isHigherThan(thisRoleLevel: RoleLevel): Boolean = {
    underlying < thisRoleLevel.underlying
  }

  def isSameOrHigherThan(thisRoleLevel: RoleLevel): Boolean = {
    underlying <= thisRoleLevel.underlying
  }
}

object RoleLevels {

  case class OtherLevel(underlying: Int) extends RoleLevel {
    assert(underlying >= 0, "role level cannot be lower than zero")
    def isHighestLevel = underlying == 0
    def isDepartmentApproverLevel = underlying == 1
  }

  def apply(arg: Int) = arg match {
    case 0 ⇒ CEO
    case 1 ⇒ DepartmentHead
    case other ⇒ OtherLevel(other)
  }

  case object CEO extends RoleLevel {
    val underlying = 0
    def isHighestLevel = true
    def isDepartmentApproverLevel = false
  }

  case object DepartmentHead extends RoleLevel {
    val underlying = 1
    def isHighestLevel = false
    def isDepartmentApproverLevel = true
  }

  implicit class RoleLevelIntToDomainAdapter(val arg: Int) extends AnyVal {
    def asDomain = RoleLevels(arg)
  }
}
