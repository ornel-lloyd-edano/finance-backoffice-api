package tech.pegb.backoffice.dao.aggregations.abstraction

sealed trait AggFunction {

}

object AggFunctions {
  case object Sum extends AggFunction
  case object Avg extends AggFunction
  case object Count extends AggFunction
}

