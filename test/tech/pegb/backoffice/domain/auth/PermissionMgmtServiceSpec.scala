package tech.pegb.backoffice.domain.auth

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.auth.abstraction.PermissionDao
import tech.pegb.backoffice.domain.auth.abstraction.PermissionManagement
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class PermissionMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val permissionDao = stub[PermissionDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[PermissionDao].to(permissionDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val permissionMgmtService = inject[PermissionManagement]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  "PermissionMgmtService" should {
    "return created permission in createPermission" ignore {
      fail()
    }
    "return permission matching id in getPermissionById" ignore {
      fail()
    }
    "return permissions matching criteria in getPermissionByCriteria" ignore {
      fail()
    }
    "return count of permissions matching criteria in countPermissionByCriteria" ignore {
      fail()
    }
    "return updated permission matching id updatePermissionById" ignore {
      fail()
    }
    "return id of deleted permission in deletePermissionById" ignore {
      fail()
    }
  }

}

