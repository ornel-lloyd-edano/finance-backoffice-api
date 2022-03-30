package tech.pegb.backoffice

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import tech.pegb.backoffice.dao.{PostgresDao, SqlDao}
import tech.pegb.backoffice.dao.aggregations.abstraction.GenericAggregationDao
import tech.pegb.backoffice.dao.aggregations.sql.{AggGreenplumDao, AggNdbDao}
import tech.pegb.backoffice.dao.postgresql.TransactionReportingSqlDao
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionDao
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionService
import tech.pegb.backoffice.domain.transaction.implementation.{TransactionMgmtService, TransactionsReportingService}

class Module extends AbstractModule {

  override def configure = {
    bind(classOf[TransactionDao]).to(classOf[TransactionSqlDao])
    bind(classOf[TransactionDao]).annotatedWith(Names.named("TransactionReportingSqlDao")).to(classOf[TransactionReportingSqlDao])

    bind(classOf[TransactionService]).to(classOf[TransactionMgmtService])
    bind(classOf[TransactionService]).annotatedWith(Names.named("TransactionsReportingService"))
      .to(classOf[TransactionsReportingService])

    //removing because of some intermittent behavior in unit tests
    bind(classOf[GenericAggregationDao]).annotatedWith(Names.named("MySQLAggregationDao"))
      .to(classOf[AggNdbDao])

    bind(classOf[GenericAggregationDao]).annotatedWith(Names.named("GreenPlumAggregationDao"))
      .to(classOf[AggGreenplumDao])

  }
}
