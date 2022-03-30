package tech.pegb.backoffice

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{AbstractController, ControllerComponents, Request}
import sangria.execution._
import sangria.marshalling.playJson._
import sangria.parser.{QueryParser, SyntaxError}
import sangria.slowlog.SlowLog
import tech.pegb.backoffice.domain.graphql.{GraphQLRepo, SchemaDefinition}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class GraphQLController @Inject() (cc: ControllerComponents, graphQLRepo: GraphQLRepo) extends AbstractController(cc) {

  case object TooComplexQueryError extends Exception("Query is too expensive.")

  lazy val exceptionHandler = ExceptionHandler {
    case (_, error @ TooComplexQueryError) ⇒ HandledException(error.getMessage)
    case (_, error @ MaxQueryDepthReachedError(_)) ⇒ HandledException(error.getMessage)
  }

  private def executeQuery(query: String, variables: Option[JsObject], operation: Option[String], tracing: Boolean) =
    QueryParser.parse(query) match {

      // query parsed successfully, time to execute it!
      case Success(queryAst) ⇒
        Executor.execute(SchemaDefinition.Value, queryAst, graphQLRepo,
          variables = variables getOrElse Json.obj(),
          operationName = operation,
          exceptionHandler = exceptionHandler,
          queryReducers = List(
            QueryReducer.rejectMaxDepth[GraphQLRepo](15),
            QueryReducer.rejectComplexQueries[GraphQLRepo](4000, (_, _) ⇒ TooComplexQueryError)),
          middleware = if (tracing) SlowLog.apolloTracing :: Nil else Nil)
          .map(Ok(_))
          .recover {
            case error: QueryAnalysisError ⇒ BadRequest(error.resolveError)
            case error: ErrorWithResolver ⇒ InternalServerError(error.resolveError)
          }

      // can't parse GraphQL query, return error
      case Failure(error: SyntaxError) ⇒
        Future.successful(BadRequest(Json.obj(
          "syntaxError" → error.getMessage,
          "locations" → Json.arr(Json.obj(
            "line" → error.originalError.position.line,
            "column" → error.originalError.position.column)))))

      case Failure(error) ⇒
        throw error
    }

  def ql = Action.async(parse.json) { request ⇒
    val query = (request.body \ "query").as[String]
    val operation = (request.body \ "operationName").asOpt[String]
    lazy val variables = (request.body \ "variables").toOption.flatMap {
      case JsString(vars) ⇒ Some(parseVariables(vars))
      case obj: JsObject ⇒ Some(obj)
      case _ ⇒ None
    }

    def parseVariables(variables: String) =
      if (variables.trim == "" || variables.trim == "null") Json.obj() else Json.parse(variables).as[JsObject]

    executeQuery(query, variables, operation, isTracingEnabled(request))
  }

  def isTracingEnabled(request: Request[_]) = request.headers.get("X-Apollo-Tracing").isDefined

}
