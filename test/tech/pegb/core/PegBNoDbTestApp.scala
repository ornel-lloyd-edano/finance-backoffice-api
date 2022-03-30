package tech.pegb.core

import org.scalamock.scalatest.MockFactory
import org.scalatest.Suite
import play.api.db.evolutions.EvolutionsModule
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import play.api.inject.{Binding, bind}
import tech.pegb.backoffice.dao.report.abstraction.ReportDefinitionDao
import tech.pegb.backoffice.dao.types.abstraction.TypesDao

trait PegBNoDbTestApp extends PegBTestApp with MockFactory { this: Suite ⇒

  val typesDao = stub[TypesDao]
  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[TypesDao].to(typesDao),
      bind[ReportDefinitionDao].to[MockReportDefinitionDao])

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .disable[EvolutionsModule]
      .overrides(additionalBindings)
      .build()
  }

  override protected def initDb(): Unit = {
    ()
  }

}
