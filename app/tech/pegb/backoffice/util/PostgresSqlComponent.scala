package tech.pegb.backoffice.util

import com.zaxxer.hikari.HikariDataSource
import scala.concurrent.duration._

trait PostgresqlComponent {
  //override val driver: JdbcProfile = PostgresProfile
  final val schema = "dwh"
  //val dbConfig: Config = configuration.getConfig("db.dwhv2")
  val ds = new HikariDataSource
  ds.setConnectionTestQuery("SELECT 1")
  ds.setConnectionInitSql("SELECT 1")
  ds.setDriverClassName("org.postgresql.Driver")
  ds.setUsername("gpadmin")
  ds.setPassword("gpadmin")
  ds.setAutoCommit(true)
  ds.setJdbcUrl("jdbc:postgresql://172.30.1.50:5432/dwh")
  ds.setMaxLifetime(1.hour.toMillis)
  ds.setMinimumIdle(10)
  ds.setMaximumPoolSize(20)

  //val db = Database.forDataSource(ds, Some(ds.getMaximumPoolSize))
  val db = ds.getConnection

}

object PostgresqlComponent extends PostgresqlComponent
