package tech.pegb.backoffice.dao.aggregations

import org.scalamock.scalatest.MockFactory
import play.api.db.DBApi
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.aggregations.dto.{AggregatedValue, AggregationInput, AggregationResult, Entity, GroupByInput, Grouping, JoinColumn}
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}
import tech.pegb.core.PegBTestApp
import tech.pegb.backoffice.dao.aggregations.abstraction.AggFunctions.{Avg, Count, Sum}
import tech.pegb.backoffice.dao.aggregations.abstraction.ScalarFunctions.GetDate
import tech.pegb.backoffice.dao.aggregations.sql.AggGreenplumDao
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.model.MatchTypes.GreaterOrEqual
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao

class AggGreenplumDaoSpec extends PegBTestApp with MockFactory {
  import tech.pegb.backoffice.dao.Dao._

  lazy val aggGreenplumDao = fakeApplication().injector.instanceOf[AggGreenplumDao]

  override def initDb(): Unit = {
    val dbApi = inject[DBApi]
    val db = dbApi.database("reports")
    val connection = db.getConnection()
    connection.prepareStatement(initSql).executeUpdate()
    connection.commit()
  }

  def insertData =
    """INSERT INTO transactions
      |(unique_id,id,sequence ,primary_account_id ,primary_account_uuid ,primary_account_number,secondary_account_id,secondary_account_uuid ,secondary_account_number
      |,receiver_phone ,direction,type,amount,currency,exchange_rate,channel ,other_party ,instrument,instrument_id ,latitude ,longitude ,explanation ,status,created_at
      |,updated_at     ,cost_rate    ,previous_balance     ,gp_insert_timestamp     ,primary_account_type     ,primary_account_main_type     ,primary_account_user_id     ,primary_account_user_uuid
      |,effective_rate     ,dashboard_revenue
      |)
      |values(
      |2992,'611db3e8-23dd-4f76-b9ca-7ed8414a30fd',1,616,'3406eaa0-6e80-42e9-9dc0-89eb53419a62','415.1',642,'ad60caa6-4d08-42bf-88cf-efc47739156b','415.3','','debit','currency_exchange',0.2000,'3'
      |,null,'INTERNAL','','','',null,null,'Transaction generated','success','2019-10-29 09:08:03'
      |,'2019-10-20 12:13:44',1.0000,47338.4400,'2019-10-24 13:59:30.023333','2','liability',415,'402aa843-8f72-4293-a85d-8a417d0d2790',1.000000,1.50);
      |INSERT INTO transactions
      |(unique_id,id,sequence ,primary_account_id ,primary_account_uuid ,primary_account_number,secondary_account_id,secondary_account_uuid ,secondary_account_number
      |,receiver_phone ,direction,type,amount,currency,exchange_rate,channel ,other_party ,instrument,instrument_id ,latitude ,longitude ,explanation ,status,created_at
      |,updated_at     ,cost_rate    ,previous_balance     ,gp_insert_timestamp     ,primary_account_type     ,primary_account_main_type     ,primary_account_user_id     ,primary_account_user_uuid
      |,effective_rate     ,dashboard_revenue
      |)
      |values(
      |2958,'611db3e8-23dd-4f76-b9ca-7ed8414a30fd',1,616,'3406eaa0-6e80-42e9-9dc0-89eb53419a62','415.1',642,'ad60caa6-4d08-42bf-88cf-efc47739156b','415.3','','debit','fees',0.2000,'3'
      |,null,'INTERNAL','','','',null,null,'Transaction generated','success','2019-10-29 09:08:03'
      |,'2019-10-20 12:13:44',1.0000,47338.4400,'2019-10-24 13:59:30.023333','2','liability',415,'402aa843-8f72-4293-a85d-8a417d0d2790',1.000000,2.50);
      |
      |INSERT INTO accounts (
      |id ,uuid ,number ,name ,account_type ,is_main_account,user_id ,user_uuid ,currency ,balance ,blocked_balance ,status
      |,closed_at ,last_transaction_at,created_at,updated_at,updated_by,created_by,main_type,gp_insert_timestamp
      |) values
      |(616,'3406eaa0-6e80-42e9-9dc0-89eb53419a62','pegb_fees.3','1_fee_collection_AED'
      |,'7','f',1,'3802a2c5-fbdf-11e9-856a-fa163eafb21a','3',0.0000,0.0000,'active',null,null,'2019-10-31 13:06:14','2019-10-31 13:06:14','1','wallet_api','liability','2019-12-11 12:27:13.114391');
      |INSERT INTO accounts (
      |id ,uuid ,number ,name ,account_type ,is_main_account,user_id ,user_uuid ,currency ,balance ,blocked_balance ,status
      |,closed_at ,last_transaction_at,created_at,updated_at,updated_by,created_by,main_type,gp_insert_timestamp
      |) values
      |(642,'ad60caa6-4d08-42bf-88cf-efc47739156b','pegb_fees.3','1_fee_collection_AED'
      |,'7','f',1,'3802a2c5-fbdf-11e9-856a-fa163eafb21a','3',0.0000,0.0000,'active',null,null,'2019-10-31 13:06:14','2019-10-31 13:06:14','1','wallet_api','liability','2019-12-11 12:27:13.114391');
      |
      |INSERT INTO currencies (
      |id ,currency_name )
      | values
      |('3','KES');
      |""".stripMargin

  override def initSql =
    s"""
         |SET SCHEMA $reportsSchema;
         |DROP TABLE IF EXISTS transactions;
         |CREATE TABLE transactions
         |(
         |  unique_id bigint NOT NULL,
         |  id character varying(40) NOT NULL,
         |  sequence integer NOT NULL,
         |  primary_account_id integer NOT NULL,
         |  primary_account_uuid character varying DEFAULT 'missing'::character varying,
         |  primary_account_number character varying DEFAULT 'missing'::character varying,
         |  secondary_account_id integer,
         |  secondary_account_uuid character varying DEFAULT 'missing'::character varying,
         |  secondary_account_number character varying DEFAULT 'missing'::character varying,
         |  receiver_phone character varying,
         |  direction character varying DEFAULT 'missing'::character varying,
         |  type character varying DEFAULT 'missing'::character varying,
         |  amount numeric(20,4) DEFAULT NULL::numeric,
         |  currency character varying DEFAULT 'missing'::character varying,
         |  exchange_rate numeric(20,4) DEFAULT NULL::numeric,
         |  channel character varying DEFAULT 'missing'::character varying,
         |  other_party character varying,
         |  instrument character varying,
         |  instrument_id character varying,
         |  latitude numeric,
         |  longitude numeric,
         |  explanation character varying,
         |  status character varying DEFAULT 'missing'::character varying,
         |  created_at timestamp without time zone,
         |  updated_at timestamp without time zone,
         |  cost_rate numeric(20,4) DEFAULT NULL::numeric,
         |  previous_balance numeric(20,4) DEFAULT NULL::numeric,
         |  gp_insert_timestamp timestamp without time zone DEFAULT now(),
         |  primary_account_type character varying NOT NULL,
         |  primary_account_main_type character varying NOT NULL,
         |  primary_account_user_id integer NOT NULL,
         |  primary_account_user_uuid character varying NOT NULL,
         |  effective_rate numeric(20,6),
         |  dashboard_revenue numeric(20,4)
         |);
         |DROP TABLE IF EXISTS accounts;
         |CREATE TABLE accounts
         |(
         |  id integer NOT NULL,
         |  uuid character varying DEFAULT 'missing'::character varying,
         |  "number" character varying DEFAULT 'missing'::character varying,
         |  name character varying DEFAULT 'missing'::character varying,
         |  account_type character varying DEFAULT 'missing'::character varying,
         |  is_main_account boolean,
         |  user_id integer,
         |  user_uuid character varying DEFAULT 'missing'::character varying,
         |  currency character varying DEFAULT 'missing'::character varying,
         |  balance numeric(30,4) DEFAULT NULL::numeric,
         |  blocked_balance numeric(30,4) DEFAULT NULL::numeric,
         |  status character varying DEFAULT 'missing'::character varying,
         |  closed_at timestamp without time zone,
         |  last_transaction_at timestamp without time zone,
         |  created_at timestamp without time zone,
         |  updated_at timestamp without time zone,
         |  updated_by character varying,
         |  created_by character varying DEFAULT 'missing'::character varying,
         |  main_type character varying DEFAULT 'missing'::character varying,
         |  gp_insert_timestamp timestamp without time zone DEFAULT now()
         |);
         |
         |DROP TABLE IF EXISTS currencies;
         |CREATE TABLE currencies
         |(
         |  id character varying NOT NULL,
         |  currency_name character varying DEFAULT 'missing'::character varying
         |);
         |
         |$insertData
         |""".stripMargin

  override def cleanupSql: String =
    """
      |TRUNCATE TABLE transactions
    """.stripMargin

  "AggGreenplumDaoSpec" should {

    "return aggregates" in {
      // TODO : we are following the convention of "table.colum" in GroupByInput and Criteria. We wish to include `table` as a separate column later
      val entity = Entity(TransactionSqlDao.TableName, Option(TransactionSqlDao.TableAlias))
      val expressionsToAggregate = Seq(AggregationInput("dashboard_revenue", Sum, Option("revenue")))
      val criteria = Seq(CriteriaField(s"${TransactionSqlDao.TableAlias}.created_at", "2019-09-30", GreaterOrEqual))
      val groupBy = Seq(GroupByInput(s"${TransactionSqlDao.TableAlias}.type", None, None))
      val orderBy = None
      val limit = None
      val offset = None

      val result = aggGreenplumDao.aggregate(Seq(entity), expressionsToAggregate, criteria, groupBy, orderBy, limit, offset)

      println("!!!! Actual result " + result)
      result mustBe Right(List(AggregationResult(List(AggregatedValue("revenue", "1.5000")), List(Grouping(s"${TransactionSqlDao.TableAlias}.type", "currency_exchange"))), AggregationResult(List(AggregatedValue("revenue", "2.5000")), List(Grouping(s"${TransactionSqlDao.TableAlias}.type", "fees")))))
    }

    "return using multiple joins" in {
      val entity = Seq(Entity(TransactionSqlDao.TableName, Option(TransactionSqlDao.TableAlias)), Entity(AccountSqlDao.TableName, Option(AccountSqlDao.TableAlias), Seq(JoinColumn(s"${TransactionSqlDao.TableAlias}.primary_account_id", s"${AccountSqlDao.TableAlias}.id"))))
      val expressionsToAggregate = Seq(AggregationInput("dashboard_revenue", Sum, Option("revenue")))
      val criteria = Seq(CriteriaField(s"${TransactionSqlDao.TableAlias}.created_at", "2019-09-30", GreaterOrEqual))
      val groupBy = Seq(GroupByInput(s"${TransactionSqlDao.TableAlias}.type", None, None), GroupByInput(s"${AccountSqlDao.TableAlias}.uuid", None, Option("uuid")), GroupByInput(s"${TransactionSqlDao.TableAlias}.id", None, None))
      //val orderBy = Option(OrderingSet("revenue", "ASC"))
      val orderBy = None
      val limit = None
      val offset = None

      val result = aggGreenplumDao.aggregate(entity, expressionsToAggregate, criteria, groupBy, orderBy, limit, offset)

      println("!!!! Actual result " + result)
      val expected = Right(List(AggregationResult(List(AggregatedValue("revenue", "1.5000")), List(Grouping(s"${TransactionSqlDao.TableAlias}.type", "currency_exchange"), Grouping("uuid", "3406eaa0-6e80-42e9-9dc0-89eb53419a62"), Grouping(s"${TransactionSqlDao.TableAlias}.id", "611db3e8-23dd-4f76-b9ca-7ed8414a30fd"))), AggregationResult(List(AggregatedValue("revenue", "2.5000")), List(Grouping(s"${TransactionSqlDao.TableAlias}.type", "fees"), Grouping("uuid", "3406eaa0-6e80-42e9-9dc0-89eb53419a62"), Grouping(s"${TransactionSqlDao.TableAlias}.id", "611db3e8-23dd-4f76-b9ca-7ed8414a30fd")))))
      result mustBe expected
    }

    "return using joins which are not to driver table" in {
      val entity = Seq(Entity(TransactionSqlDao.TableName, Option(TransactionSqlDao.TableAlias)), Entity(AccountSqlDao.TableName, Option(AccountSqlDao.TableAlias), Seq(JoinColumn(s"${TransactionSqlDao.TableAlias}.primary_account_id", s"${AccountSqlDao.TableAlias}.id"))), Entity(CurrencySqlDao.TableName, Option(CurrencySqlDao.TableAlias), Seq(JoinColumn(s"${AccountSqlDao.TableAlias}.currency", s"${CurrencySqlDao.TableAlias}.id"))))
      val expressionsToAggregate = Seq(AggregationInput(s"${TransactionSqlDao.TableAlias}.dashboard_revenue", Sum, Option("revenue")))
      val criteria = Seq(CriteriaField(s"${TransactionSqlDao.TableAlias}.created_at", "2019-09-30", GreaterOrEqual))
      val groupBy = Seq(GroupByInput(s"${TransactionSqlDao.TableAlias}.type", None, None), GroupByInput(s"${AccountSqlDao.TableAlias}.uuid", None, Option("uuid")), GroupByInput(s"${TransactionSqlDao.TableAlias}.id", None, None), GroupByInput(s"${CurrencySqlDao.TableAlias}.currency_name", None, None))
      //val orderBy = Option(OrderingSet("revenue", "ASC"))
      val orderBy = None
      val limit = None
      val offset = None

      val result = aggGreenplumDao.aggregate(entity, expressionsToAggregate, criteria, groupBy, orderBy, limit, offset)

      println("!!!! Actual result " + result)
      val expected = Right(List(AggregationResult(List(AggregatedValue("revenue", "2.5000")), List(Grouping(s"${TransactionSqlDao.TableAlias}.type", "fees"), Grouping("uuid", "3406eaa0-6e80-42e9-9dc0-89eb53419a62"), Grouping(s"${TransactionSqlDao.TableAlias}.id", "611db3e8-23dd-4f76-b9ca-7ed8414a30fd"), Grouping(s"${CurrencySqlDao.TableAlias}.currency_name", "KES"))), AggregationResult(List(AggregatedValue("revenue", "1.5000")), List(Grouping(s"${TransactionSqlDao.TableAlias}.type", "currency_exchange"), Grouping("uuid", "3406eaa0-6e80-42e9-9dc0-89eb53419a62"), Grouping(s"${TransactionSqlDao.TableAlias}.id", "611db3e8-23dd-4f76-b9ca-7ed8414a30fd"), Grouping(s"${CurrencySqlDao.TableAlias}.currency_name", "KES")))))
      result mustBe expected
    }

    "return after applying scalar functions to date" in {
      val entity = Seq(Entity(TransactionSqlDao.TableName, Option(TransactionSqlDao.TableAlias)), Entity(AccountSqlDao.TableName, Option(AccountSqlDao.TableAlias), Seq(JoinColumn(s"${TransactionSqlDao.TableAlias}.primary_account_id", s"${AccountSqlDao.TableAlias}.id"))), Entity(CurrencySqlDao.TableName, Option(CurrencySqlDao.TableAlias), Seq(JoinColumn(s"${AccountSqlDao.TableAlias}.currency", s"${CurrencySqlDao.TableAlias}.id"))))
      val expressionsToAggregate = Seq(AggregationInput(s"${TransactionSqlDao.TableAlias}.dashboard_revenue", Sum, Option("revenue")))
      val criteria = Seq(CriteriaField(s"${TransactionSqlDao.TableAlias}.created_at", "2019-09-30", GreaterOrEqual))
      val groupBy = Seq(GroupByInput(s"${TransactionSqlDao.TableAlias}.created_at", Option(GetDate), Option("date")), GroupByInput(s"${TransactionSqlDao.TableAlias}.type", None, None))
      //val orderBy = Option(OrderingSet("revenue", "ASC"))
      val orderBy = None
      val limit = None
      val offset = None

      val result = aggGreenplumDao.aggregate(entity, expressionsToAggregate, criteria, groupBy, orderBy, limit, offset)

      println("!!!! Actual result " + result)
      val expected = Right(List(AggregationResult(List(AggregatedValue("revenue", "2.5000")), List(Grouping("date", "2019-10-29"), Grouping(s"${TransactionSqlDao.TableAlias}.type", "fees"))), AggregationResult(List(AggregatedValue("revenue", "1.5000")), List(Grouping("date", "2019-10-29"), Grouping(s"${TransactionSqlDao.TableAlias}.type", "currency_exchange")))))
      result mustBe expected
    }

    "return after multiple joins to driver table" in {
      val entity = Seq(Entity(TransactionSqlDao.TableName, Option(TransactionSqlDao.TableAlias)), Entity(AccountSqlDao.TableName, Option(AccountSqlDao.TableAlias), Seq(JoinColumn(s"${TransactionSqlDao.TableAlias}.primary_account_id", s"${AccountSqlDao.TableAlias}.id"))), Entity(CurrencySqlDao.TableName, Option(CurrencySqlDao.TableAlias), Seq(JoinColumn(s"${TransactionSqlDao.TableAlias}.currency", s"${CurrencySqlDao.TableAlias}.id"))))
      val expressionsToAggregate = Seq(AggregationInput(s"${TransactionSqlDao.TableAlias}.dashboard_revenue", Sum, Option("revenue")))
      val criteria = Seq(CriteriaField(s"${TransactionSqlDao.TableAlias}.created_at", "2019-09-30", GreaterOrEqual))
      val groupBy = Seq(GroupByInput(s"${TransactionSqlDao.TableAlias}.type", None, None), GroupByInput(s"${AccountSqlDao.TableAlias}.uuid", None, Option("uuid")), GroupByInput(s"${TransactionSqlDao.TableAlias}.id", None, None), GroupByInput(s"${CurrencySqlDao.TableAlias}.currency_name", None, None))
      //val orderBy = Option(OrderingSet("revenue", "ASC"))
      val orderBy = None
      val limit = None
      val offset = None

      val result = aggGreenplumDao.aggregate(entity, expressionsToAggregate, criteria, groupBy, orderBy, limit, offset)

      println("!!!! Actual result " + result)
      val expected = Right(List(AggregationResult(List(AggregatedValue("revenue", "2.5000")), List(Grouping(s"${TransactionSqlDao.TableAlias}.type", "fees"), Grouping("uuid", "3406eaa0-6e80-42e9-9dc0-89eb53419a62"), Grouping(s"${TransactionSqlDao.TableAlias}.id", "611db3e8-23dd-4f76-b9ca-7ed8414a30fd"), Grouping(s"${CurrencySqlDao.TableAlias}.currency_name", "KES"))), AggregationResult(List(AggregatedValue("revenue", "1.5000")), List(Grouping(s"${TransactionSqlDao.TableAlias}.type", "currency_exchange"), Grouping("uuid", "3406eaa0-6e80-42e9-9dc0-89eb53419a62"), Grouping(s"${TransactionSqlDao.TableAlias}.id", "611db3e8-23dd-4f76-b9ca-7ed8414a30fd"), Grouping(s"${CurrencySqlDao.TableAlias}.currency_name", "KES")))))
      result mustBe expected
    }

    "return when only aggregate is requested" in {
      val entity = Seq(Entity(TransactionSqlDao.TableName, Option(TransactionSqlDao.TableAlias)), Entity(AccountSqlDao.TableName, Option(AccountSqlDao.TableAlias), Seq(JoinColumn(s"${TransactionSqlDao.TableAlias}.primary_account_id", s"${AccountSqlDao.TableAlias}.id"))), Entity(CurrencySqlDao.TableName, Option(CurrencySqlDao.TableAlias), Seq(JoinColumn(s"${TransactionSqlDao.TableAlias}.currency", s"${CurrencySqlDao.TableAlias}.id"))))
      val expressionsToAggregate = Seq(AggregationInput(s"${TransactionSqlDao.TableAlias}.dashboard_revenue", Sum, Option("revenue")))
      val criteria = Seq(CriteriaField(s"${TransactionSqlDao.TableAlias}.created_at", "2019-09-30", GreaterOrEqual))
      val groupBy = Seq.empty
      //val orderBy = Option(OrderingSet("revenue", "ASC"))
      val orderBy = None
      val limit = None
      val offset = None

      val result = aggGreenplumDao.aggregate(entity, expressionsToAggregate, criteria, groupBy, orderBy, limit, offset)

      println("!!!! Actual result " + result)
      val expected = Right(List(AggregationResult(List(AggregatedValue("revenue", "4.0000")), List())))
      result mustBe expected
    }

    // Entity(transactions,Some(tx),List()),Entity(accounts,Some(a),List(JoinColumn(tx.primary_account_id,a.id))),Entity(currencies,Some(c),List(JoinColumn(tx.currency_id,c.id)))

  }

}
