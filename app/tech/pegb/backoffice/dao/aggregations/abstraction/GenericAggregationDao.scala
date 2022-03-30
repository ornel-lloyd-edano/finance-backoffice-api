package tech.pegb.backoffice.dao.aggregations.abstraction

import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.aggregations.dto.{AggregationInput, AggregationResult, Entity, GroupByInput}
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}

trait GenericAggregationDao {

  /*
  NOTE:
  ex. aggregate(
    entity = Entity(TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias)),
    expressionsToAggregate = Seq( AggregationInput(TransactionSqlDao.cAmount, AggFunctions.Sum),
      AggregationInput(s"${TransactionSqlDao.cAmount} - ${TransactionSqlDao.cFee}", AggFunctions.Sum, "revenue")),
    criteria = Seq(CriteriaField(TransactionSqlDao.cCurrency, "KES"),
      CriteriaField(TransactionSqlDao.cPrimaryAccount, "1234")),
    groupBy = Seq(GroupByInput(TransactionSqlDao.cInstitution),
      GroupByInput(TransactionSqlDao.cCreatedAt, Some(ScalarFunctions.Month), Some("monthly") )),
    orderBy = Some(OrderingSet( "monthly", Order.DESC )),
    limit = Some(4), offset = Some(1)

  ex. join with accounts and users and currencies
  aggregate(
    entity = Seq(
      Entity(TransactionSqlDao.TableName, Some(TransactionSqlDao.TableAlias),
         Seq( JoinColumn(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}", s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}"),
              JoinColumn(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}", s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}") ))
      Entity(AccountSqlDao.TableName, Some(AccountSqlDao.TableAlias),
        Seq( JoinColumn(s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cUserId}", s"${UserSqlDao.TableAlias}.${UserSqlDao.cId}") )),
      Entity(UserSqlDao.TableName, Some(UserSqlDao.TableAlias), Seq())
    ),
    expressionsToAggregate = Seq( AggregationInput(TransactionSqlDao.cAmount, AggFunctions.Sum),
      AggregationInput(s"${TransactionSqlDao.cAmount} - ${TransactionSqlDao.cFee}", AggFunctions.Sum, "revenue")),
    criteria = Seq(CriteriaField(TransactionSqlDao.cCurrency, "KES"),
      CriteriaField(TransactionSqlDao.cPrimaryAccount, "1234")),
    groupBy = Seq(GroupByInput(TransactionSqlDao.cInstitution),
      GroupByInput(TransactionSqlDao.cCreatedAt, Some(ScalarFunctions.Month), Some("monthly") )),
    orderBy = Some(OrderingSet( "monthly", Order.DESC )),
    limit = Some(4), offset = Some(1)
 */

  def aggregate(
    entity: Seq[Entity],
    expressionsToAggregate: Seq[AggregationInput],
    criteria: Seq[CriteriaField[_]],
    groupBy: Seq[GroupByInput],
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[AggregationResult]]

}
