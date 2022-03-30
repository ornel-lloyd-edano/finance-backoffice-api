package tech.pegb.backoffice.dao.aggregations.abstraction

sealed trait ScalarFunction {

}

object ScalarFunctions {
  case object GetMonth extends ScalarFunction
  case object GetDate extends ScalarFunction
  case object GetWeek extends ScalarFunction
}
