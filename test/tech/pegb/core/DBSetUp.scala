package tech.pegb.core

import play.api.db.{DBApi, Database, Databases}
import play.api.db.evolutions.{Evolutions, EvolutionsReader}

class DBSetUp extends DBApi {
  val evolutionReader: EvolutionsReader = new TestEvolutionsReader

  val h2Url = "jdbc:h2:mem:backoffice;MODE=MYSQL;DATABASE_TO_UPPER=FALSE;IGNORECASE=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS backoffice"

  val database = Databases("org.h2.Driver", h2Url, "backoffice", Map("driver" -> "org.h2.Driver"))

  Evolutions.applyEvolutions(database, evolutionReader)

  override def databases(): Seq[Database] = Seq(database)

  override def shutdown(): Unit = database.shutdown()

  override def database(name: String): Database = database
}
