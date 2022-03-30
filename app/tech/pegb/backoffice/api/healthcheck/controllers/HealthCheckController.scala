package tech.pegb.backoffice.api.healthcheck.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import tech.pegb.backoffice.BuildInfo

@Singleton
class HealthCheckController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  private def buildInfoString = BuildInfo.toString

  def isHealthy = Action { _ ⇒
    val message = s"server time: ${java.time.ZonedDateTime.now},  build: $buildInfoString"
    Ok(message)
  }
}
