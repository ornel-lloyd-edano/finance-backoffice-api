package tech.pegb.backoffice.dao.helper

import java.sql.SQLException

import tech.pegb.backoffice.dao.{DaoError, DaoErrorHandler}

object Helper extends DaoErrorHandler {

  implicit class ExceptionHandler[T](dto: T) {

    def handleException(op: String): PartialFunction[Throwable, DaoError] = {

      case e: SQLException if e.getErrorCode == 1406 || e.getErrorCode == 22001 ⇒ //22001 is for H2
        constraintViolationError(s"Unable to $op task. One or more fields is too big for the defined column size.")
      case e: SQLException if e.getErrorCode == 1062 ⇒
        constraintViolationError(s"Unable to $op task. Id or uuid is duplicated.")
      case e: Exception ⇒
        logger.error(s"Unknown error while performing $op task $dto", e)
        genericDbError("Unknown error")

    }
  }

}
