package tech.pegb.backoffice.domain.graphql

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import io.circe._
import io.circe.parser._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros._
import sangria.marshalling.circe._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.customer.abstraction.CustomerRead
import tech.pegb.backoffice.domain.graphql.SchemaDefinition._
import tech.pegb.backoffice.domain.model.TransactionAggregatation
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionService
import tech.pegb.backoffice.domain.transaction.model.{Transaction, TransactionStatus}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.TestExecutionContext
import tech.pegb.backoffice.domain.model.CustomerAggregation
import tech.pegb.backoffice.domain.settings.abstraction.SystemSettingService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SchemaSpec extends WordSpec with Matchers with MockFactory with ScalaFutures {
  val mockedTransactionService = stub[TransactionService]
  val mockedCustomerService = stub[CustomerRead]
  val mockedSystemSettings = stub[SystemSettingService]
  val executionContexts: WithExecutionContexts = TestExecutionContext

  val config = AppConfig("application.test.conf")

  val transactionRepo = new TransactionRepo(mockedTransactionService, config, mockedSystemSettings, executionContexts)
  val customerRepo = new CustomersRepo(mockedCustomerService, executionContexts)
  val graphQLRepo = new GraphQLRepo(transactionRepo, customerRepo, executionContexts)

  val trxExample = Transaction.getEmpty.copy(
    id = "1549446333",
    direction = "credit",
    `type` = "merchant_payment",
    amount = BigDecimal(500.00),
    currency = Currency.getInstance("AED"),
    channel = "IOS_APP",
    explanation = Some("some explanation"),
    status = TransactionStatus("success"),
    instrument = None,
    createdAt = LocalDateTime.of(2018, 12, 26, 3, 7, 30, 0))

  val testRecord = List(trxExample, trxExample.copy(id = "2"), trxExample.copy(id = "3"))
  val result = Future.successful(Right(testRecord))

  def setupMocks(): Unit = {
    (mockedTransactionService.getById _).when("999").returns(Future.successful(
      Left(ServiceError.notFoundError("Not found", UUID.randomUUID().toOption))))
    (mockedTransactionService.getById _).when(*).returns(Future.successful(Right(trxExample)))
    (mockedTransactionService.getTransactionsByCriteria _).when(*, *, *, *).returns(result)
    (mockedTransactionService.sumTransactionsByCriteria _).when(*).returns(Future.successful(Right(BigDecimal(100.01))))
    (mockedTransactionService.countTransactionsByCriteria _).when(*).returns(Future.successful(Right(123)))
    (mockedTransactionService.aggregateTransactionByCriteriaAndPivots _).when(*, *).returns(
      Future.successful(Right(Seq(
        TransactionAggregatation(uniqueId = "1", count = Option(123), sum = Option(BigDecimal(123.33)), day = Option(22), currency = Option(Currency.getInstance("AED"))),
        TransactionAggregatation(uniqueId = "2", count = Option(423), sum = Option(BigDecimal(1232.33)), day = Option(23), currency = Option(Currency.getInstance("AED")))))))
    (mockedCustomerService.aggregateCustomersByCriteriaAndPivots _).when(*, *, *).returns(
      Future.successful(Right(List(
        CustomerAggregation(uniqueId = "1", count = Option(123), sum = Option(BigDecimal(123.33)), isActivated = Option(true)),
        CustomerAggregation(uniqueId = "2", count = Option(423), sum = Option(BigDecimal(1232.33)), isActivated = Option(false))))))
  }

  "Transactions Schema" should {
    "allow to fetch a transaction by its ID provided through variables" in {
      setupMocks()
      val query =
        graphql"""
         query FetchTransactionByIDQuery($$transactionId: String!) {
           transaction(id: $$transactionId) {
             amount
           }
         }
       """

      executeQuery(query, vars = Json.obj("transactionId" → Json.fromString("1"))) should be(parse(
        """
         {
           "data": {
             "transaction": {
               "amount": 500.0
             }
           }
         }
        """).right.get)
    }

    "allow to fetch transactions by status with limits and offsets provided in variables" in {
      setupMocks()
      val query =
        graphql"""
         query FetchTransactions($$status: String, $$limit: Int, $$offset: Int, $$dateFrom: String) {
           transactions(status: $$status, limit: $$limit, offset: $$offset, date_from: $$dateFrom) {
             amount
           }
           sum(status: $$status)
         }
       """

      val variables = Json.obj(
        "status" -> Json.fromString("success"),
        "limit" → Json.fromInt(5),
        "offset" → Json.fromInt(1))

      executeQuery(query, vars = variables) should be(parse(
        """
         {
          "data" : {
            "transactions" : [
              {
                "amount" : 500.0
              },
              {
                "amount" : 500.0
              },
              {
                "amount" : 500.0
              }
            ],
            "sum" : 100.01
          }
        }
        """).right.get)
    }

    "allows to count transactions with no parameters" in {
      setupMocks()
      val query =
        graphql"""
         query CountTransactions($$type: String!) {
           only_type_count: count(type: $$type)
           count
         }
       """

      executeQuery(query, vars = Json.obj("type" → Json.fromString("currency_exchange"))) should be(parse(
        """
         {
           "data": {
             "only_type_count": 123,
             "count": 123
           }
         }
        """).right.get)
    }

    "allow to fetch transactions with no arguments" in {
      setupMocks()
      val query =
        graphql"""
         query TransactionsTimeSeriesQuery {
          transactions {
            amount
            createdAt
          }
        }
       """

      val variables = Json.obj()

      executeQuery(query, vars = variables) should be(parse(
        """{
          "data" : {
            "transactions" : [
              {
                "amount" : 500.0,
                "createdAt" : "2018-12-26T03:07:30"
              },
              {
                "amount" : 500.0,
                "createdAt" : "2018-12-26T03:07:30"
              },
              {
                "amount" : 500.0,
                "createdAt" : "2018-12-26T03:07:30"
              }
            ]
          }
        }
        """).right.get)
    }

    "returns null for transaction not found" in {
      setupMocks()
      val query =
        graphql"""
         query {
          transaction(id: "999") {
            amount
            createdAt
          }
        }
       """

      val variables = Json.obj()

      executeQuery(query, vars = variables) should be(parse(
        """{
          "data" : {
            "transaction" : null
          }
        }
        """).right.get)
    }

    "returns all for transaction status null" in {
      setupMocks()
      val query =
        graphql"""
         query {
          transactions(type: "") {
            amount
            createdAt
          }
        }
       """

      val variables = Json.obj()

      executeQuery(query, vars = variables) should be(parse(
        """{
          "data" : {
            "transactions" : [
              {
                "amount" : 500.0,
                "createdAt" : "2018-12-26T03:07:30"
              },
              {
                "amount" : 500.0,
                "createdAt" : "2018-12-26T03:07:30"
              },
              {
                "amount" : 500.0,
                "createdAt" : "2018-12-26T03:07:30"
              }
            ]
          }
        }
        """).right.get)
    }

    "returns aggregates for transactions grouped by day and currency" in {
      setupMocks()
      val query =
        graphql"""
         query {
          transactionAggregations(groupBy: ["currency", "day"]) {
            sum
            count
            day
            currency
          }
        }
       """

      val variables = Json.obj()

      executeQuery(query, vars = variables) should be(parse(
        """{
          "data" : {
            "transactionAggregations" : [
              {
                "sum" : 123.33,
                "count" : 123,
                "day" : 22,
                "currency" : "AED"
              },
              {
                "sum" : 1232.33,
                "count" : 423,
                "day" : 23,
                "currency" : "AED"
              }
            ]
          }
        }
        """).right.get)
    }

    "returns aggregates for customers grouped by activated at" in {
      setupMocks()
      val query =
        graphql"""
         query {
          customerAggregations(groupBy: ["activated"]) {
            sum
            count
            isActivated
          }
        }
       """

      val variables = Json.obj()

      executeQuery(query, vars = variables) should be(parse(
        """{
          "data" : {
            "customerAggregations" : [
              {
                "sum" : 123.33,
                "count" : 123,
                "isActivated" : true
              },
              {
                "sum" : 1232.33,
                "count" : 423,
                "isActivated" : false
              }
            ]
          }
        }
        """).right.get)
    }
  }

  def executeQuery(query: Document, vars: Json = Json.obj()) = {
    val futureResult = Executor.execute(Value, query,
      variables = vars,
      userContext = graphQLRepo)

    Await.result(futureResult, 10.seconds)
  }
}
